import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { Blackjack } from '../components/Blackjack.jsx';

// ─── Mock the API module ──────────────────────────────────────────────────────
vi.mock('../api/api.js', () => ({
  blackjackApi: {
    createSession: vi.fn(),
    startRound: vi.fn(),
    placeBet: vi.fn(),
    hit: vi.fn(),
    stand: vi.fn(),
    closeSession: vi.fn(),
  },
}));

import { blackjackApi } from '../api/api.js';

// ─── Shared test state fixtures ───────────────────────────────────────────────
const bettingState = {
  sessionId: 'sess-1',
  phase: 'BETTING',
  winner: 'NONE',
  balance: 100.0,
  currentBet: 0,
  playerCards: [],
  dealerCards: [],
  playerValue: 0,
  dealerValue: null,
  cardsRemaining: 52,
  canContinue: true,
};

const playerTurnState = {
  ...bettingState,
  phase: 'PLAYER_TURN',
  balance: 90.0,
  currentBet: 10.0,
  playerCards: [{ rank: 'KING', suit: 'SPADES' }, { rank: 'SEVEN', suit: 'HEARTS' }],
  playerValue: 17,
  dealerCards: [{ rank: 'EIGHT', suit: 'CLUBS' }],
  dealerValue: null,
};

// ─── Tests ────────────────────────────────────────────────────────────────────
describe('Blackjack Component', () => {
  const user = userEvent.setup();

  beforeEach(() => {
    vi.clearAllMocks();
    blackjackApi.createSession.mockResolvedValue(bettingState);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ── Rendering ──────────────────────────────────────────────────────────────

  it('renders "Loading" meta while API call is in flight', () => {
    // Don't resolve the promise yet
    blackjackApi.createSession.mockReturnValue(new Promise(() => {}));
    render(<Blackjack />);
    expect(screen.getByText('Loading')).toBeInTheDocument();
  });

  it('displays BETTING phase after session is created', async () => {
    render(<Blackjack />);
    await waitFor(() => expect(screen.getByText('BETTING')).toBeInTheDocument());
  });

  it('displays balance from server state', async () => {
    render(<Blackjack />);
    await waitFor(() => expect(screen.getByText(/Balance: \$100\.00/)).toBeInTheDocument());
  });

  // ── Button States ──────────────────────────────────────────────────────────

  it('Place Bet button is enabled in BETTING phase', async () => {
    render(<Blackjack />);
    await waitFor(() => screen.getByText('BETTING'));
    expect(screen.getByRole('button', { name: /Place Bet/i })).not.toBeDisabled();
  });

  it('Hit button is disabled in BETTING phase (not PLAYER_TURN)', async () => {
    render(<Blackjack />);
    await waitFor(() => screen.getByText('BETTING'));
    expect(screen.getByRole('button', { name: /Hit/i })).toBeDisabled();
  });

  it('Stand button is disabled in BETTING phase (not PLAYER_TURN)', async () => {
    render(<Blackjack />);
    await waitFor(() => screen.getByText('BETTING'));
    expect(screen.getByRole('button', { name: /Stand/i })).toBeDisabled();
  });

  it('Hit and Stand are enabled during PLAYER_TURN', async () => {
    blackjackApi.createSession.mockResolvedValue(playerTurnState);
    render(<Blackjack />);
    await waitFor(() => screen.getByText('PLAYER_TURN'));
    expect(screen.getByRole('button', { name: /Hit/i })).not.toBeDisabled();
    expect(screen.getByRole('button', { name: /Stand/i })).not.toBeDisabled();
  });

  it('Place Bet is disabled during PLAYER_TURN', async () => {
    blackjackApi.createSession.mockResolvedValue(playerTurnState);
    render(<Blackjack />);
    await waitFor(() => screen.getByText('PLAYER_TURN'));
    expect(screen.getByRole('button', { name: /Place Bet/i })).toBeDisabled();
  });

  // ── Error Handling ─────────────────────────────────────────────────────────

  it('shows error message when API throws', async () => {
    blackjackApi.createSession.mockRejectedValue(new Error('Server error'));
    render(<Blackjack />);
    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('Server error')
    );
  });

  it('clears error on successful retry', async () => {
    blackjackApi.createSession
      .mockRejectedValueOnce(new Error('Server error'))
      .mockResolvedValueOnce(bettingState);

    render(<Blackjack />);
    await waitFor(() => screen.getByRole('alert'));

    await user.click(screen.getByRole('button', { name: /New Session/i }));
    await waitFor(() =>
      expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    );
  });

  // ── Interactions ───────────────────────────────────────────────────────────

  it('New Session button calls createSession API', async () => {
    render(<Blackjack />);
    await waitFor(() => screen.getByText('BETTING'));

    blackjackApi.createSession.mockResolvedValue(bettingState);
    await user.click(screen.getByRole('button', { name: /New Session/i }));

    expect(blackjackApi.createSession).toHaveBeenCalledTimes(2); // initial + button click
  });

  it('Place Bet button calls placeBet API with correct amount', async () => {
    blackjackApi.placeBet.mockResolvedValue(playerTurnState);
    render(<Blackjack />);
    await waitFor(() => screen.getByText('BETTING'));

    await user.click(screen.getByRole('button', { name: /Place Bet/i }));
    expect(blackjackApi.placeBet).toHaveBeenCalledWith('sess-1', 10);
  });

  it('Hit button calls hit API', async () => {
    blackjackApi.createSession.mockResolvedValue(playerTurnState);
    blackjackApi.hit.mockResolvedValue({ ...playerTurnState, playerValue: 18 });
    render(<Blackjack />);
    await waitFor(() => screen.getByText('PLAYER_TURN'));

    await user.click(screen.getByRole('button', { name: /Hit/i }));
    expect(blackjackApi.hit).toHaveBeenCalledWith('sess-1');
  });

  it('Stand button calls stand API', async () => {
    blackjackApi.createSession.mockResolvedValue(playerTurnState);
    blackjackApi.stand.mockResolvedValue({ ...playerTurnState, phase: 'ROUND_OVER', winner: 'PLAYER' });
    render(<Blackjack />);
    await waitFor(() => screen.getByText('PLAYER_TURN'));

    await user.click(screen.getByRole('button', { name: /Stand/i }));
    expect(blackjackApi.stand).toHaveBeenCalledWith('sess-1');
  });
});
