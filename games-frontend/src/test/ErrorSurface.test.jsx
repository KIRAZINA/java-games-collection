import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { RoomLobby } from '../components/RoomLobby.jsx';
import { blackjackApi, roomsApi, rehydrateRoomTokens } from '../api/api.js';

// NOTE: this file deliberately does NOT mock ../api/api.js — it exercises the real
// request/error pipeline (status parsing, {message}/{error} shapes) end-to-end.

const NETWORK_MSG =
  'Unable to reach the server. Please ensure the backend is running and try again.';

function mkResponse({ status, body, unparseable = false }) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => {
      if (unparseable) throw new SyntaxError('Unexpected token < in JSON');
      return body;
    },
  };
}

function stubFetch(handler) {
  const fn = vi.fn(async (url, init = {}) => handler(String(url), init));
  vi.stubGlobal('fetch', fn);
  return fn;
}

const roomList = [{
  roomId: 'r-9', roomName: 'Host Room', gameType: 'BLACKJACK', phase: 'LOBBY',
  playerCount: 1, maxPlayers: 4, spectatorCount: 0, passwordProtected: false,
  ownerName: 'Host', createdAt: 't', lastActivity: 't', timeLimitSeconds: 0,
  isSinglePlayer: false,
}];

describe('Area 4 - server error responses surface the server message', () => {
  beforeEach(() => {
    sessionStorage.clear();
    rehydrateRoomTokens();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  // ── api() level: every status carries the right shape ──────────────────────

  it('400 with {message} shape surfaces the server message', async () => {
    stubFetch(() => mkResponse({ status: 400, body: { message: 'Insufficient funds for this bet' } }));
    await expect(blackjackApi.placeBet('s-1', 999))
      .rejects.toThrow('Insufficient funds for this bet');
  });

  it('403 with {error} shape surfaces the server message', async () => {
    stubFetch(() => mkResponse({ status: 403, body: { error: 'Invalid or missing player token', status: 403 } }));
    await expect(roomsApi.markReady('r-1', 'p-1'))
      .rejects.toThrow('Invalid or missing player token');
  });

  it('404 with {error} shape surfaces the server message', async () => {
    stubFetch(() => mkResponse({ status: 404, body: { error: 'Room not found: r-1', status: 404 } }));
    await expect(roomsApi.getRoomState('r-1'))
      .rejects.toThrow('Room not found: r-1');
  });

  it('409 duplicate idempotent request surfaces "Duplicate request in progress"', async () => {
    stubFetch(() => mkResponse({ status: 409, body: { message: 'Duplicate request in progress' } }));
    await expect(roomsApi.joinRoom('r-1', 'p-1', 'Pat'))
      .rejects.toThrow('Duplicate request in progress');
  });

  it('500 with {message} shape surfaces the server message, not "Request failed"', async () => {
    stubFetch(() => mkResponse({
      status: 500,
      body: { message: 'An unexpected error occurred: connection pool exhausted' },
    }));
    await expect(blackjackApi.createSession(100, 'BASIC'))
      .rejects.toThrow('An unexpected error occurred: connection pool exhausted');
  });

  it('500 with an unparseable body falls back to "Something went wrong"', async () => {
    stubFetch(() => mkResponse({ status: 500, unparseable: true }));
    await expect(blackjackApi.createSession(100, 'BASIC'))
      .rejects.toThrow('Something went wrong');
  });

  it('network failure surfaces the reachability message', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => { throw new TypeError('Failed to fetch'); }));
    await expect(roomsApi.listRooms('BLACKJACK')).rejects.toThrow(NETWORK_MSG);
  });

  // ── end to end: real api() → real RoomLobby UI ─────────────────────────────

  it('409 on join shows the server message in the UI and the app stays usable (not a silent no-op)', async () => {
    const user = userEvent.setup();
    stubFetch((url, init) =>
      init.method === 'POST'
        ? mkResponse({ status: 409, body: { message: 'Duplicate request in progress' } })
        : mkResponse({ status: 200, body: roomList })
    );

    render(<RoomLobby gameKey="blackjack" playerId="p-1" playerName="Pat" />);
    const joinButton = await screen.findByRole('button', { name: 'Join' });
    await user.click(joinButton);

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Duplicate request in progress');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Join' })).toBeEnabled());
  });

  it('403 wrong password on join shows the server reason to the user', async () => {
    const user = userEvent.setup();
    vi.stubGlobal('prompt', vi.fn(() => 'guess'));
    stubFetch((url, init) =>
      init.method === 'POST'
        ? mkResponse({ status: 403, body: { error: 'Invalid password', status: 403 } })
        : mkResponse({
            status: 200,
            body: [{ ...roomList[0], roomId: 'r-lock', passwordProtected: true }],
          })
    );

    render(<RoomLobby gameKey="blackjack" playerId="p-1" playerName="Pat" />);
    const joinButton = await screen.findByRole('button', { name: 'Join' });
    await user.click(joinButton);

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Invalid password');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Join' })).toBeEnabled());
  });

  it('500 on join surfaces the server message and the app does not crash', async () => {
    const user = userEvent.setup();
    stubFetch((url, init) =>
      init.method === 'POST'
        ? mkResponse({ status: 500, body: { message: 'An unexpected error occurred: boom' } })
        : mkResponse({ status: 200, body: roomList })
    );

    render(<RoomLobby gameKey="blackjack" playerId="p-1" playerName="Pat" />);
    const joinButton = await screen.findByRole('button', { name: 'Join' });
    await user.click(joinButton);

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('An unexpected error occurred: boom');

    // Still interactive: a retry click issues a new request rather than crashing.
    const fetchCalls = globalThis.fetch.mock.calls.length;
    await user.click(screen.getByRole('button', { name: 'Join' }));
    await waitFor(() => expect(globalThis.fetch.mock.calls.length).toBeGreaterThan(fetchCalls));
    expect(screen.getByRole('button', { name: 'Join' })).toBeInTheDocument();
  });
});
