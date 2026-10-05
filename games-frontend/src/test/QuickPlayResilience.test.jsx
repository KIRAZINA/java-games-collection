import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import App from '../App.jsx';
import { roomsApi, blackjackApi } from '../api/api.js';

// Step 6g E - "click Quick Play, nothing happens" must not be possible again:
// 1. the Quick Play button is disabled (and labelled "Creating room…") while the
//    create request is in flight,
// 2. a reject surfaces as a visible error instead of a silently unchanged screen,
// 3. a 5xx specifically triggers a list refetch (the room may exist despite the
//    error - "error after side effect"), and if our own room is in the fresh
//    list the app routes into it instead of showing the error.
//
// The api is spied on directly (not fetch stubbed) so the tests can hold the
// createRoom promise open to observe the in-flight UI state.

function stubNetwork() {
  vi.stubGlobal(
    'fetch',
    vi.fn(async () => {
      throw new TypeError('network disabled for this test');
    })
  );
}

function make500(message = 'An unexpected error occurred: test 500') {
  const err = new Error(message);
  err.status = 500;
  return err;
}

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('6g E1 - quick play in-flight state', () => {
  beforeEach(() => {
    vi.stubGlobal('prompt', vi.fn(() => 'Alice'));
    stubNetwork();
  });

  it('disables Quick Play with a "Creating room…" label while createRoom is in flight', async () => {
    let rejectCreate;
    const createRoom = vi
      .spyOn(roomsApi, 'createRoom')
      .mockImplementation(() => new Promise((_, reject) => { rejectCreate = reject; }));
    vi.spyOn(roomsApi, 'listRooms').mockResolvedValue([]);

    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /Blackjack \(Solo\)/ }));

    // the create call is in flight - App already switched to the lobby page
    await waitFor(() => expect(createRoom).toHaveBeenCalledTimes(1));

    const quickPlay = await screen.findByRole('button', { name: /Creating room/ });
    expect(quickPlay).toBeDisabled();

    // a second click must not start a second create request
    expect(createRoom).toHaveBeenCalledTimes(1);

    rejectCreate(make500());
    await waitFor(() => expect(roomsApi.listRooms).toHaveBeenCalled());

    // once settled the button is back and enabled
    const settled = await screen.findByRole('button', { name: /Quick Play \(Solo\)/ });
    expect(settled).toBeEnabled();
  });
});

describe('6g E2 - a 500 from quick play is surfaced, not swallowed', () => {
  beforeEach(() => {
    vi.stubGlobal('prompt', vi.fn(() => 'Alice'));
    stubNetwork();
  });

  it('shows the server error and refetches the room list on a 500', async () => {
    const listRooms = vi.spyOn(roomsApi, 'listRooms').mockResolvedValue([]);
    vi.spyOn(roomsApi, 'createRoom').mockRejectedValue(make500('Server exploded'));

    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /Blackjack \(Solo\)/ }));

    // the error is visible - this is the exact case that used to render nothing
    expect(await screen.findByRole('alert')).toHaveTextContent('Server exploded');

    // 5xx refetch: the list is pulled again because the room may exist despite
    // the failed response
    await waitFor(() => expect(listRooms).toHaveBeenCalledTimes(1));
    expect(listRooms.mock.calls[0][0]).toBe('BLACKJACK');
  });
});

describe('6g E3 - 500 with the room already created routes into it', () => {
  beforeEach(() => {
    vi.stubGlobal('prompt', vi.fn(() => 'Alice'));
    stubNetwork();
  });

  it('refetches the list and enters the own room instead of showing the error', async () => {
    const soloRoom = {
      roomId: 'r-ghost-1',
      roomName: "Alice's Practice",
      gameType: 'BLACKJACK',
      phase: 'PLAYING',
      playerCount: 1,
      maxPlayers: 1,
      spectatorCount: 0,
      passwordProtected: false,
      ownerName: 'Alice',
      createdAt: new Date().toISOString(),
      lastActivity: new Date().toISOString(),
      timeLimitSeconds: 0,
      isSinglePlayer: true,
      playerToken: null,
    };
    vi.spyOn(roomsApi, 'createRoom').mockRejectedValue(make500());
    vi.spyOn(roomsApi, 'listRooms').mockResolvedValue([soloRoom]);
    // getRoomState is what the in-room views poll; settle it straight to
    // PLAYING so the Blackjack handoff completes without extra mocking.
    vi.spyOn(roomsApi, 'getRoomState').mockResolvedValue({
      roomPhase: 'PLAYING',
      timeRemaining: 0,
      players: [],
      playerCount: 1,
    });
    // the routed-into game mounts and bootstraps its own session - resolve it
    // (and the room registration) so no error alert is ever rendered.
    vi.spyOn(blackjackApi, 'createSession').mockResolvedValue({
      sessionId: 's-ghost',
      phase: 'BETTING',
      balance: 100,
      dealerCards: [],
      playerCards: [],
      dealerValue: 0,
      playerValue: 0,
      notifications: [],
    });
    vi.spyOn(roomsApi, 'registerSession').mockResolvedValue(null);

    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /Blackjack \(Solo\)/ }));

    // the 5xx path refetched the list...
    await waitFor(() => expect(roomsApi.listRooms).toHaveBeenCalled());

    // ...and the own room was found: the error is NOT shown, the game is.
    expect(screen.queryByRole('alert')).toBeNull();
    await screen.findByRole('heading', { name: /Blackjack/ }, { timeout: 4000 });
    expect(roomsApi.getRoomState).toHaveBeenCalledWith('r-ghost-1');
  });
});
