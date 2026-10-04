import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import App from '../App.jsx';
import { RoomLobby } from '../components/RoomLobby.jsx';
import { getRoomToken } from '../api/api.js';

// NOTE: uses the real api.js pipeline - 403 must reach err.status so the lobby
// can distinguish an expired token from any other failure.
//
// F3 (step 6b.1): sessionStorage is now consulted on every request, so a
// same-tab clear() is enough to stop X-Player-Token going out. The old
// rehydrateRoomTokens() workaround in beforeEach is gone: it is exactly what
// hid the bug (it re-seeded the memory cache right after clearing the store).

const NOTICE = 'Invalid or missing player token. Please rejoin the room.';

function mkResponse(status, body) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  };
}

const soloRoom = {
  roomId: 'r-1',
  roomName: 'Solo Run',
  gameType: 'MINESWEEPER',
  phase: 'LOBBY',
  playerCount: 1,
  maxPlayers: 2,
  spectatorCount: 0,
  passwordProtected: false,
  ownerName: 'Host',
  createdAt: 't',
  lastActivity: 't',
  timeLimitSeconds: 60,
  isSinglePlayer: true,
};

let sentTokens;

function stubBackend() {
  sentTokens = [];
  vi.stubGlobal('fetch', vi.fn(async (url, init = {}) => {
    const method = init.method ?? 'GET';
    const target = String(url);
    sentTokens.push({ target, token: (init.headers ?? {})['X-Player-Token'] });
    if (method === 'POST' && target.includes('/join')) {
      return mkResponse(200, {
        roomId: 'r-1',
        playerToken: 'tok-abc',
        gameType: 'MINESWEEPER',
        isSinglePlayer: true,
      });
    }
    if (method === 'POST' && target.includes('/ready')) {
      return mkResponse(403, { error: 'Invalid or missing player token', status: 403 });
    }
    if (target.includes('/state')) {
      return mkResponse(200, { roomPhase: 'LOBBY', timeRemaining: 0, players: [] });
    }
    return mkResponse(200, [soloRoom]);
  }));
}

function tokenOfRequest(fragment) {
  return sentTokens.find((r) => r.target.includes(fragment));
}

describe('Area 5 - token loss sends the player back to the lobby with a rejoin notice', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('renders the notice prop as a role=alert in the room list', async () => {
    stubBackend();
    render(
      <RoomLobby gameKey="minesweeper" playerId="p-1" playerName="Pat" notice={NOTICE} />
    );
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(NOTICE);
  });

  it('sessionStorage.clear() stops the token being sent - 403 on markReady drops it and returns to the lobby', async () => {
    const user = userEvent.setup();
    stubBackend();
    const onAuthLost = vi.fn();

    render(
      <RoomLobby
        gameKey="minesweeper"
        playerId="p-1"
        playerName="Pat"
        onAuthLost={onAuthLost}
      />
    );

    await user.click(await screen.findByRole('button', { name: 'Join' }));
    expect(getRoomToken('r-1')).toBe('tok-abc');

    // The store is wiped at runtime; no `storage` event fires in this tab.
    sessionStorage.clear();
    expect(sessionStorage.getItem('roomTokens')).toBeNull();

    const readyButton = await screen.findByRole(
      'button', { name: "I'm Ready!" }, { timeout: 3000 }
    );
    sentTokens.length = 0;
    await user.click(readyButton);

    const readyRequest = tokenOfRequest('/ready');
    expect(readyRequest, 'the ready request was sent').toBeTruthy();
    expect(
      readyRequest.token,
      'X-Player-Token must be omitted once sessionStorage no longer has it'
    ).toBeUndefined();

    await waitFor(() => expect(onAuthLost).toHaveBeenCalledWith(NOTICE));
    expect(getRoomToken('r-1')).toBeUndefined();
    expect(await screen.findByRole('button', { name: 'Join' })).toBeInTheDocument();
  });

  it('App flow: 403 during the ready check shows the rejoin notice in the lobby', async () => {
    const user = userEvent.setup();
    vi.stubGlobal('prompt', vi.fn(() => 'Pat'));
    stubBackend();

    render(<App />);

    await user.click(screen.getByRole('tab', { name: /Minesweeper/ }));
    await user.click(await screen.findByRole('button', { name: 'Join' }));

    expect(getRoomToken('r-1')).toBe('tok-abc');
    sessionStorage.clear();

    const readyButton = await screen.findByRole(
      'button', { name: "I'm Ready!" }, { timeout: 3000 }
    );
    sentTokens.length = 0;
    await user.click(readyButton);

    const readyRequest = tokenOfRequest('/ready');
    expect(readyRequest, 'the ready request was sent').toBeTruthy();
    expect(readyRequest.token, 'cleared storage must not be resurrected from memory').toBeUndefined();

    const alert = await screen.findByRole('alert', {}, { timeout: 3000 });
    expect(alert).toHaveTextContent(NOTICE);
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Join' })).toBeInTheDocument()
    );
    expect(getRoomToken('r-1')).toBeUndefined();
  });
});
