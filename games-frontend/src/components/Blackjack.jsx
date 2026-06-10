import { useCallback, useEffect, useRef, useState } from 'react';
import { blackjackApi, roomsApi } from '../api/api.js';

// ─── GameHeader (shared) ──────────────────────────────────────────────────────

export function GameHeader({ title, meta }) {
  return (
    <header className="game-header">
      <h2>{title}</h2>
      <span>{meta}</span>
    </header>
  );
}

// ─── HandPanel ────────────────────────────────────────────────────────────────

function HandPanel({ title, cards, value }) {
  return (
    <section className="hand-panel">
      <div className="hand-title">
        <h3>{title}</h3>
        <span>{value}</span>
      </div>
      <div className="cards-row">
        {cards.length === 0 && <div className="playing-card empty">No cards</div>}
        {cards.map((card, index) => (
          <div className="playing-card" key={`${card.rank}-${card.suit}-${index}`}>
            <strong>{card.rank}</strong>
            <span>{card.suit}</span>
          </div>
        ))}
      </div>
    </section>
  );
}

// ─── Blackjack Component ──────────────────────────────────────────────────────

function formatMoney(value) {
  return Number(value ?? 0).toFixed(2);
}

export function Blackjack({ roomId, playerId, playerName, onExit }) {
  const [state, setState] = useState(null);
  const [initialBalance, setInitialBalance] = useState(100);
  const [difficulty, setDifficulty] = useState('BASIC');
  const [bet, setBet] = useState(10);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [opponents, setOpponents] = useState([]);
  const registeredRef = useRef(false);

  const run = useCallback(async (action) => {
    setBusy(true);
    setError('');
    try {
      setState(await action());
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }, []);

  const createSession = useCallback(
    () => run(() => blackjackApi.createSession(Number(initialBalance), difficulty)),
    [difficulty, initialBalance, run]
  );

  useEffect(() => {
    createSession();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!state?.sessionId || !roomId || registeredRef.current) return;
    registeredRef.current = true;
    roomsApi.registerSession(roomId, playerId, state.sessionId).catch(() => {});
  }, [state, roomId, playerId]);

  useEffect(() => {
    if (!roomId) return;
    const interval = setInterval(async () => {
      try {
        const roomState = await roomsApi.getRoomState(roomId);
        const others = (roomState.players ?? []).filter((p) => p.playerId !== playerId);
        setOpponents(others);
      } catch {
        // polling error — ignore
      }
    }, 2000);
    return () => clearInterval(interval);
  }, [roomId, playerId]);

  const sessionId = state?.sessionId;

  return (
    <div className="game-layout">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <GameHeader title="Blackjack" meta={state?.phase ?? 'Loading'} />
        {onExit && (
          <button onClick={onExit} style={{ minHeight: 36, padding: '0 12px' }}>Exit Room</button>
        )}
      </div>
      <div className="toolbar">
        <label>
          Balance
          <input
            id="bj-initial-balance"
            type="number"
            min="1"
            value={initialBalance}
            onChange={(e) => setInitialBalance(e.target.value)}
          />
        </label>
        <label>
          Dealer
          <select
            id="bj-difficulty"
            value={difficulty}
            onChange={(e) => setDifficulty(e.target.value)}
          >
            <option value="BASIC">Basic</option>
            <option value="CONSERVATIVE">Conservative</option>
            <option value="AGGRESSIVE">Aggressive</option>
          </select>
        </label>
        <button id="bj-new-session" onClick={createSession} disabled={busy}>
          New Session
        </button>
        <button
          id="bj-new-round"
          onClick={() => run(() => blackjackApi.startRound(sessionId))}
          disabled={!state || busy}
        >
          New Round
        </button>
      </div>

      {error && <p className="error-line" role="alert">{error}</p>}

      <div className="blackjack-table">
        <HandPanel title="Dealer" cards={state?.dealerCards ?? []} value={state?.dealerValue ?? 'Hidden'} />
        <HandPanel title="Player" cards={state?.playerCards ?? []} value={state?.playerValue ?? 0} />
      </div>

      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
        <div className="status-strip">
          <span>Balance: ${formatMoney(state?.balance)}</span>
          <span>Bet: ${formatMoney(state?.currentBet)}</span>
          <span>Winner: {state?.winner ?? 'NONE'}</span>
          <span>Deck: {state?.cardsRemaining ?? 0}</span>
        </div>

        {opponents.map((opp) => (
          <div key={opp.playerId} className="status-strip" style={{ borderLeft: '3px solid #3466a8', paddingLeft: 10 }}>
            <span style={{ fontWeight: 700, color: '#3466a8' }}>{opp.playerName}</span>
            <span>Bal: ${formatMoney(opp.metrics?.balance)}</span>
            <span>{opp.metrics?.phase ?? ''}</span>
          </div>
        ))}
      </div>

      <div className="toolbar actions">
        <label>
          Bet
          <input
            id="bj-bet-amount"
            type="number"
            min="1"
            value={bet}
            onChange={(e) => setBet(e.target.value)}
          />
        </label>
        <button
          id="bj-place-bet"
          onClick={() => run(() => blackjackApi.placeBet(sessionId, Number(bet)))}
          disabled={!state || state.phase !== 'BETTING' || busy}
        >
          Place Bet
        </button>
        <button
          id="bj-hit"
          onClick={() => run(() => blackjackApi.hit(sessionId))}
          disabled={!state || state.phase !== 'PLAYER_TURN' || busy}
        >
          Hit
        </button>
        <button
          id="bj-stand"
          onClick={() => run(() => blackjackApi.stand(sessionId))}
          disabled={!state || state.phase !== 'PLAYER_TURN' || busy}
        >
          Stand
        </button>
      </div>
    </div>
  );
}

export default Blackjack;
