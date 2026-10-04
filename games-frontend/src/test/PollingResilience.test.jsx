import { render, screen, act } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { Blackjack } from '../components/Blackjack.jsx';
import { Minesweeper } from '../components/Minesweeper.jsx';
import { Game2048 } from '../components/Game2048.jsx';

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
    getRoomState: vi.fn(), getRoomProgress: vi.fn(), registerSession: vi.fn(),
    markReady: vi.fn(), leaveRoom: vi.fn(),
  },
}));

import { blackjackApi, minesweeperApi, game2048Api, roomsApi } from '../api/api.js';

const bettingState = {
  sessionId: 'bj-1', phase: 'BETTING', winner: 'NONE', difficulty: 'BASIC',
  balance: 100, currentBet: 0, playerCards: [], playerValue: 0,
  dealerCards: [], dealerValue: null, cardsRemaining: 52, canContinue: true,
  notifications: [],
};

const minesweeperState = {
  sessionId: 'ms-1', rows: 9, cols: 9, totalMines: 10, flagsPlaced: 0,
  remainingMines: 10, firstClickDone: false, gameOver: false, won: false,
  boardsCleared: 0, score: 0, isLocked: false,
  cells: Array.from({ length: 81 }, (_, i) => ({
    row: Math.floor(i / 9), col: i % 9, state: 'COVERED', adjacentMines: 0, mine: false,
  })),
};

const g2048State = {
  sessionId: 'g-1', size: 4, score: 0, gameOver: false, moved: false,
  movesMade: 0, tiles: [{ row: 0, col: 0, value: 2 }], iceBlockCount: 0,
};

const playingState = { roomPhase: 'PLAYING', timeRemaining: 60, players: [] };

function errWithStatus(status, message) {
  return Object.assign(new Error(message), { status });
}

async function flushMicrotasks() {
  await act(async () => {});
}

async function advance(ms) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms);
  });
}

describe('Area 3 - polling cleanup and error resilience', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
    blackjackApi.createSession.mockResolvedValue(bettingState);
    minesweeperApi.createSession.mockResolvedValue(minesweeperState);
    game2048Api.createSession.mockResolvedValue(g2048State);
    roomsApi.getRoomState.mockResolvedValue(playingState);
    roomsApi.getRoomProgress.mockResolvedValue(playingState);
    roomsApi.registerSession.mockResolvedValue({});
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  // ── Unmount cleanup ────────────────────────────────────────────────────────

  it('unmounting before the first poll resolves issues no further requests and warns nothing', async () => {
    const errSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    let resolvePoll;
    roomsApi.getRoomState.mockReturnValue(new Promise((res) => { resolvePoll = res; }));

    const { unmount } = render(
      <Blackjack roomId="r-1" playerId="p-1" playerName="Pat" />
    );
    await flushMicrotasks();

    await advance(1000);
    // Step 6e B1: /state now fires once on mount AND once on the 1s tick -
    // two in-flight (still-pending) requests before unmount.
    expect(roomsApi.getRoomState).toHaveBeenCalledTimes(2);

    unmount();
    resolvePoll(playingState);
    await advance(10000);

    expect(roomsApi.getRoomState).toHaveBeenCalledTimes(2);
    expect(roomsApi.getRoomProgress).not.toHaveBeenCalled();
    expect(errSpy).not.toHaveBeenCalled();
    errSpy.mockRestore();
  });

  // ── 404: room gone ─────────────────────────────────────────────────────────

  it('Blackjack: state poll 404 shows "room no longer exists" and stops polling (no stuck spinner)', async () => {
    const gone = errWithStatus(404, 'Room not found: r-1');
    roomsApi.getRoomState.mockRejectedValue(gone);
    roomsApi.getRoomProgress.mockRejectedValue(gone);

    render(<Blackjack roomId="r-1" playerId="p-1" playerName="Pat" />);
    await flushMicrotasks();

    await advance(1000);

    expect(screen.getByRole('alert')).toHaveTextContent(/room no longer exists/i);
    expect(screen.queryByText('Waiting for players')).not.toBeInTheDocument();

    const callsAfterStop = roomsApi.getRoomState.mock.calls.length;
    expect(callsAfterStop).toBeGreaterThan(0);

    await advance(10000);
    expect(roomsApi.getRoomState).toHaveBeenCalledTimes(callsAfterStop);
    expect(screen.queryByText('Waiting for players')).not.toBeInTheDocument();
  });

  it('Minesweeper: state poll 404 shows "room no longer exists" and stops polling', async () => {
    const gone = errWithStatus(404, 'Room not found: r-2');
    roomsApi.getRoomState.mockRejectedValue(gone);
    roomsApi.getRoomProgress.mockRejectedValue(gone);

    render(<Minesweeper roomId="r-2" playerId="p-1" playerName="Pat" />);
    await flushMicrotasks();

    await advance(1000);

    expect(screen.getByRole('alert')).toHaveTextContent(/room no longer exists/i);
    expect(screen.queryByText('Waiting for players')).not.toBeInTheDocument();

    const callsAfterStop = roomsApi.getRoomState.mock.calls.length;
    await advance(10000);
    expect(roomsApi.getRoomState).toHaveBeenCalledTimes(callsAfterStop);
  });

  it('Game2048: state poll 404 shows "room no longer exists" and stops polling', async () => {
    const gone = errWithStatus(404, 'Room not found: r-3');
    roomsApi.getRoomState.mockRejectedValue(gone);
    roomsApi.getRoomProgress.mockRejectedValue(gone);

    render(<Game2048 roomId="r-3" playerId="p-1" playerName="Pat" />);
    await flushMicrotasks();

    await advance(1000);

    expect(screen.getByRole('alert')).toHaveTextContent(/room no longer exists/i);
    expect(screen.queryByText('Waiting for players')).not.toBeInTheDocument();

    const callsAfterStop = roomsApi.getRoomState.mock.calls.length;
    await advance(10000);
    expect(roomsApi.getRoomState).toHaveBeenCalledTimes(callsAfterStop);
  });

  // ── 500: transient vs sustained ────────────────────────────────────────────

  it('a single 500 does NOT stop polling: the next tick still polls and no error shows', async () => {
    roomsApi.getRoomState
      .mockRejectedValueOnce(errWithStatus(500, 'boom'))
      .mockResolvedValue(playingState);

    render(<Blackjack roomId="r-1" playerId="p-1" playerName="Pat" />);
    await flushMicrotasks();

    // Step 6e B1: the initial fetch fires on mount and eats the single 500
    // (tolerated); both 1s ticks after it succeed.
    await advance(1000); // tick 1 → success
    await advance(1000); // tick 2 → success

    expect(roomsApi.getRoomState).toHaveBeenCalledTimes(3);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.getByText('BETTING')).toBeInTheDocument();
  });

  it('repeated 500s exhaust the bounded retries, stop polling, and surface an error mid-game', async () => {
    roomsApi.getRoomState
      .mockResolvedValueOnce(playingState)
      .mockRejectedValue(errWithStatus(500, 'boom'));
    roomsApi.getRoomProgress.mockRejectedValue(errWithStatus(500, 'boom'));

    render(<Blackjack roomId="r-1" playerId="p-1" playerName="Pat" />);
    await flushMicrotasks();

    await advance(1000); // tick 1 → success → PLAYING (in game)
    expect(screen.getByText('BETTING')).toBeInTheDocument();

    await advance(1000); // failure 1
    await advance(1000); // failure 2
    await advance(1000); // failure 3 → give up

    expect(screen.getByRole('alert')).toHaveTextContent(/lost connection/i);

    const callsAtStop = roomsApi.getRoomState.mock.calls.length;
    await advance(10000);
    expect(roomsApi.getRoomState).toHaveBeenCalledTimes(callsAtStop);
  });
});
