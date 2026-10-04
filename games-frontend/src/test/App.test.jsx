import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import App from '../App.jsx';
import { roomsApi } from '../api/api.js';

// Step 6b.1 regression guards. playerId and the quick-play owner name are only
// observable on the wire, so both tests capture them from createRoom.
// The API is stubbed to reject AFTER the call is recorded: the assertions are
// about the request payload, and rejecting keeps the app in the room list
// instead of mounting a game component. Reaching the game is covered by the
// real-browser B4 scenario in e2e/step6b.spec.ts.

function stubNetwork() {
  vi.stubGlobal(
    'fetch',
    vi.fn(async () => {
      throw new TypeError('network disabled for this test');
    })
  );
}

function stubQuickPlay() {
  vi.stubGlobal('prompt', vi.fn(() => 'Alice'));
  stubNetwork();
}

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('F1 - each App instance owns a distinct playerId', () => {
  let createRoom;

  beforeEach(() => {
    createRoom = vi.spyOn(roomsApi, 'createRoom').mockRejectedValue(new Error('stopped after create'));
    stubQuickPlay();
  });

  it('two mounted App instances produce different, non-trivial playerIds', async () => {
    const user = userEvent.setup();
    const first = render(<App />);
    const second = render(<App />);

    await user.click(within(first.container).getByRole('button', { name: /Blackjack \(Solo\)/ }));
    await user.click(within(second.container).getByRole('button', { name: /Blackjack \(Solo\)/ }));

    await waitFor(() => expect(createRoom).toHaveBeenCalledTimes(2));

    const idA = createRoom.mock.calls[0][3];
    const idB = createRoom.mock.calls[1][3];

    expect(idA, 'first instance playerId').toBeTruthy();
    expect(idB, 'second instance playerId').toBeTruthy();
    expect(idA.length, 'playerId longer than the single "p" character').toBeGreaterThan(1);
    expect(idB.length, 'playerId longer than the single "p" character').toBeGreaterThan(1);
    expect(idA).toMatch(/^player-/);
    expect(idB).toMatch(/^player-/);
    expect(idA, 'the two instances must not share one identity').not.toBe(idB);
    expect(idA).not.toBe('p');
    expect(idB).not.toBe('p');
  });
});

describe('F2 - quick play uses the prompted name instead of stale state', () => {
  let createRoom;

  beforeEach(() => {
    createRoom = vi.spyOn(roomsApi, 'createRoom').mockRejectedValue(new Error('stopped after create'));
    stubQuickPlay();
  });

  it('sends ownerName "Alice" to createRoom even though playerName state is still empty', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /Blackjack \(Solo\)/ }));

    await waitFor(() => expect(createRoom).toHaveBeenCalledTimes(1));

    const [roomName, gameType, settings, ownerId, ownerName] = createRoom.mock.calls[0];
    expect(ownerName).toBe('Alice');
    expect(roomName).toBe("Alice's Practice");
    expect(gameType).toBe('BLACKJACK');
    expect(settings.isSinglePlayer).toBe(true);
    expect(ownerId).toMatch(/^player-.+/);

    expect(window.prompt).toHaveBeenCalledWith('Enter your display name:', 'Player');
    await screen.findByText('Playing as');
    expect(screen.getByText('Alice')).toBeInTheDocument();
  });
});

describe('G1 - a multiplayer room waits in the lobby for the ready step', () => {
  let createRoom;

  beforeEach(() => {
    createRoom = vi.spyOn(roomsApi, 'createRoom').mockResolvedValue({
      roomId: 'r-multi',
      playerToken: 'tok-multi',
      isSinglePlayer: false,
    });
    vi.spyOn(roomsApi, 'getRoomState').mockResolvedValue({
      roomPhase: 'LOBBY',
      timeRemaining: 30,
      players: [],
      playerCount: 1,
    });
    vi.stubGlobal('prompt', vi.fn(() => 'Alice'));
    stubNetwork();
  });

  it('opens the room ready overlay, never the game component waiting card', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /Play Minesweeper/ }));
    await user.click(screen.getByRole('button', { name: '+ Create Room' }));
    await user.type(screen.getByLabelText('Room Name'), 'Multi Room');
    await user.click(screen.getByRole('button', { name: 'Create Room' }));

    // "I'm Ready!" only renders for roomPhase === 'LOBBY' and it lives in
    // RoomLobby, so finding it proves the multiplayer room stayed in the lobby.
    const ready = await screen.findByRole('button', { name: "I'm Ready!" }, { timeout: 4000 });
    expect(ready).toBeInTheDocument();
    expect(createRoom).toHaveBeenCalledTimes(1);
    expect(createRoom.mock.calls[0][2].isSinglePlayer, 'the room is multiplayer').toBe(false);
    expect(createRoom.mock.calls[0][3], 'ownerId').toMatch(/^player-.+/);

    // the game component's waiting card has no ready button - reaching it here
    // is what made every multiplayer room unstartable
    expect(screen.queryByText('Waiting for players')).not.toBeInTheDocument();
  });
});

describe('solo non-blackjack rooms hand off from the lobby to the game', () => {
  // Coverage, not a regression guard: solo non-blackjack has always routed
  // through RoomLobby (6b.2 only changed the multiplayer branch). B5 covered
  // this path until F5 turned B5 into a multiplayer room, so nothing exercised
  // "solo room created in the lobby -> phase poll -> game component" after that.
  let createRoom;
  let leaveRoom;

  beforeEach(() => {
    createRoom = vi.spyOn(roomsApi, 'createRoom').mockResolvedValue({
      roomId: 'r-solo-handoff',
      playerToken: 'tok-solo',
      isSinglePlayer: true,
    });
    // solo rooms are created already PLAYING (GameRoomService.createRoom)
    vi.spyOn(roomsApi, 'getRoomState').mockResolvedValue({
      roomPhase: 'PLAYING',
      timeRemaining: 60,
      players: [],
      playerCount: 1,
    });
    leaveRoom = vi.spyOn(roomsApi, 'leaveRoom').mockResolvedValue(undefined);
    vi.stubGlobal('prompt', vi.fn(() => 'Alice'));
    stubNetwork();
  });

  it('opens the game component on the first poll and does not leave the room behind', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /Play Minesweeper/ }));
    await user.click(screen.getByRole('button', { name: '+ Create Room' }));
    await user.click(screen.getByRole('checkbox', { name: /Single Player/i }));
    await user.type(screen.getByLabelText('Room Name'), 'Solo Room');
    await user.click(screen.getByRole('button', { name: 'Create Room' }));

    expect(await screen.findByText('Minesweeper Lobby')).toBeInTheDocument();

    // the poll sees PLAYING and hands off to the game component
    await screen.findByRole('heading', { name: /^Minesweeper$/ }, { timeout: 4000 });
    expect(screen.queryByText('Minesweeper Lobby')).not.toBeInTheDocument();
    expect(createRoom.mock.calls[0][2].isSinglePlayer).toBe(true);

    // case 1 of the ownership effect: handing off must not abandon the room
    expect(leaveRoom).not.toHaveBeenCalled();
  });
});
