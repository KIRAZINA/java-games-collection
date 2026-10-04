import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { GameHeader } from './Blackjack.jsx';
import { minesweeperApi, roomsApi } from '../api/api.js';
import { roomWinnerLabel } from './roomWinner.js';

function renderMineCell(cell) {
  if (!cell) return '';
  if (cell.state === 'FLAGGED') return '🚩';
  if (cell.state === 'WRONG_FLAG') return '❌';
  if (cell.mine) return '💣';
  if (cell.state === 'OPENED' && cell.adjacentMines > 0) return cell.adjacentMines;
  return '';
}

function formatTime(seconds) {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
}

const MAX_POLL_FAILURES = 3;

export function Minesweeper({ roomId, playerId, playerName, onExit }) {
  const [state, setState] = useState(null);
  const [settings, setSettings] = useState({ rows: 9, cols: 9, mines: 10 });
  const [error, setError] = useState('');
  const [pollError, setPollError] = useState('');
  const [busy, setBusy] = useState(false);
  const [opponents, setOpponents] = useState([]);
  const [opponentAlert, setOpponentAlert] = useState('');
  const [roomPhase, setRoomPhase] = useState('LOBBY');
  const [timeRemaining, setTimeRemaining] = useState(0);
  const [winnerLabel, setWinnerLabel] = useState(null);
  const sessionIdRef = useRef(null);
  const prevOpponentsRef = useRef([]);
  const registeredRef = useRef(false);
  const prevScoreRef = useRef(0);
  const pollFailuresRef = useRef(0);

  const createSession = useCallback(async () => {
    setBusy(true);
    setError('');
    try {
      const data = await minesweeperApi.createSession(
        Number(settings.rows),
        Number(settings.cols),
        Number(settings.mines)
      );
      sessionIdRef.current = data.sessionId;
      registeredRef.current = false;
      setState(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }, [settings]);

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

    let cancelled = false;
    let stopped = false;
    let stateInterval;
    let progressInterval;
    pollFailuresRef.current = 0;
    setPollError('');

    const stopPolling = () => {
      stopped = true;
      clearInterval(stateInterval);
      clearInterval(progressInterval);
    };

    const onPollFailure = (err) => {
      if (cancelled || stopped) return;
      if (err?.status === 404) {
        setPollError('Room no longer exists. It may have been closed or deleted.');
        stopPolling();
        return;
      }
      pollFailuresRef.current += 1;
      if (pollFailuresRef.current >= MAX_POLL_FAILURES) {
        setPollError('Lost connection to the room. Please exit and try again.');
        stopPolling();
      }
    };

    const pollState = async () => {
      try {
        const roomState = await roomsApi.getRoomState(roomId);
        if (cancelled || stopped) return;
        pollFailuresRef.current = 0;
        setRoomPhase(roomState.roomPhase);
        setWinnerLabel(roomWinnerLabel(roomState));
        if (roomState.roomPhase === 'PLAYING') {
          setTimeRemaining(roomState.timeRemaining);
        }
      } catch (err) {
        onPollFailure(err);
      }
    };

    // Step 6e B1: fire the first /state on mount so the room phase (and the
    // countdown for timed games) populate on the first frame instead of
    // waiting up to 1000ms.
    pollState();
    stateInterval = setInterval(pollState, 1000);

    progressInterval = setInterval(async () => {
      try {
        const progress = await roomsApi.getRoomProgress(roomId);
        if (cancelled || stopped) return;
        pollFailuresRef.current = 0;
        setTimeRemaining(progress.timeRemaining);
        if (progress.roomPhase) setRoomPhase(progress.roomPhase);

        const others = (progress.players ?? []).filter((p) => p.playerId !== playerId);
        setOpponents(others);

        const prev = prevOpponentsRef.current;
        for (const opp of others) {
          const prevOpp = prev.find((p) => p.playerId === opp.playerId);
          if (opp.gameOver && (!prevOpp || !prevOpp.gameOver)) {
            if (opp.won) {
              setOpponentAlert(`${opp.playerName} won! (boards: ${opp.boardsCleared})`);
            } else {
              setOpponentAlert(`${opp.playerName} lost!`);
            }
            setTimeout(() => setOpponentAlert(''), 5000);
          }
          if (!opp.gameOver && prevOpp && opp.score > prevOpp.score && opp.score > 0) {
            setOpponentAlert(`${opp.playerName} scored ${opp.score - (prevOpp.score || 0)} more!`);
            setTimeout(() => setOpponentAlert(''), 3000);
          }
        }
        prevOpponentsRef.current = others;
      } catch (err) {
        onPollFailure(err);
      }
    }, 2000);

    return () => {
      cancelled = true;
      clearInterval(stateInterval);
      clearInterval(progressInterval);
    };
  }, [roomId, playerId]);

  async function handleNextBoard() {
    if (!state?.sessionId || busy) return;
    setBusy(true);
    setError('');
    try {
      setState(await minesweeperApi.nextBoard(state.sessionId));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const cellMap = useMemo(() => {
    const map = new Map();
    for (const cell of state?.cells ?? []) {
      map.set(`${cell.row}:${cell.col}`, cell);
    }
    return map;
  }, [state]);

  async function cellAction(action, row, col) {
    if (!state || busy || state.gameOver || state.isLocked) return;
    setBusy(true);
    setError('');
    try {
      if (action === 'open') {
        setState(await minesweeperApi.open(state.sessionId, row, col));
      } else {
        setState(await minesweeperApi.toggleFlag(state.sessionId, row, col));
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const isUrgent = roomPhase === 'PLAYING' && timeRemaining <= 10 && timeRemaining > 0;

  if (roomId && pollError) {
    return (
      <div className="game-layout">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <GameHeader title="Minesweeper" meta="Disconnected" />
          {onExit && (
            <button onClick={onExit} style={{ minHeight: 36, padding: '0 12px' }}>Exit Room</button>
          )}
        </div>
        <div className="ready-check-overlay">
          <div className="ready-check-card">
            <p className="error-line" role="alert">{pollError}</p>
          </div>
        </div>
      </div>
    );
  }

  if (roomId && (roomPhase === 'LOBBY' || roomPhase === 'READY_CHECK')) {
    return (
      <div className="game-layout">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <GameHeader title="Minesweeper" meta="Waiting..." />
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
        <GameHeader
          title="Minesweeper"
          meta={state?.gameOver ? (state.won ? 'Won' : 'Lost') : 'In progress'}
        />
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

      {roomPhase === 'GAME_OVER' && winnerLabel && (
        <p className="winner-line" role="status">Winner: {winnerLabel}</p>
      )}

      {opponentAlert && (
        <p className="error-line" role="alert" style={{ borderLeftColor: 'var(--border-strong)', background: 'var(--surface-3)', color: 'var(--text)' }}>
          {opponentAlert}
        </p>
      )}

      <div className="toolbar">
        <label>
          Rows
          <input id="ms-rows" type="number" min="4" max="30" value={settings.rows}
            onChange={(e) => setSettings({ ...settings, rows: e.target.value })} />
        </label>
        <label>
          Columns
          <input id="ms-cols" type="number" min="4" max="30" value={settings.cols}
            onChange={(e) => setSettings({ ...settings, cols: e.target.value })} />
        </label>
        <label>
          Mines
          <input id="ms-mines" type="number" min="1" value={settings.mines}
            onChange={(e) => setSettings({ ...settings, mines: e.target.value })} />
        </label>
        <button id="ms-new-board" onClick={createSession} disabled={busy}>
          New Board
        </button>
        {state?.won && (
          <button id="ms-next-board" onClick={handleNextBoard} disabled={busy}
            style={{ background: 'var(--success)', color: 'var(--bg)', borderColor: 'var(--success)' }}>
            Next Board ({state.boardsCleared + 1})
          </button>
        )}
      </div>

      {error && <p className="error-line" role="alert">{error}</p>}
      {pollError && <p className="error-line" role="alert">{pollError}</p>}

      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
        <div className="status-strip">
          <span>Flags: {state?.flagsPlaced ?? 0}</span>
          <span>Remaining: {state?.remainingMines ?? 0}</span>
          <span>Score: {state?.score ?? 0}</span>
          <span>Boards: {state?.boardsCleared ?? 0}</span>
        </div>

        {opponents.map((opp) => {
          const isGameOver = opp.gameOver ?? false;
          const isWon = opp.won ?? false;
          const isLocked = opp.isLocked ?? false;
          const oppScore = opp.score ?? opp.clearedFields ?? 0;
          const isOvertake = oppScore > (prevScoreRef.current > 0 && opp.playerId ? prevOpponentsRef.current.find(p => p.playerId === opp.playerId)?.score ?? 0 : 0);

          return (
            <div key={opp.playerId} className={`status-strip opponent-strip ${isOvertake ? 'score-overtake' : ''}`}
              style={{ borderLeft: '3px solid var(--border-strong)', paddingLeft: 10 }}>
              <span style={{ fontWeight: 700, color: 'var(--text)' }}>{opp.playerName}</span>
              <span>Opened: {opp.clearedFields ?? 0}</span>
              <span>Score: {opp.score ?? 0}</span>
              <span>Boards: {opp.boardsCleared ?? 0}</span>
              <span>{isLocked ? '💥 Locked' : isGameOver ? (isWon ? 'Won' : 'Lost') : 'Playing'}</span>
            </div>
          );
        })}
      </div>

      {state?.isLocked && (
        <div className="locked-overlay">
          <div className="locked-banner">
            <span className="boom-icon">💥</span>
            <h3>BOOM! Board Locked</h3>
            <p>Waiting for timer to expire...</p>
          </div>
        </div>
      )}

      <div
        className="mines-grid"
        style={{ '--rows': state?.rows ?? 9, '--cols': state?.cols ?? 9 }}
      >
        {Array.from({ length: (state?.rows ?? 0) * (state?.cols ?? 0) }, (_, idx) => {
          const row = Math.floor(idx / (state?.cols ?? 1));
          const col = idx % (state?.cols ?? 1);
          const cell = cellMap.get(`${row}:${col}`);
          const justRevealed = cell?.state === 'OPENED' && !cell?.mine;
          return (
            <button
              key={`${row}-${col}`}
              id={`ms-cell-${row}-${col}`}
              className={`mine-cell ${cell?.state?.toLowerCase() ?? ''} ${justRevealed ? 'tile-reveal' : ''}`}
              onClick={() => cellAction('open', row, col)}
              onContextMenu={(e) => {
                e.preventDefault();
                cellAction('flag', row, col);
              }}
              disabled={busy || state?.gameOver || state?.isLocked}
              aria-label={`Row ${row + 1}, column ${col + 1}`}
            >
              {renderMineCell(cell)}
            </button>
          );
        })}
      </div>
    </div>
  );
}

export default Minesweeper;
