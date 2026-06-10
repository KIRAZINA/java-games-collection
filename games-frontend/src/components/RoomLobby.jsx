import { useCallback, useEffect, useRef, useState } from 'react';
import { roomsApi } from '../api/api.js';
import { GameHeader } from './Blackjack.jsx';

const GAME_LABELS = {
  blackjack: 'Blackjack',
  minesweeper: 'Minesweeper',
  '2048': '2048',
};

const GAME_TYPE_MAP = {
  blackjack: 'BLACKJACK',
  minesweeper: 'MINESWEEPER',
  '2048': 'TWENTY_FORTY_EIGHT',
};

const DEFAULT_SETTINGS = {
  blackjack: { settings: { initialBalance: 100, difficulty: 'BASIC' }, maxPlayers: 4 },
  minesweeper: { settings: { rows: 9, cols: 9, mines: 10 }, maxPlayers: 2 },
  '2048': { settings: {}, maxPlayers: 2 },
};

function formatTime(seconds) {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return m > 0 ? `${m}m ${s}s` : `${s}s`;
}

function CreateRoomForm({ gameKey, playerId, playerName, onSubmit, onCancel }) {
  const [roomName, setRoomName] = useState(`${playerName}'s Room`);
  const [password, setPassword] = useState('');
  const [settings, setSettings] = useState(DEFAULT_SETTINGS[gameKey]?.settings ?? {});
  const [maxPlayers, setMaxPlayers] = useState(DEFAULT_SETTINGS[gameKey]?.maxPlayers ?? 4);
  const [timeLimitSeconds, setTimeLimitSeconds] = useState(60);
  const [isSinglePlayer, setIsSinglePlayer] = useState(false);

  const effectiveMaxPlayers = isSinglePlayer ? 1 : maxPlayers;

  function handleSubmit(e) {
    e.preventDefault();
    const gameType = GAME_TYPE_MAP[gameKey];
    const gameSettings = {
      gameType,
      settings,
      passwordProtected: password.length > 0,
      passwordHash: password,
      allowBots: false,
      maxPlayers: effectiveMaxPlayers,
      timeLimitSeconds: gameKey === 'blackjack' ? 0 : timeLimitSeconds,
      isSinglePlayer,
    };
    onSubmit(roomName, gameType, gameSettings, password);
  }

  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.35)',
      display: 'grid', placeItems: 'center', zIndex: 100,
    }}>
      <form onSubmit={handleSubmit} style={{
        background: '#fff', borderRadius: 10, padding: 24,
        minWidth: 340, maxWidth: 420, display: 'grid', gap: 14,
        border: '1px solid #dce3ec',
      }}>
        <h3 style={{ margin: 0 }}>Create {GAME_LABELS[gameKey]} Room</h3>

        <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: '#526174' }}>
          Room Name
          <input value={roomName} onChange={(e) => setRoomName(e.target.value)} required />
        </label>

        {gameKey === 'blackjack' && (
          <>
            <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: '#526174' }}>
              Dealer Difficulty
              <select value={settings.difficulty} onChange={(e) => setSettings({ ...settings, difficulty: e.target.value })}>
                <option value="BASIC">Basic</option>
                <option value="CONSERVATIVE">Conservative</option>
                <option value="AGGRESSIVE">Aggressive</option>
              </select>
            </label>
            <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: '#526174' }}>
              Starting Balance
              <input type="number" min="1" value={settings.initialBalance}
                onChange={(e) => setSettings({ ...settings, initialBalance: Number(e.target.value) })} />
            </label>
          </>
        )}

        {gameKey === 'minesweeper' && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8 }}>
            <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: '#526174' }}>
              Rows
              <input type="number" min="4" max="30" value={settings.rows}
                onChange={(e) => setSettings({ ...settings, rows: Number(e.target.value) })} />
            </label>
            <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: '#526174' }}>
              Cols
              <input type="number" min="4" max="30" value={settings.cols}
                onChange={(e) => setSettings({ ...settings, cols: Number(e.target.value) })} />
            </label>
            <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: '#526174' }}>
              Mines
              <input type="number" min="1" value={settings.mines}
                onChange={(e) => setSettings({ ...settings, mines: Number(e.target.value) })} />
            </label>
          </div>
        )}

        {gameKey !== 'blackjack' && (
          <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: '#526174' }}>
            Time Limit
            <select value={timeLimitSeconds} onChange={(e) => setTimeLimitSeconds(Number(e.target.value))}>
              <option value={30}>30 seconds</option>
              <option value={60}>1 minute</option>
              <option value={180}>3 minutes</option>
            </select>
          </label>
        )}

        {gameKey !== 'blackjack' && (
          <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontWeight: 700, fontSize: '0.82rem', color: '#526174', cursor: 'pointer' }}>
            <input type="checkbox" checked={isSinglePlayer}
              onChange={(e) => setIsSinglePlayer(e.target.checked)} style={{ width: 'auto' }} />
            Single Player (practice mode)
          </label>
        )}

        <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: '#526174' }}>
          Max Players
          <input type="number" min="1" max="8" value={effectiveMaxPlayers}
            onChange={(e) => setMaxPlayers(Number(e.target.value))}
            disabled={isSinglePlayer} />
        </label>

        <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: '#526174' }}>
          Password (optional)
          <input type="password" value={password} placeholder="Leave blank for public room"
            onChange={(e) => setPassword(e.target.value)} />
        </label>

        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', paddingTop: 6 }}>
          <button type="button" onClick={onCancel}>Cancel</button>
          <button type="submit" style={{ background: '#243e68', color: '#fff', borderColor: '#243e68' }}>
            Create Room
          </button>
        </div>
      </form>
    </div>
  );
}

function ReadyCheckOverlay({ roomId, playerId, roomPhase, timeRemaining, onReadySent }) {
  const [readySent, setReadySent] = useState(false);

  const countdown = roomPhase === 'READY_CHECK' ? Math.max(0, Math.min(3, timeRemaining)) : null;

  async function handleMarkReady() {
    try {
      await roomsApi.markReady(roomId, playerId);
      setReadySent(true);
      onReadySent?.();
    } catch {}
  }

  return (
    <div className="ready-check-overlay">
      <div className="ready-check-card">
        {roomPhase === 'LOBBY' && !readySent && (
          <>
            <h2>Get Ready</h2>
            <p>Press the button when you're ready to start</p>
            <button className="ready-button" onClick={handleMarkReady}>
              I'm Ready!
            </button>
          </>
        )}
        {roomPhase === 'LOBBY' && readySent && (
          <>
            <h2>Waiting for players...</h2>
            <p>Waiting for all players to ready up</p>
            <div className="ready-spinner"></div>
          </>
        )}
        {roomPhase === 'READY_CHECK' && countdown !== null && (
          <>
            <h2>Get Ready!</h2>
            <div className="countdown-display" key={countdown}>
              {countdown > 0 ? countdown : 'GO!'}
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export function RoomLobby({ gameKey, playerId, playerName, onEnterGame, onQuickPlay }) {
  const [rooms, setRooms] = useState([]);
  const [error, setError] = useState('');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [busy, setBusy] = useState(false);

  const [activeRoomId, setActiveRoomId] = useState(null);
  const [roomPhase, setRoomPhase] = useState(null);
  const [timeRemaining, setTimeRemaining] = useState(0);
  const [readySent, setReadySent] = useState(false);
  const pollRef = useRef(null);

  const fetchRooms = useCallback(async () => {
    try {
      const gameType = GAME_TYPE_MAP[gameKey];
      const data = await roomsApi.listRooms(gameType);
      setRooms(data);
      setError('');
    } catch (err) {
      setError(err.message);
    }
  }, [gameKey]);

  useEffect(() => {
    fetchRooms();
    const interval = setInterval(fetchRooms, 5000);
    return () => clearInterval(interval);
  }, [fetchRooms]);

  useEffect(() => {
    if (!activeRoomId) {
      if (pollRef.current) {
        clearInterval(pollRef.current);
        pollRef.current = null;
      }
      return;
    }

    pollRef.current = setInterval(async () => {
      try {
        const st = await roomsApi.getRoomState(activeRoomId);
        setRoomPhase(st.roomPhase);

        if (st.roomPhase === 'READY_CHECK') {
          setTimeRemaining(st.timeRemaining);
        }

        if (st.roomPhase === 'PLAYING' || st.roomPhase === 'GAME_OVER') {
          clearInterval(pollRef.current);
          pollRef.current = null;
          onEnterGame(activeRoomId, gameKey);
        }
      } catch {
        clearInterval(pollRef.current);
        pollRef.current = null;
        setActiveRoomId(null);
      }
    }, 500);

    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
  }, [activeRoomId, gameKey, onEnterGame]);

  async function handleCreateRoom(roomName, gameType, gameSettings, password) {
    setBusy(true);
    setError('');
    try {
      const summary = await roomsApi.createRoom(roomName, gameType, gameSettings, playerId, playerName);
      if (summary.isSinglePlayer && gameKey !== 'blackjack') {
        setActiveRoomId(summary.roomId);
      } else {
        onEnterGame(summary.roomId, gameKey);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
      setShowCreateForm(false);
    }
  }

  async function handleJoinRoom(room) {
    console.log('[RoomLobby] handleJoinRoom roomId:', room.roomId, 'playerId:', playerId, 'isSinglePlayer:', room.isSinglePlayer);
    if (busy) return;
    let password = '';
    if (room.passwordProtected) {
      password = prompt('This room is password protected. Enter password:', '') || '';
    }
    setBusy(true);
    setError('');
    try {
      await roomsApi.joinRoom(room.roomId, playerId, playerName, password || undefined);
      console.log('[RoomLobby] joinRoom succeeded for roomId:', room.roomId);
      if (room.isSinglePlayer && gameKey !== 'blackjack') {
        setActiveRoomId(room.roomId);
      } else {
        onEnterGame(room.roomId, gameKey);
      }
    } catch (err) {
      console.error('[RoomLobby] joinRoom failed for roomId:', room.roomId, 'error:', err.message);
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  if (activeRoomId) {
    return (
      <div className="game-layout">
        <GameHeader title={`${GAME_LABELS[gameKey]} Lobby`} meta="Starting..." />
        <ReadyCheckOverlay
          roomId={activeRoomId}
          playerId={playerId}
          roomPhase={roomPhase}
          timeRemaining={timeRemaining}
          onReadySent={() => setReadySent(true)}
        />
      </div>
    );
  }

  return (
    <div className="game-layout">
      <GameHeader title={`${GAME_LABELS[gameKey]} Lobby`} meta={`${rooms.length} room(s)`} />

      <div className="toolbar">
        <button onClick={fetchRooms} disabled={busy}>Refresh</button>
        <button onClick={() => onQuickPlay(gameKey)} disabled={busy}
          style={{ background: '#1b5e20', color: '#fff', borderColor: '#1b5e20' }}>
          ⚡ Quick Play (Solo)
        </button>
        <button onClick={() => setShowCreateForm(true)} disabled={busy}
          style={{ background: '#243e68', color: '#fff', borderColor: '#243e68' }}>
          + Create Room
        </button>
      </div>

      {error && <p className="error-line" role="alert">{error}</p>}

      {rooms.length === 0 && !error && (
        <p style={{ color: '#607088', padding: '1rem 0' }}>
          No active rooms. Create one to get started!
        </p>
      )}

      <div style={{ display: 'grid', gap: 10 }}>
        {rooms.map((room) => (
          <div key={room.roomId} style={{
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            border: '1px solid #d6deea', borderRadius: 8, background: '#fff',
            padding: '12px 16px',
          }}>
            <div>
              <strong>{room.roomName}</strong>
              <div style={{ fontSize: '0.82rem', color: '#607088', marginTop: 4 }}>
                {room.playerCount}/{room.maxPlayers} players
                {room.passwordProtected ? ' 🔒' : ''}
                {room.phase !== 'LOBBY' ? ` — ${room.phase}` : ''}
                {room.timeLimitSeconds > 0 ? ` — ${formatTime(room.timeLimitSeconds)}` : ''}
                {gameKey !== 'blackjack' && room.isSinglePlayer ? ' — Solo' : ''}
              </div>
            </div>
            <button
              onClick={() => handleJoinRoom(room)}
              disabled={busy || (room.phase !== 'LOBBY' && room.phase !== 'READY_CHECK') || room.playerCount >= room.maxPlayers}
            >
              {room.phase === 'LOBBY' ? 'Join' : room.phase === 'READY_CHECK' ? 'Playing' : room.phase}
            </button>
          </div>
        ))}
      </div>

      {showCreateForm && (
        <CreateRoomForm
          gameKey={gameKey}
          playerId={playerId}
          playerName={playerName}
          onSubmit={handleCreateRoom}
          onCancel={() => setShowCreateForm(false)}
        />
      )}
    </div>
  );
}

export default RoomLobby;
