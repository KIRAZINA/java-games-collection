import { useCallback, useEffect, useRef, useState } from 'react';
import { GameHeader } from './Blackjack.jsx';
import { game2048Api, roomsApi } from '../api/api.js';

const TILE_COLORS = {
  0:    '#cdc1b4',
  2:    '#eee4da',
  4:    '#ede0c8',
  8:    '#f2b179',
  16:   '#f59563',
  32:   '#f67c5f',
  64:   '#f65e3b',
  128:  '#edcf72',
  256:  '#edcc61',
  512:  '#edc850',
  1024: '#edc53f',
  2048: '#edc22e',
};

function formatTime(seconds) {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
}

export function Game2048({ roomId, playerId, playerName, onExit }) {
  const [state, setState] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [opponents, setOpponents] = useState([]);
  const [opponentAlert, setOpponentAlert] = useState('');
  const [roomPhase, setRoomPhase] = useState('LOBBY');
  const [timeRemaining, setTimeRemaining] = useState(0);
  const sessionIdRef = useRef(null);
  const registeredRef = useRef(false);
  const prevOpponentsRef = useRef([]);
  const prevScoreRef = useRef(0);
  const moveRef = useRef(null);

  const createSession = useCallback(async () => {
    setBusy(true);
    setError('');
    try {
      const data = await game2048Api.createSession();
      sessionIdRef.current = data.sessionId;
      registeredRef.current = false;
      setState(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }, []);

  useEffect(() => {
    createSession();
  }, []);

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
        if (roomState.roomPhase === 'PLAYING') {
          setTimeRemaining(roomState.timeRemaining);
        }
      } catch {}
    }, 1000);

    const progressInterval = setInterval(async () => {
      try {
        const progress = await roomsApi.getRoomProgress(roomId);
        setTimeRemaining(progress.timeRemaining);
        if (progress.roomPhase) setRoomPhase(progress.roomPhase);

        const others = (progress.players ?? []).filter((p) => p.playerId !== playerId);
        setOpponents(others);

        const prev = prevOpponentsRef.current;
        for (const opp of others) {
          const prevOpp = prev.find((p) => p.playerId === opp.playerId);
          if (opp.gameOver && (!prevOpp || !prevOpp.gameOver)) {
            setOpponentAlert(`${opp.playerName} is out! (Score: ${opp.score})`);
            setTimeout(() => setOpponentAlert(''), 5000);
          }
          if (!opp.gameOver && prevOpp && opp.score > prevOpp.score && opp.score > 0) {
            setOpponentAlert(`${opp.playerName} scored ${opp.score - (prevOpp.score || 0)} more!`);
            setTimeout(() => setOpponentAlert(''), 3000);
          }
        }
        prevOpponentsRef.current = others;
      } catch {}
    }, 2000);

    return () => {
      clearInterval(stateInterval);
      clearInterval(progressInterval);
    };
  }, [roomId, playerId]);

  const move = useCallback(
    async (direction) => {
      if (!state || busy || state.gameOver) return;
      setBusy(true);
      setError('');
      try {
        const newState = await game2048Api.move(state.sessionId, direction);
        setState(newState);
      } catch (err) {
        setError(err.message);
      } finally {
        setBusy(false);
      }
    },
    [busy, state]
  );

  moveRef.current = move;

  useEffect(() => {
    function handleKeyDown(e) {
      const direction = {
        ArrowUp: 'UP',
        ArrowDown: 'DOWN',
        ArrowLeft: 'LEFT',
        ArrowRight: 'RIGHT',
      }[e.key];
      if (direction) {
        e.preventDefault();
        moveRef.current(direction);
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const values = Array.from({ length: 16 }, () => 0);
  let lastIceBlockCount = 0;
  const iceBlockPositions = new Set();
  for (const tile of state?.tiles ?? []) {
    if (tile.value === -1) {
      iceBlockPositions.add(tile.row * 4 + tile.col);
      lastIceBlockCount++;
    }
    values[tile.row * 4 + tile.col] = tile.value;
  }

  const isUrgent = roomPhase === 'PLAYING' && timeRemaining <= 10 && timeRemaining > 0;

  if (roomId && (roomPhase === 'LOBBY' || roomPhase === 'READY_CHECK')) {
    return (
      <div className="game-layout compact-game">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <GameHeader title="2048" meta="Waiting..." />
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
    <div className="game-layout compact-game">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <GameHeader title="2048" meta={!state ? 'Loading' : (state.gameOver ? 'No moves left' : 'In progress')} />
        {onExit && (
          <button onClick={onExit} style={{ minHeight: 36, padding: '0 12px' }}>Exit Room</button>
        )}
      </div>

      {roomPhase === 'PLAYING' && (
        <div className={`timer-display ${isUrgent ? 'timer-urgent' : ''}`}>
          {formatTime(timeRemaining)}
        </div>
      )}

      {roomPhase === 'GAME_OVER' && (
        <div className="timer-display timer-expired">
          Time's Up!
        </div>
      )}

      {opponentAlert && (
        <p className="error-line" role="alert" style={{ borderLeftColor: '#3466a8', background: '#e8f0fd', color: '#1d3a6f' }}>
          {opponentAlert}
        </p>
      )}

      <div className="toolbar">
        <button id="g2048-new-board" onClick={createSession} disabled={busy}>
          New Board
        </button>
        <button
          id="g2048-reset"
          onClick={async () => {
            if (!state) return;
            try {
              setState(await game2048Api.reset(state.sessionId));
            } catch (err) {
              setError(err.message);
            }
          }}
          disabled={!state || busy}
        >
          Reset
        </button>
      </div>

      {error && <p className="error-line" role="alert">{error}</p>}

      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
        <div className="status-strip">
          <span>Score: {state?.score ?? 0}</span>
          <span>Moves: {state?.movesMade ?? 0}</span>
          <span>Ice: {state?.iceBlockCount ?? 0}</span>
        </div>

        {opponents.map((opp) => {
          const isGameOver = opp.gameOver ?? false;
          const oppScore = opp.score ?? 0;
          const isOvertake = oppScore > (prevOpponentsRef.current.find(p => p.playerId === opp.playerId)?.score ?? 0);

          return (
            <div key={opp.playerId}
              className={`status-strip opponent-strip ${isOvertake ? 'score-overtake' : ''}`}
              style={{ borderLeft: '3px solid #3466a8', paddingLeft: 10 }}>
              <span style={{ fontWeight: 700, color: '#3466a8' }}>{opp.playerName}</span>
              <span>Score: {oppScore}</span>
              <span>Moves: {opp.movesMade ?? 0}</span>
              <span>{isGameOver ? 'Done' : 'Playing'}</span>
            </div>
          );
        })}
      </div>

      <div className="board-2048" aria-label="2048 board">
        {values.map((value, index) => {
          const isIceBlock = value === -1;
          const row = Math.floor(index / 4);
          const col = index % 4;
          return (
            <div
              key={`${row}-${col}`}
              id={`g2048-tile-${index}`}
              className={`tile-2048 ${isIceBlock ? 'tile-ice-block' : `value-${value}`}`}
              style={isIceBlock ? {} : { backgroundColor: TILE_COLORS[value] ?? '#3c3a32' }}
              aria-label={isIceBlock ? 'Ice Block' : value ? `Tile ${value}` : 'Empty'}
            >
              {isIceBlock ? '🧊' : value || ''}
            </div>
          );
        })}
      </div>

      <div className="move-pad" aria-label="Move controls">
        <button id="g2048-up"    onClick={() => move('UP')}    disabled={busy}>↑</button>
        <button id="g2048-left"  onClick={() => move('LEFT')}  disabled={busy}>←</button>
        <button id="g2048-right" onClick={() => move('RIGHT')} disabled={busy}>→</button>
        <button id="g2048-down"  onClick={() => move('DOWN')}  disabled={busy}>↓</button>
      </div>
    </div>
  );
}

export default Game2048;
