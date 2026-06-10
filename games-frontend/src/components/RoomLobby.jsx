import { useCallback, useEffect, useState } from 'react';
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
  minesweeper: { settings: { rows: 9, cols: 9, mines: 10 }, maxPlayers: 1 },
  '2048': { settings: {}, maxPlayers: 1 },
};

function CreateRoomForm({ gameKey, playerId, playerName, onSubmit, onCancel }) {
  const [roomName, setRoomName] = useState(`${playerName}'s Room`);
  const [password, setPassword] = useState('');
  const [settings, setSettings] = useState(DEFAULT_SETTINGS[gameKey]?.settings ?? {});
  const [maxPlayers, setMaxPlayers] = useState(DEFAULT_SETTINGS[gameKey]?.maxPlayers ?? 4);

  function handleSubmit(e) {
    e.preventDefault();
    const gameType = GAME_TYPE_MAP[gameKey];
    const gameSettings = {
      gameType,
      settings,
      passwordProtected: password.length > 0,
      passwordHash: password,
      allowBots: false,
      maxPlayers,
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
            <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: '#526174' }}>
              Max Players
              <input type="number" min="1" max="8" value={maxPlayers}
                onChange={(e) => setMaxPlayers(Number(e.target.value))} />
            </label>
          </>
        )}

        {gameKey === 'minesweeper' && (
          <>
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
          </>
        )}

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

export function RoomLobby({ gameKey, playerId, playerName, onEnterGame }) {
  const [rooms, setRooms] = useState([]);
  const [error, setError] = useState('');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [busy, setBusy] = useState(false);

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

  async function handleCreateRoom(roomName, gameType, gameSettings, password) {
    setBusy(true);
    setError('');
    try {
      const summary = await roomsApi.createRoom(roomName, gameType, gameSettings, playerId, playerName);
      onEnterGame(summary.roomId, gameKey);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
      setShowCreateForm(false);
    }
  }

  async function handleJoinRoom(room) {
    if (busy) return;
    let password = '';
    if (room.passwordProtected) {
      password = prompt('This room is password protected. Enter password:', '') || '';
    }
    setBusy(true);
    setError('');
    try {
      await roomsApi.joinRoom(room.roomId, playerId, playerName, password || undefined);
      onEnterGame(room.roomId, gameKey);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="game-layout">
      <GameHeader title={`${GAME_LABELS[gameKey]} Lobby`} meta={`${rooms.length} room(s)`} />

      <div className="toolbar">
        <button onClick={fetchRooms} disabled={busy}>Refresh</button>
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
                {room.state !== 'WAITING' ? ` — ${room.state}` : ''}
              </div>
            </div>
            <button
              onClick={() => handleJoinRoom(room)}
              disabled={busy || room.state !== 'WAITING' || room.playerCount >= room.maxPlayers}
            >
              {room.state === 'WAITING' ? 'Join' : room.state}
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
