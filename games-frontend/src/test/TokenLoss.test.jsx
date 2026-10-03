import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import App from '../App.jsx';
import { RoomLobby } from '../components/RoomLobby.jsx';
import { getRoomToken, rehydrateRoomTokens } from '../api/api.js';

// NOTE: uses the real api.js pipeline — 403 must reach err.status so the lobby
// can distinguish an expired token from any other failure.

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

function stubBackend() {
  vi.stubGlobal('fetch', vi.fn(async (url, init = {}) => {
    const method = init.method ?? 'GET';
    const target = String(url);
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

describe('Area 5 - token loss sends the player back to the lobby with a rejoin notice', () => {
  beforeEach(() => {
    sessionStorage.clear();
    rehydrateRoomTokens();
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

  it('403 on markReady drops the stale token, returns to the room list, and reports auth loss', async () => {
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

    const readyButton = await screen.findByRole(
      'button', { name: "I'm Ready!" }, { timeout: 3000 }
    );
    await user.click(readyButton);

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

    const readyButton = await screen.findByRole(
      'button', { name: "I'm Ready!" }, { timeout: 3000 }
    );
    await user.click(readyButton);

    const alert = await screen.findByRole('alert', {}, { timeout: 3000 });
    expect(alert).toHaveTextContent(NOTICE);
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Join' })).toBeInTheDocument()
    );
    expect(getRoomToken('r-1')).toBeUndefined();
  });
});
