import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { Minesweeper } from '../components/Minesweeper.jsx';

// ─── Mock the API module ──────────────────────────────────────────────────────
vi.mock('../api/api.js', () => ({
  minesweeperApi: {
    createSession: vi.fn(),
    open: vi.fn(),
    toggleFlag: vi.fn(),
    reset: vi.fn(),
    closeSession: vi.fn(),
  },
  // GameHeader is imported in Minesweeper via Blackjack — stub it
  blackjackApi: {},
}));

import { minesweeperApi } from '../api/api.js';

// ─── Fixtures ─────────────────────────────────────────────────────────────────

/** Builds a 9×9 grid of 81 covered cell views */
function makeCells(rows = 9, cols = 9) {
  const cells = [];
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      cells.push({ row: r, col: c, state: 'COVERED', adjacentMines: 0, mine: false });
    }
  }
  return cells;
}

const initialState = {
  sessionId: 'ms-1',
  rows: 9,
  cols: 9,
  totalMines: 10,
  flagsPlaced: 0,
  remainingMines: 10,
  firstClickDone: false,
  gameOver: false,
  won: false,
  cells: makeCells(),
};

const afterFirstClick = {
  ...initialState,
  firstClickDone: true,
  cells: makeCells().map((c) =>
    c.row === 4 && c.col === 4 ? { ...c, state: 'OPENED' } : c
  ),
};

const gameOverLostState = {
  ...afterFirstClick,
  gameOver: true,
  won: false,
};

const gameOverWonState = {
  ...afterFirstClick,
  gameOver: true,
  won: true,
};

// ─── Tests ────────────────────────────────────────────────────────────────────
describe('Minesweeper Component', () => {
  const user = userEvent.setup();

  beforeEach(() => {
    vi.clearAllMocks();
    minesweeperApi.createSession.mockResolvedValue(initialState);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ── Rendering ──────────────────────────────────────────────────────────────

  it('renders 81 cell buttons for a 9×9 board', async () => {
    render(<Minesweeper />);
    await waitFor(() =>
      expect(screen.getAllByRole('button', { name: /Row \d+, column \d+/i })).toHaveLength(81)
    );
  });

  it('shows flag count in status strip', async () => {
    render(<Minesweeper />);
    await waitFor(() =>
      expect(screen.getByText(/Flags: 0/)).toBeInTheDocument()
    );
  });

  it('shows remaining mines in status strip', async () => {
    render(<Minesweeper />);
    await waitFor(() =>
      expect(screen.getByText(/Remaining: 10/)).toBeInTheDocument()
    );
  });

  it('shows score of 0 before first click', async () => {
    render(<Minesweeper />);
    await waitFor(() =>
      expect(screen.getByText(/Score: 0/)).toBeInTheDocument()
    );
  });

  // ── Interactions ───────────────────────────────────────────────────────────

  it('left-click on a cell calls the open endpoint', async () => {
    minesweeperApi.open.mockResolvedValue(afterFirstClick);
    render(<Minesweeper />);
    await waitFor(() => screen.getAllByRole('button', { name: /Row \d+, column \d+/i }));

    const cell44 = screen.getByLabelText('Row 5, column 5'); // 0-indexed row=4,col=4 → label uses +1
    await user.click(cell44);

    expect(minesweeperApi.open).toHaveBeenCalledWith('ms-1', 4, 4);
  });

  it('right-click on a cell calls the toggleFlag endpoint', async () => {
    minesweeperApi.toggleFlag.mockResolvedValue({
      ...initialState,
      flagsPlaced: 1,
      remainingMines: 9,
      cells: makeCells().map((c) =>
        c.row === 0 && c.col === 0 ? { ...c, state: 'FLAGGED' } : c
      ),
    });
    render(<Minesweeper />);
    await waitFor(() => screen.getAllByRole('button', { name: /Row \d+, column \d+/i }));

    const cell00 = screen.getByLabelText('Row 1, column 1');
    await user.pointer({ target: cell00, keys: '[MouseRight]' });

    expect(minesweeperApi.toggleFlag).toHaveBeenCalledWith('ms-1', 0, 0);
  });

  it('shows updated flag count after flagging a cell', async () => {
    minesweeperApi.toggleFlag.mockResolvedValue({
      ...initialState,
      flagsPlaced: 1,
      remainingMines: 9,
      cells: makeCells().map((c) =>
        c.row === 0 && c.col === 0 ? { ...c, state: 'FLAGGED' } : c
      ),
    });
    render(<Minesweeper />);
    await waitFor(() => screen.getAllByRole('button', { name: /Row \d+, column \d+/i }));

    const cell = screen.getByLabelText('Row 1, column 1');
    await user.pointer({ target: cell, keys: '[MouseRight]' });

    await waitFor(() => expect(screen.getByText(/Flags: 1/)).toBeInTheDocument());
  });

  // ── Game Over ──────────────────────────────────────────────────────────────

  it('all cell buttons are disabled after game over (lost)', async () => {
    minesweeperApi.createSession.mockResolvedValue(gameOverLostState);
    render(<Minesweeper />);
    await waitFor(() => {
      const buttons = screen.getAllByRole('button', { name: /Row \d+, column \d+/i });
      buttons.forEach((btn) => expect(btn).toBeDisabled());
    });
  });

  it('shows "💥 Lost" meta after losing', async () => {
    minesweeperApi.createSession.mockResolvedValue(gameOverLostState);
    render(<Minesweeper />);
    await waitFor(() =>
      expect(screen.getByText(/Lost/)).toBeInTheDocument()
    );
  });

  it('shows "🏆 Won" meta after winning', async () => {
    minesweeperApi.createSession.mockResolvedValue(gameOverWonState);
    render(<Minesweeper />);
    await waitFor(() =>
      expect(screen.getByText(/Won/)).toBeInTheDocument()
    );
  });

  // ── Error Handling ─────────────────────────────────────────────────────────

  it('shows error message when createSession fails', async () => {
    minesweeperApi.createSession.mockRejectedValue(new Error('Board error'));
    render(<Minesweeper />);
    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('Board error')
    );
  });
});
