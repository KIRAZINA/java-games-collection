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

export function Game2048({ roomId, playerId, playerName, onExit }) {
  const [state, setState] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [opponents, setOpponents] = useState([]);
  const sessionIdRef = useRef(null);
  const registeredRef = useRef(false);

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

  const move = useCallback(
    async (direction) => {
      if (!state || busy || state.gameOver) return;
      setBusy(true);
      setError('');
      try {
        setState(await game2048Api.move(state.sessionId, direction));
      } catch (err) {
        setError(err.message);
      } finally {
        setBusy(false);
      }
    },
    [busy, state]
  );

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
        move(direction);
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [move]);

  const values = Array.from({ length: 16 }, () => 0);
  for (const tile of state?.tiles ?? []) {
    values[tile.row * 4 + tile.col] = tile.value;
  }

  return (
    <div className="game-layout compact-game">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <GameHeader title="2048" meta={!state ? 'Loading' : (state.gameOver ? 'No moves left' : 'In progress')} />
        {onExit && (
          <button onClick={onExit} style={{ minHeight: 36, padding: '0 12px' }}>Exit Room</button>
        )}
      </div>

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
        </div>

        {opponents.map((opp) => {
          const isGameOver = opp.gameOver ?? opp.metrics?.gameOver ?? false;
          return (
            <div key={opp.playerId} className="status-strip" style={{ borderLeft: '3px solid #3466a8', paddingLeft: 10 }}>
              <span style={{ fontWeight: 700, color: '#3466a8' }}>{opp.playerName}</span>
              <span>Score: {opp.score ?? opp.metrics?.score ?? 0}</span>
              <span>Moves: {opp.movesMade ?? opp.metrics?.movesMade ?? 0}</span>
              <span>{isGameOver ? 'Done' : 'Playing'}</span>
            </div>
          );
        })}
      </div>

      <div className="board-2048" aria-label="2048 board">
        {values.map((value, index) => (
          <div
            key={index}
            id={`g2048-tile-${index}`}
            className={`tile-2048 value-${value}`}
            style={{ backgroundColor: TILE_COLORS[value] ?? '#3c3a32' }}
            aria-label={value ? `Tile ${value}` : 'Empty'}
          >
            {value || ''}
          </div>
        ))}
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
