import { useCallback, useEffect, useRef, useState } from 'react';
import { blackjackApi, roomsApi } from '../api/api.js';

export function GameHeader({ title, meta }) {
  return (
    <header className="game-header">
      <h2>{title}</h2>
      <span>{meta}</span>
    </header>
  );
}

function HandPanel({ title, cards, value, isDealer, phase }) {
  const roundOver = phase === 'ROUND_OVER';

  let displayCards = cards;
  if (isDealer && !roundOver && cards.length > 1) {
    displayCards = [cards[0], ...cards.slice(1).map(() => null)];
  }

  const displayValue = isDealer && !roundOver ? '\u00A0?\u00A0' : value;
  const playerTurn = phase === 'PLAYER_TURN' || phase === 'DEALER_TURN';

  return (
    <section className="hand-panel">
      <div className="hand-title">
        <h3>{title}</h3>
        <span>{displayValue}</span>
      </div>
      <div className="cards-row">
        {displayCards.length === 0 && <div className="playing-card empty">No cards</div>}
        {displayCards.map((card, index) =>
          card === null ? (
            <div className={`playing-card card-back ${isDealer && playerTurn ? 'card-hidden-pulse' : ''}`} key={`hidden-${index}`}>
              <div className="card-back-pattern">◆</div>
            </div>
          ) : (
            <div className="playing-card card-deal-in" key={`${card.rank}-${card.suit}-${index}`}>
              <strong>{card.rank}</strong>
              <span>{card.suit}</span>
            </div>
          )
        )}
      </div>
    </section>
  );
}

function formatMoney(value) {
  return Number(value ?? 0).toFixed(2);
}

export function Blackjack({ roomId, playerId, playerName, onExit }) {
  const [state, setState] = useState(null);
  const [difficulty, setDifficulty] = useState('BASIC');
  const [bet, setBet] = useState(10);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [opponents, setOpponents] = useState([]);
  const [roomPhase, setRoomPhase] = useState('LOBBY');
  const [countdown, setCountdown] = useState(null);
  const [balanceFlash, setBalanceFlash] = useState('');
  const registeredRef = useRef(false);
  const intervalRef = useRef(null);
  const prevBalanceRef = useRef(null);

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
    () => run(() => blackjackApi.createSession(100, difficulty)),
    [difficulty, run]
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

    const stateInterval = setInterval(async () => {
      try {
        const roomState = await roomsApi.getRoomState(roomId);
        setRoomPhase(roomState.roomPhase);
      } catch {}
    }, 1000);

    const progressInterval = setInterval(async () => {
      try {
        const progress = await roomsApi.getRoomProgress(roomId);
        if (progress.roomPhase) setRoomPhase(progress.roomPhase);
        const others = (progress.players ?? []).filter((p) => p.playerId !== playerId);
        setOpponents(others);
      } catch {
        // polling error — ignore
      }
    }, 2000);

    return () => {
      clearInterval(stateInterval);
      clearInterval(progressInterval);
    };
  }, [roomId, playerId]);

  // ─── Balance Flash Animation ──────────────────────────────────────────

  useEffect(() => {
    if (state && prevBalanceRef.current !== null && prevBalanceRef.current !== state.balance) {
      if (state.balance > prevBalanceRef.current) {
        setBalanceFlash('balance-flash-green');
      } else {
        setBalanceFlash('balance-flash-red');
      }
      const timer = setTimeout(() => setBalanceFlash(''), 500);
      prevBalanceRef.current = state.balance;
      return () => clearTimeout(timer);
    } else if (state) {
      prevBalanceRef.current = state.balance;
    }
  }, [state?.balance]);

  // ─── Live Ticking Countdown ───────────────────────────────────────────

  const handleStartRoundRef = useRef(null);
  handleStartRoundRef.current = () => {
    if (state?.sessionId) {
      run(() => blackjackApi.startRound(state.sessionId));
    }
  };

  const roundOver = state?.phase === 'ROUND_OVER';

  useEffect(() => {
    if (roundOver && intervalRef.current === null) {
      setCountdown(5);
      intervalRef.current = setInterval(() => {
        setCountdown(prev => prev - 1);
      }, 1000);
    } else if (!roundOver && intervalRef.current !== null) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
      setCountdown(null);
    }

    return () => {
      if (intervalRef.current !== null) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [roundOver]);

  const prevCountdownRef = useRef(countdown);
  useEffect(() => {
    if (prevCountdownRef.current === 1 && countdown === 0) {
      handleStartRoundRef.current?.();
    }
    prevCountdownRef.current = countdown;
  }, [countdown]);

  const sessionId = state?.sessionId;
  const isBankrupt = state?.balance === 0 && state?.phase === 'ROUND_OVER';

  // ─── Manual New Round (clears auto timer) ─────────────────────────────

  const handleNewRound = useCallback(() => {
    if (intervalRef.current !== null) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
      setCountdown(null);
    }
    run(() => blackjackApi.startRound(sessionId));
  }, [sessionId, run]);

  if (roomId && (roomPhase === 'LOBBY' || roomPhase === 'READY_CHECK')) {
    return (
      <div className="game-layout">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <GameHeader title="Blackjack" meta="Waiting..." />
        </div>
        <div className="ready-check-overlay">
          <div className="ready-check-card">
            <h2>Waiting for players</h2>
            <p>Other players are joining the room. The game will start shortly.</p>
            <div className="ready-spinner"></div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="game-layout">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <GameHeader title="Blackjack" meta={state?.phase ?? 'Loading'} />
        {onExit && (
          <button onClick={onExit} style={{ minHeight: 36, padding: '0 12px' }}>Exit Room</button>
        )}
      </div>

      {/* ─── Notification Toast ───────────────────────────────────────── */}
      {state?.notifications?.length > 0 && (
        <div style={{
          background: '#e8f0fd', border: '1px solid #3466a8', borderRadius: 6,
          padding: '10px 14px', marginBottom: 8, color: '#1d3a6f', fontWeight: 600,
        }} role="alert">
          {state.notifications.map((msg, i) => (
            <p key={i} style={{ margin: '2px 0' }}>{msg}</p>
          ))}
        </div>
      )}

      {/* ─── Bankruptcy Banner ────────────────────────────────────────── */}
      {isBankrupt && (
        <div style={{
          background: '#fff3e0', border: '1px solid #e65100', borderRadius: 6,
          padding: '12px 14px', marginBottom: 8, color: '#bf360c', fontWeight: 600,
        }} role="alert">
          You are out of funds! The next hand will be skipped, and your balance will be refilled automatically.
        </div>
      )}

      <div className="toolbar">
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
          onClick={handleNewRound}
          disabled={!state || busy}
        >
          New Round
        </button>
      </div>

      {error && <p className="error-line" role="alert">{error}</p>}

      <div className="blackjack-table">
        <HandPanel title="Dealer" cards={state?.dealerCards ?? []} value={state?.dealerValue ?? '\u00A0?\u00A0'} isDealer={true} phase={state?.phase} />
        <HandPanel title="Player" cards={state?.playerCards ?? []} value={state?.playerValue ?? 0} />
      </div>

      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', alignItems: 'center' }}>
        <div className={`balance-display ${balanceFlash}`}>
          <span className="balance-label">Balance</span>
          <span className="balance-amount">${formatMoney(state?.balance)}</span>
        </div>

        <div className="status-strip">
          <span>Bet: ${formatMoney(state?.currentBet)}</span>
          <span>Winner: {state?.winner ?? 'NONE'}</span>
          <span>Deck: {state?.cardsRemaining ?? 0}</span>
        </div>

        {opponents.map((opp) => (
          <div key={opp.playerId} className="status-strip" style={{ borderLeft: '3px solid #3466a8', paddingLeft: 10 }}>
            <span style={{ fontWeight: 700, color: '#3466a8' }}>{opp.playerName}</span>
            <span>Bal: ${formatMoney(opp.balance ?? 0)}</span>
            <span>{opp.phase ?? ''}</span>
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

      {countdown !== null && countdown > 0 && (
        <p style={{ fontSize: '0.85rem', color: '#607088', marginTop: 6 }}>
          Next round starts in: {countdown}...
        </p>
      )}
    </div>
  );
}

export default Blackjack;
