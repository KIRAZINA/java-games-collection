import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import App from '../App.jsx';
import { roomsApi, blackjackApi } from '../api/api.js';

// Step 6g E (curator-approved correction - retry-once) :
// 1. the Quick Play button is disabled (and labelled "Creating room…") while the
//    create request is in flight, through BOTH attempts of the retry,
// 2. a reject surfaces as a visible error instead of a silently unchanged screen,
// 3. a 5xx is retried exactly once (createRoom leaves the owner's previous room
//    first, so the retry cannot stack ghosts), and only after the retry also
//    fails is the list refetched and the message shown,
// 4. if the retry succeeds the game opens - no error, no ghost left behind.
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
    let rejectFirst;
    const createRoom = vi
      .spyOn(roomsApi, 'createRoom')
      .mockImplementationOnce(() => new Promise((_, reject) => { rejectFirst = reject; }))
      .mockRejectedValue(make500());
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

    // first attempt fails with a 500 -> the single retry fires while the flag
    // is still held, then the failure path refetches the list
    rejectFirst(make500());
    await waitFor(() => expect(createRoom).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(roomsApi.listRooms).toHaveBeenCalled());

    // once both attempts settled the button is back and enabled
    const settled = await screen.findByRole('button', { name: /Quick Play \(Solo\)/ });
    expect(settled).toBeEnabled();
  });
});

describe('6g E2 - a 500 from quick play is surfaced, not swallowed', () => {
  beforeEach(() => {
    vi.stubGlobal('prompt', vi.fn(() => 'Alice'));
    stubNetwork();
  });

  it('retries once, then shows the server error and refetches the room list', async () => {
    const listRooms = vi.spyOn(roomsApi, 'listRooms').mockResolvedValue([]);
    const createRoom = vi.spyOn(roomsApi, 'createRoom').mockRejectedValue(make500('Server exploded'));

    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /Blackjack \(Solo\)/ }));

    // the error is visible - this is the exact case that used to render nothing
    expect(await screen.findByRole('alert')).toHaveTextContent('Server exploded');

    // exactly one retry: no loop, no third attempt
    expect(createRoom).toHaveBeenCalledTimes(2);

    // 5xx refetch: the list is pulled again AFTER the final failure (the
    // lobby's own mount fetch necessarily comes first)
    await waitFor(() => {
      const lastCreate = createRoom.mock.invocationCallOrder.at(-1);
      const lastList = listRooms.mock.invocationCallOrder.at(-1);
      expect(lastList).toBeGreaterThan(lastCreate);
    });
    expect(listRooms.mock.calls.at(-1)[0]).toBe('BLACKJACK');
  });
});

describe('6g E3 - 500 on the first create is healed by the retry', () => {
  beforeEach(() => {
    vi.stubGlobal('prompt', vi.fn(() => 'Alice'));
    stubNetwork();
  });

  it('the retry succeeds, the game opens, and no error is shown', async () => {
    const summary = {
      roomId: 'r-retry-1',
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
      playerToken: 'tok-retry-1',
    };
    const createRoom = vi
      .spyOn(roomsApi, 'createRoom')
      .mockRejectedValueOnce(make500())
      .mockResolvedValueOnce(summary);
    vi.spyOn(roomsApi, 'listRooms').mockResolvedValue([]);
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
      sessionId: 's-retry',
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

    // first call 500s, second call is the retry and it succeeds
    await waitFor(() => expect(createRoom).toHaveBeenCalledTimes(2));

    // no error is shown - the failed first attempt healed itself
    expect(screen.queryByRole('alert')).toBeNull();

    // the game view is open (exact heading distinguishes it from "Blackjack
    // Lobby") and it polls the room it was handed
    await screen.findByRole('heading', { name: /^Blackjack$/ }, { timeout: 4000 });
    await waitFor(() =>
      expect(roomsApi.getRoomState).toHaveBeenCalledWith('r-retry-1')
    );
    expect(screen.queryByRole('alert')).toBeNull();
  });
});
