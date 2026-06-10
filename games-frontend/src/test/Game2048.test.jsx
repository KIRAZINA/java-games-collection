import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { Game2048 } from '../components/Game2048.jsx';

// ─── Mock the API module ──────────────────────────────────────────────────────
vi.mock('../api/api.js', () => ({
  game2048Api: {
    createSession: vi.fn(),
    move: vi.fn(),
    reset: vi.fn(),
    closeSession: vi.fn(),
  },
}));

import { game2048Api } from '../api/api.js';

// ─── Shared test state fixtures ───────────────────────────────────────────────
const initialState = {
  sessionId: 'sess-2048',
  board: [
    [0, 0, 0, 0],
    [0, 2, 0, 0],
    [0, 0, 0, 4],
    [0, 0, 0, 0]
  ],
  tiles: [
    { row: 1, col: 1, value: 2 },
    { row: 2, col: 3, value: 4 }
  ],
  score: 10,
  moved: false,
  gameOver: false
};

const gameOverState = {
  ...initialState,
  gameOver: true
};

describe('Game2048 Component', () => {
  const user = userEvent.setup();

  beforeEach(() => {
    vi.clearAllMocks();
    game2048Api.createSession.mockResolvedValue(initialState);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ── Rendering ──────────────────────────────────────────────────────────────

  it('renders loading state initially', () => {
    game2048Api.createSession.mockReturnValue(new Promise(() => {}));
    render(<Game2048 />);
    expect(screen.getByText('Loading')).toBeInTheDocument();
  });

  it('renders 16 grid cells and state details after creation', async () => {
    render(<Game2048 />);
    await waitFor(() => expect(screen.getByText('Score: 10')).toBeInTheDocument());

    // Check cells: there should be 16 tiles
    for (let i = 0; i < 16; i++) {
      expect(document.getElementById(`g2048-tile-${i}`)).toBeInTheDocument();
    }

    // Verify cell contents based on tiles
    // index 5 (row 1, col 1) should be 2
    expect(document.getElementById('g2048-tile-5')).toHaveTextContent('2');
    // index 11 (row 2, col 3) should be 4
    expect(document.getElementById('g2048-tile-11')).toHaveTextContent('4');
    // index 0 should be empty
    expect(document.getElementById('g2048-tile-0')).toHaveTextContent('');
  });

  // ── Score updates from state ───────────────────────────────────────────────

  it('score updates from state', async () => {
    render(<Game2048 />);
    await waitFor(() => expect(screen.getByText('Score: 10')).toBeInTheDocument());
  });

  // ── Keyboard Moves ─────────────────────────────────────────────────────────

  it('arrow key UP triggers move UP', async () => {
    game2048Api.move.mockResolvedValue({
      ...initialState,
      score: 14,
      moved: true,
      tiles: [
        { row: 0, col: 1, value: 2 },
        { row: 0, col: 3, value: 4 }
      ]
    });

    render(<Game2048 />);
    await waitFor(() => expect(screen.getByText('Score: 10')).toBeInTheDocument());

    fireEvent.keyDown(window, { key: 'ArrowUp' });

    await waitFor(() => {
      expect(game2048Api.move).toHaveBeenCalledWith('sess-2048', 'UP');
      expect(screen.getByText('Score: 14')).toBeInTheDocument();
    });
  });

  it('arrow key DOWN triggers move DOWN', async () => {
    game2048Api.move.mockResolvedValue(initialState);
    render(<Game2048 />);
    await waitFor(() => expect(screen.getByText('Score: 10')).toBeInTheDocument());

    fireEvent.keyDown(window, { key: 'ArrowDown' });

    await waitFor(() => {
      expect(game2048Api.move).toHaveBeenCalledWith('sess-2048', 'DOWN');
    });
  });

  it('arrow key LEFT triggers move LEFT', async () => {
    game2048Api.move.mockResolvedValue(initialState);
    render(<Game2048 />);
    await waitFor(() => expect(screen.getByText('Score: 10')).toBeInTheDocument());

    fireEvent.keyDown(window, { key: 'ArrowLeft' });

    await waitFor(() => {
      expect(game2048Api.move).toHaveBeenCalledWith('sess-2048', 'LEFT');
    });
  });

  it('arrow key RIGHT triggers move RIGHT', async () => {
    game2048Api.move.mockResolvedValue(initialState);
    render(<Game2048 />);
    await waitFor(() => expect(screen.getByText('Score: 10')).toBeInTheDocument());

    fireEvent.keyDown(window, { key: 'ArrowRight' });

    await waitFor(() => {
      expect(game2048Api.move).toHaveBeenCalledWith('sess-2048', 'RIGHT');
    });
  });

  // ── Button Moves ───────────────────────────────────────────────────────────

  it('clicking move-pad buttons triggers move API calls', async () => {
    game2048Api.move.mockResolvedValue(initialState);
    render(<Game2048 />);
    await waitFor(() => expect(screen.getByText('Score: 10')).toBeInTheDocument());

    await user.click(screen.getByRole('button', { name: '↑' }));
    expect(game2048Api.move).toHaveBeenLastCalledWith('sess-2048', 'UP');

    await user.click(screen.getByRole('button', { name: '←' }));
    expect(game2048Api.move).toHaveBeenLastCalledWith('sess-2048', 'LEFT');

    await user.click(screen.getByRole('button', { name: '→' }));
    expect(game2048Api.move).toHaveBeenLastCalledWith('sess-2048', 'RIGHT');

    await user.click(screen.getByRole('button', { name: '↓' }));
    expect(game2048Api.move).toHaveBeenLastCalledWith('sess-2048', 'DOWN');
  });

  // ── Reset ──────────────────────────────────────────────────────────────────

  it('clicking reset button triggers reset API', async () => {
    game2048Api.reset.mockResolvedValue({
      ...initialState,
      score: 0
    });

    render(<Game2048 />);
    await waitFor(() => expect(screen.getByText('Score: 10')).toBeInTheDocument());

    const resetButton = screen.getByRole('button', { name: /Reset/i });
    await user.click(resetButton);

    await waitFor(() => {
      expect(game2048Api.reset).toHaveBeenCalledWith('sess-2048');
      expect(screen.getByText('Score: 0')).toBeInTheDocument();
    });
  });

  // ── Game Over state ────────────────────────────────────────────────────────

  it('shows game over text and disables move actions when gameOver=true', async () => {
    game2048Api.createSession.mockResolvedValue(gameOverState);
    render(<Game2048 />);

    await waitFor(() => {
      expect(screen.getByText('No moves left')).toBeInTheDocument();
    });

    // Make sure typing arrow keys doesn't invoke move when gameOver is true
    fireEvent.keyDown(window, { key: 'ArrowUp' });
    expect(game2048Api.move).not.toHaveBeenCalled();
  });

  // ── Error handling ─────────────────────────────────────────────────────────

  it('shows error message when API throws', async () => {
    game2048Api.createSession.mockRejectedValue(new Error('Failed to create session'));
    render(<Game2048 />);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Failed to create session');
    });
  });
});
