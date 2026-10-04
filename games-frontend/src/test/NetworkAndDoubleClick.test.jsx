import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { Blackjack } from '../components/Blackjack.jsx';
import { Minesweeper } from '../components/Minesweeper.jsx';
import { Game2048 } from '../components/Game2048.jsx';
import { RoomLobby } from '../components/RoomLobby.jsx';

vi.mock('../api/api.js', () => ({
  blackjackApi: {
    createSession: vi.fn(), getState: vi.fn(), startRound: vi.fn(),
    placeBet: vi.fn(), hit: vi.fn(), stand: vi.fn(), closeSession: vi.fn(),
  },
  minesweeperApi: {
    createSession: vi.fn(), getState: vi.fn(), open: vi.fn(), toggleFlag: vi.fn(),
    reset: vi.fn(), nextBoard: vi.fn(), closeSession: vi.fn(),
  },
  game2048Api: {
    createSession: vi.fn(), getState: vi.fn(), move: vi.fn(), reset: vi.fn(), closeSession: vi.fn(),
  },
  roomsApi: {
    listRooms: vi.fn(), listAllRooms: vi.fn(), getRoom: vi.fn(), createRoom: vi.fn(),
    joinRoom: vi.fn(), leaveRoom: vi.fn(), deleteRoom: vi.fn(), getRoomState: vi.fn(),
    getRoomProgress: vi.fn(), registerSession: vi.fn(), getRoomsForPlayer: vi.fn(),
    markReady: vi.fn(),
  },
  setRoomToken: vi.fn(),
  clearRoomToken: vi.fn(),
  getRoomToken: vi.fn(),
  rehydrateRoomTokens: vi.fn(),
  extractRoomIdFromPath: vi.fn(),
}));

import { blackjackApi, minesweeperApi, game2048Api, roomsApi } from '../api/api.js';

const NETWORK_MSG =
  'Unable to reach the server. Please ensure the backend is running and try again.';

const bettingState = {
  sessionId: 'bj-1', phase: 'BETTING', winner: 'NONE', difficulty: 'BASIC',
  balance: 100, currentBet: 0, playerCards: [], playerValue: 0,
  dealerCards: [], dealerValue: null, cardsRemaining: 52, canContinue: true,
  notifications: [],
};

const playerTurnState = {
  ...bettingState, phase: 'PLAYER_TURN', balance: 90, currentBet: 10,
  playerCards: [{ rank: '10', suit: 'HEARTS' }, { rank: '5', suit: 'SPADES' }],
  playerValue: 15,
  dealerCards: [{ rank: 'K', suit: 'DIAMONDS' }, { rank: '7', suit: 'CLUBS' }],
  dealerValue: 17,
};

const minesweeperState = {
  sessionId: 'ms-1', rows: 9, cols: 9, totalMines: 10, flagsPlaced: 0,
  remainingMines: 10, firstClickDone: false, gameOver: false, won: false,
  boardsCleared: 0,
  cells: Array.from({ length: 81 }, (_, i) => ({
    row: Math.floor(i / 9), col: i % 9, state: 'COVERED', adjacentMines: 0, mine: false,
  })),
  score: 0, isLocked: false,
};

const g2048State = {
  sessionId: 'g-1', size: 4, score: 0, gameOver: false, moved: false,
  movesMade: 0, tiles: [{ row: 0, col: 0, value: 2 }, { row: 0, col: 1, value: 4 }],
  iceBlockCount: 0,
};

const roomList = [{
  roomId: 'r-lobby', roomName: 'Host Room', gameType: 'BLACKJACK', phase: 'LOBBY',
  playerCount: 1, maxPlayers: 4, spectatorCount: 0, passwordProtected: false,
  ownerName: 'Host', createdAt: 't', lastActivity: 't', timeLimitSeconds: 0,
  isSinglePlayer: false,
}];

function deferred() {
  let resolve, reject;
  const promise = new Promise((res, rej) => { resolve = res; reject = rej; });
  return { promise, resolve, reject };
}

/** Drives RoomLobby into the solo-room overlay so the Ready button can render. */
async function openSoloRoomOverlay(user, { rejectReady } = {}) {
  roomsApi.listRooms.mockResolvedValue([]);
  roomsApi.createRoom.mockResolvedValue({
    roomId: 'r-solo', playerToken: 'tok', isSinglePlayer: true,
  });
  roomsApi.getRoomState.mockResolvedValue({
    roomPhase: 'LOBBY', timeRemaining: 30, players: [], playerCount: 1,
  });
  if (rejectReady) {
    roomsApi.markReady.mockRejectedValue(rejectReady);
  }

  render(<RoomLobby gameKey="minesweeper" playerId="p-1" playerName="Pat" />);
  await user.click(await screen.findByRole('button', { name: '+ Create Room' }));
  await user.click(screen.getByRole('checkbox', { name: /Single Player/i }));
  await user.click(screen.getByRole('button', { name: 'Create Room' }));

  const readyButton = await screen.findByRole('button', { name: "I'm Ready!" }, { timeout: 4000 });
  return readyButton;
}

describe('Area 1 - network failure during mutating actions', () => {
  const user = userEvent.setup();

  beforeEach(() => {
    vi.clearAllMocks();
    blackjackApi.createSession.mockResolvedValue(bettingState);
    minesweeperApi.createSession.mockResolvedValue(minesweeperState);
    game2048Api.createSession.mockResolvedValue(g2048State);
    roomsApi.listRooms.mockResolvedValue(roomList);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('placeBet failure: error surfaced, busy cleared, no partial state', async () => {
    blackjackApi.placeBet.mockRejectedValue(new Error(NETWORK_MSG));
    render(<Blackjack />);
    const betButton = await screen.findByRole('button', { name: 'Place Bet' });
    await user.click(betButton);

    await screen.findByRole('alert');
    expect(screen.getByRole('alert')).toHaveTextContent(NETWORK_MSG);
    expect(screen.getByRole('button', { name: 'Place Bet' })).toBeEnabled();
    expect(blackjackApi.placeBet).toHaveBeenCalledTimes(1);
    expect(screen.getByText('$100.00')).toBeInTheDocument();
  });

  it('hit failure: error surfaced, busy cleared, hand unchanged', async () => {
    blackjackApi.createSession.mockResolvedValue(playerTurnState);
    blackjackApi.hit.mockRejectedValue(new Error(NETWORK_MSG));
    render(<Blackjack />);
    const hitButton = await screen.findByRole('button', { name: 'Hit' });
    await user.click(hitButton);

    await screen.findByRole('alert');
    expect(screen.getByRole('alert')).toHaveTextContent(NETWORK_MSG);
    expect(screen.getByRole('button', { name: 'Hit' })).toBeEnabled();
    expect(blackjackApi.hit).toHaveBeenCalledTimes(1);
    expect(screen.getByText('15')).toBeInTheDocument();
  });

  it('stand failure: error surfaced, busy cleared, state unchanged', async () => {
    blackjackApi.createSession.mockResolvedValue(playerTurnState);
    blackjackApi.stand.mockRejectedValue(new Error(NETWORK_MSG));
    render(<Blackjack />);
    const standButton = await screen.findByRole('button', { name: 'Stand' });
    await user.click(standButton);

    await screen.findByRole('alert');
    expect(screen.getByRole('alert')).toHaveTextContent(NETWORK_MSG);
    expect(screen.getByRole('button', { name: 'Stand' })).toBeEnabled();
    expect(blackjackApi.stand).toHaveBeenCalledTimes(1);
    // The bet span renders as "Bet: $10.00"; match its full node text.
    expect(screen.getByText(/Bet: \$10\.00/)).toBeInTheDocument();
  });

  it('open failure: error surfaced, busy cleared, board unchanged', async () => {
    minesweeperApi.open.mockRejectedValue(new Error(NETWORK_MSG));
    render(<Minesweeper />);
    const cell = await screen.findByLabelText('Row 1, column 1');
    await user.click(cell);

    await screen.findByRole('alert');
    expect(screen.getByRole('alert')).toHaveTextContent(NETWORK_MSG);
    expect(screen.getByLabelText('Row 1, column 1')).toBeEnabled();
    expect(minesweeperApi.open).toHaveBeenCalledTimes(1);
    expect(screen.getByText(/Score: 0/)).toBeInTheDocument();
  });

  it('flag failure: error surfaced, busy cleared, flags unchanged', async () => {
    minesweeperApi.toggleFlag.mockRejectedValue(new Error(NETWORK_MSG));
    render(<Minesweeper />);
    const cell = await screen.findByLabelText('Row 1, column 1');
    await user.pointer({ target: cell, keys: '[MouseRight]' });

    await screen.findByRole('alert');
    expect(screen.getByRole('alert')).toHaveTextContent(NETWORK_MSG);
    expect(screen.getByLabelText('Row 1, column 1')).toBeEnabled();
    expect(minesweeperApi.toggleFlag).toHaveBeenCalledTimes(1);
    expect(screen.getByText(/Flags: 0/)).toBeInTheDocument();
  });

  it('move failure: error surfaced, busy cleared, board unchanged', async () => {
    game2048Api.move.mockRejectedValue(new Error(NETWORK_MSG));
    const { container } = render(<Game2048 />);
    const upButton = await screen.findByRole('button', { name: '↑' });
    expect(container.querySelector('#g2048-up')).toBe(upButton);
    await user.click(upButton);

    await screen.findByRole('alert');
    expect(screen.getByRole('alert')).toHaveTextContent(NETWORK_MSG);
    expect(upButton).toBeEnabled();
    expect(game2048Api.move).toHaveBeenCalledTimes(1);
    expect(screen.getByText(/Score: 0/)).toBeInTheDocument();
  });

  it('join failure: error surfaced, busy cleared, join button usable again', async () => {
    roomsApi.joinRoom.mockRejectedValue(new Error('Room is full'));
    render(<RoomLobby gameKey="blackjack" playerId="p-1" playerName="Pat" />);
    const joinButton = await screen.findByRole('button', { name: 'Join' });
    await user.click(joinButton);

    await screen.findByRole('alert');
    expect(screen.getByRole('alert')).toHaveTextContent('Room is full');
    expect(roomsApi.joinRoom).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('button', { name: 'Join' })).toBeEnabled();
  });

  it('ready failure surfaces an error (previously swallowed silently)', async () => {
    const userLocal = userEvent.setup();
    await openSoloRoomOverlay(userLocal, {
      rejectReady: new Error('Invalid or missing player token'),
    });
    await userLocal.click(screen.getByRole('button', { name: "I'm Ready!" }));

    const alert = await screen.findByRole('alert', {}, { timeout: 3000 });
    expect(alert).toHaveTextContent('Invalid or missing player token');
    expect(roomsApi.markReady).toHaveBeenCalledTimes(1);
  });
});

describe('Area 2 - rapid repeated clicks send a single request', () => {
  const user = userEvent.setup();

  beforeEach(() => {
    vi.clearAllMocks();
    blackjackApi.createSession.mockResolvedValue(bettingState);
    minesweeperApi.createSession.mockResolvedValue(minesweeperState);
    game2048Api.createSession.mockResolvedValue(g2048State);
    roomsApi.listRooms.mockResolvedValue(roomList);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('double-click Place Bet: one request, disabled while pending, re-enabled after', async () => {
    const gate = deferred();
    blackjackApi.placeBet.mockReturnValue(gate.promise);
    render(<Blackjack />);
    const betButton = await screen.findByRole('button', { name: 'Place Bet' });

    await user.click(betButton);
    expect(blackjackApi.placeBet).toHaveBeenCalledTimes(1);
    expect(betButton).toBeDisabled();

    await user.click(betButton);
    expect(blackjackApi.placeBet).toHaveBeenCalledTimes(1);

    gate.resolve(bettingState);
    await waitFor(() => expect(betButton).toBeEnabled());
    expect(blackjackApi.placeBet).toHaveBeenCalledTimes(1);
  });

  it('double-click a cell: one open request, disabled while pending', async () => {
    const gate = deferred();
    minesweeperApi.open.mockReturnValue(gate.promise);
    render(<Minesweeper />);
    const cell = await screen.findByLabelText('Row 1, column 1');

    await user.click(cell);
    expect(minesweeperApi.open).toHaveBeenCalledTimes(1);
    expect(cell).toBeDisabled();

    await user.click(cell);
    expect(minesweeperApi.open).toHaveBeenCalledTimes(1);

    gate.resolve(minesweeperState);
    await waitFor(() => expect(cell).toBeEnabled());
    expect(minesweeperApi.open).toHaveBeenCalledTimes(1);
  });

  it('double-click a move button: one move request, disabled while pending', async () => {
    const gate = deferred();
    game2048Api.move.mockReturnValue(gate.promise);
    render(<Game2048 />);
    const upButton = await screen.findByRole('button', { name: '↑' });

    await user.click(upButton);
    expect(game2048Api.move).toHaveBeenCalledTimes(1);
    expect(upButton).toBeDisabled();

    await user.click(upButton);
    expect(game2048Api.move).toHaveBeenCalledTimes(1);

    gate.resolve(g2048State);
    await waitFor(() => expect(upButton).toBeEnabled());
    expect(game2048Api.move).toHaveBeenCalledTimes(1);
  });

  it('double-click Reset: one reset request (previously unguarded)', async () => {
    const gate = deferred();
    game2048Api.reset.mockReturnValue(gate.promise);
    const { container } = render(<Game2048 />);
    await screen.findByRole('button', { name: '↑' });
    const resetButton = container.querySelector('#g2048-reset');
    expect(resetButton).toBeEnabled();

    await user.click(resetButton);
    await user.click(resetButton);

    expect(game2048Api.reset).toHaveBeenCalledTimes(1);

    gate.resolve(g2048State);
    await waitFor(() => expect(resetButton).toBeEnabled());
  });

  it('double-click Join: one join request', async () => {
    const gate = deferred();
    roomsApi.joinRoom.mockReturnValue(gate.promise);
    render(<RoomLobby gameKey="blackjack" playerId="p-1" playerName="Pat" />);
    const joinButton = await screen.findByRole('button', { name: 'Join' });

    await user.click(joinButton);
    expect(roomsApi.joinRoom).toHaveBeenCalledTimes(1);
    expect(joinButton).toBeDisabled();

    await user.click(joinButton);
    expect(roomsApi.joinRoom).toHaveBeenCalledTimes(1);

    // The room must keep reporting LOBBY or the poll's error path replaces the
    // overlay again; stub it before the join resolves.
    roomsApi.getRoomState.mockResolvedValue({
      roomPhase: 'LOBBY', timeRemaining: 30, players: [], playerCount: 1,
    });
    gate.resolve({ roomId: 'r-lobby', playerToken: 'tok', isSinglePlayer: false });
    // G1 (step 6b.2): a join no longer re-renders the same list row - the room
    // hands over to its ready overlay - so the busy state is proven by that
    // hand-off.
    await screen.findByRole('button', { name: "I'm Ready!" }, { timeout: 4000 });
    expect(roomsApi.joinRoom).toHaveBeenCalledTimes(1);
  });

  it('double-click "I\u2019m Ready!": one markReady request (previously unguarded)', async () => {
    const userLocal = userEvent.setup();
    const gate = deferred();
    await openSoloRoomOverlay(userLocal);
    roomsApi.markReady.mockReturnValue(gate.promise);

    await userLocal.click(screen.getByRole('button', { name: "I'm Ready!" }));
    await userLocal.click(screen.getByRole('button', { name: "I'm Ready!" }));

    expect(roomsApi.markReady).toHaveBeenCalledTimes(1);

    gate.resolve({});
    await waitFor(() =>
      expect(screen.getByText(/Waiting for all players/i)).toBeInTheDocument()
    );
    expect(roomsApi.markReady).toHaveBeenCalledTimes(1);
  });
});
