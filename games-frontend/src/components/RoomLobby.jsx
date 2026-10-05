import { useCallback, useEffect, useRef, useState } from 'react';
import { roomsApi, setRoomToken, clearRoomToken, getRoomToken } from '../api/api.js';
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

function CreateRoomForm({ gameKey, playerId, playerName, onSubmit, onCancel, busy }) {
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
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)',
      display: 'grid', placeItems: 'center', zIndex: 100,
    }}>
      <form onSubmit={handleSubmit} style={{
        background: 'var(--surface)', borderRadius: 10, padding: 24,
        minWidth: 340, maxWidth: 420, display: 'grid', gap: 14,
        border: '1px solid var(--border)',
      }}>
        <h3 style={{ margin: 0 }}>Create {GAME_LABELS[gameKey]} Room</h3>

        <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: 'var(--text-muted)' }}>
          Room Name
          <input value={roomName} onChange={(e) => setRoomName(e.target.value)} required />
        </label>

        {gameKey === 'blackjack' && (
          <>
            <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              Dealer Difficulty
              <select value={settings.difficulty} onChange={(e) => setSettings({ ...settings, difficulty: e.target.value })}>
                <option value="BASIC">Basic</option>
                <option value="CONSERVATIVE">Conservative</option>
                <option value="AGGRESSIVE">Aggressive</option>
              </select>
            </label>
            <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              Starting Balance
              <input type="number" min="1" value={settings.initialBalance}
                onChange={(e) => setSettings({ ...settings, initialBalance: Number(e.target.value) })} />
            </label>
          </>
        )}

        {gameKey === 'minesweeper' && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8 }}>
            <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              Rows
              <input type="number" min="4" max="30" value={settings.rows}
                onChange={(e) => setSettings({ ...settings, rows: Number(e.target.value) })} />
            </label>
            <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              Cols
              <input type="number" min="4" max="30" value={settings.cols}
                onChange={(e) => setSettings({ ...settings, cols: Number(e.target.value) })} />
            </label>
            <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              Mines
              <input type="number" min="1" value={settings.mines}
                onChange={(e) => setSettings({ ...settings, mines: Number(e.target.value) })} />
            </label>
          </div>
        )}

        {gameKey !== 'blackjack' && (
          <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: 'var(--text-muted)' }}>
            Time Limit
            <select value={timeLimitSeconds} onChange={(e) => setTimeLimitSeconds(Number(e.target.value))}>
              <option value={30}>30 seconds</option>
              <option value={60}>1 minute</option>
              <option value={180}>3 minutes</option>
            </select>
          </label>
        )}

        {gameKey !== 'blackjack' && (
          <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontWeight: 700, fontSize: '0.82rem', color: 'var(--text-muted)', cursor: 'pointer' }}>
            <input type="checkbox" checked={isSinglePlayer}
              onChange={(e) => setIsSinglePlayer(e.target.checked)} style={{ width: 'auto' }} />
            Single Player (practice mode)
          </label>
        )}

        <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: 'var(--text-muted)' }}>
          Max Players
          <input type="number" min="1" max="8" value={effectiveMaxPlayers}
            onChange={(e) => setMaxPlayers(Number(e.target.value))}
            disabled={isSinglePlayer} />
        </label>

        <label style={{ display: 'grid', gap: 4, fontWeight: 700, fontSize: '0.82rem', color: 'var(--text-muted)' }}>
          Password (optional)
          <input type="password" value={password} placeholder="Leave blank for public room"
            onChange={(e) => setPassword(e.target.value)} />
        </label>

        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', paddingTop: 6 }}>
          <button type="button" onClick={onCancel} disabled={busy}>Cancel</button>
          {/* Step 6g E: while the create request is in flight the submit button
              must say what is happening and refuse a second click. */}
          <button type="submit" className="btn-primary" disabled={busy}>
            {busy ? 'Creating room…' : 'Create Room'}
          </button>
        </div>
      </form>
    </div>
  );
}

function ReadyCheckOverlay({ roomId, playerId, roomPhase, timeRemaining, onReadySent, onAuthError }) {
  const [readySent, setReadySent] = useState(false);
  const [readyError, setReadyError] = useState('');
  const [readyBusy, setReadyBusy] = useState(false);

  const countdown = roomPhase === 'READY_CHECK' ? Math.max(0, Math.min(3, timeRemaining)) : null;

  async function handleMarkReady() {
    if (readyBusy) return;
    setReadyBusy(true);
    setReadyError('');
    try {
      const result = await roomsApi.markReady(roomId, playerId);
      // Token rotation (Step 6b.3): ready invalidates the token this request
      // authenticated with and returns the replacement. Store it before
      // anything else can fire an authenticated call - the game registers its
      // session the moment the match starts, and this lobby's own leave uses
      // the same stored token.
      if (result?.playerToken) setRoomToken(roomId, result.playerToken);
      setReadySent(true);
      onReadySent?.();
    } catch (err) {
      if (err.status === 403) {
        onAuthError?.(err);
      } else {
        setReadyError(err.message);
      }
    } finally {
      setReadyBusy(false);
    }
  }

  return (
    <div className="ready-check-overlay">
      <div className="ready-check-card">
        {readyError && <p className="error-line" role="alert">{readyError}</p>}
        {roomPhase === 'LOBBY' && !readySent && (
          <>
            <h2>Get Ready</h2>
            <p>Press the button when you're ready to start</p>
            <button className="ready-button" onClick={handleMarkReady} disabled={readyBusy}>
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

export function RoomLobby({ gameKey, playerId, playerName, onEnterGame, onQuickPlay, onAuthLost, notice, creating }) {
  const [rooms, setRooms] = useState([]);
  const [error, setError] = useState('');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [busy, setBusy] = useState(false);

  const [activeRoomId, setActiveRoomId] = useState(null);
  const [roomPhase, setRoomPhase] = useState(null);
  const [timeRemaining, setTimeRemaining] = useState(0);
  const [readySent, setReadySent] = useState(false);
  const pollRef = useRef(null);
  // Step 6e B1: lets markReady() trigger one immediate /state fetch so the
  // overlay advances straight to the countdown / handoff without waiting for
  // the next 500ms tick.
  const pollNowRef = useRef(null);
  // G1 (step 6b.2): this lobby owns the room membership from create/join until
  // the room reaches PLAYING - which is why it, and not App, has to leave the
  // room when the player walks out (three cases on the effect below).
  // handedOffRef marks the moment the game takes over.
  const handedOffRef = useRef(false);

  // Three cases decide whether this leave runs - do not "simplify" it away:
  //   1. handed off to the game (handedOffRef) -> the game owns the room from
  //      here; leaving would abandon the match the moment it starts.
  //   2. auth lost, token already cleared -> api.js/the 403 path cleaned up,
  //      and /leave verifies X-Player-Token, so the request could only 403.
  //   3. otherwise -> this lobby still owns the membership (Home, game switch,
  //      remount): App's own leave only knows about currentRoom, which is set
  //      later, when the game actually opens. Leave now.
  useEffect(() => {
    if (!activeRoomId) return undefined;
    return () => {
      if (handedOffRef.current) return;
      if (!getRoomToken(activeRoomId)) return;
      roomsApi.leaveRoom(activeRoomId, playerId).catch(() => {});
      clearRoomToken(activeRoomId);
    };
  }, [activeRoomId, playerId]);

  // Switching games while waiting would otherwise keep showing the old room's
  // overlay under the new game's lobby; dropping the room runs the leave above.
  useEffect(() => {
    setActiveRoomId(null);
  }, [gameKey]);

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
      pollNowRef.current = null;
      if (pollRef.current) {
        clearInterval(pollRef.current);
        pollRef.current = null;
      }
      return;
    }

    let settled = false;
    const pollNow = async () => {
      if (settled) return;
      try {
        const st = await roomsApi.getRoomState(activeRoomId);
        setRoomPhase(st.roomPhase);

        if (st.roomPhase === 'READY_CHECK') {
          setTimeRemaining(st.timeRemaining);
        }

        if (st.roomPhase === 'PLAYING' || st.roomPhase === 'GAME_OVER') {
          settled = true;
          if (pollRef.current) {
            clearInterval(pollRef.current);
            pollRef.current = null;
          }
          handedOffRef.current = true;
          onEnterGame(activeRoomId, gameKey);
        }
      } catch {
        settled = true;
        if (pollRef.current) {
          clearInterval(pollRef.current);
          pollRef.current = null;
        }
        setActiveRoomId(null);
      }
    };

    pollNowRef.current = pollNow;
    // Step 6e B1: poll immediately so the ready overlay renders its content on
    // the first frame instead of staying blank until the first 500ms tick.
    pollNow();
    pollRef.current = setInterval(pollNow, 500);

    return () => {
      pollNowRef.current = null;
      if (pollRef.current) clearInterval(pollRef.current);
      pollRef.current = null;
    };
  }, [activeRoomId, gameKey, onEnterGame]);

  // Step 6g E: the 5xx "error after side effect" case - the room may exist on
  // the server even though the create request failed. Refetch the list and, if
  // the room we just asked for is there, hand it back instead of showing an
  // error. Matching on name + owner + solo flag + newest-createdAt is the
  // best the summary payload allows (RoomSummary carries no ownerId).
  async function recoverOwnRoom(roomName, isSinglePlayer, expectedOwnerName) {
    try {
      const rooms = (await roomsApi.listRooms(GAME_TYPE_MAP[gameKey])) ?? [];
      return rooms
        .filter((r) => r.roomName === roomName && r.isSinglePlayer === isSinglePlayer && r.ownerName === expectedOwnerName)
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))[0];
    } catch {
      return null;
    }
  }

  async function handleCreateRoom(roomName, gameType, gameSettings, password) {
    setBusy(true);
    setError('');
    try {
      const summary = await roomsApi.createRoom(roomName, gameType, gameSettings, playerId, playerName);
      setRoomToken(summary.roomId, summary.playerToken);
      // G1 (step 6b.2): every room that still has a ready step waits in this
      // lobby - the game components' waiting cards have no way to mark a player
      // ready, so sending players there meant nobody could ever start a
      // multiplayer game. Only a solo blackjack room skips straight through.
      if (summary.isSinglePlayer && gameKey === 'blackjack') {
        onEnterGame(summary.roomId, gameKey);
      } else {
        setActiveRoomId(summary.roomId);
        // Step 6e B1: refresh the list so the new room shows up right away when
        // we return to it, instead of waiting for the next 5s poll. The ready
        // overlay's own /state is fetched immediately by the poll effect above.
        fetchRooms();
      }
    } catch (err) {
      // Step 6g E: on a 5xx the room may have been created before the response
      // failed - route into it instead of reporting an error.
      if (err.status >= 500) {
        const own = await recoverOwnRoom(roomName, gameSettings.isSinglePlayer, playerName);
        if (own) {
          if (own.isSinglePlayer && gameKey === 'blackjack') {
            onEnterGame(own.roomId, gameKey);
          } else {
            setActiveRoomId(own.roomId);
          }
          return;
        }
      }
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
      const summary = await roomsApi.joinRoom(room.roomId, playerId, playerName, password || undefined);
      setRoomToken(summary.roomId, summary.playerToken);
      // G1 (step 6b.2): same rule as create - wait here until PLAYING.
      if (room.isSinglePlayer && gameKey === 'blackjack') {
        onEnterGame(summary.roomId, gameKey);
      } else {
        setActiveRoomId(summary.roomId);
        // Step 6e B1: refresh the list so the room we just joined is current
        // when we return to it, instead of waiting for the next 5s poll.
        fetchRooms();
      }
    } catch (err) {
      // Step 6g E: on a 5xx the server state may have moved (the room existed
      // and the response path failed) - refresh the list so the user sees the
      // actual state alongside the error.
      if (err.status >= 500) fetchRooms();
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  function handleReadyAuthError(err) {
    clearRoomToken(activeRoomId);
    setActiveRoomId(null);
    onAuthLost?.(`${err.message}. Please rejoin the room.`);
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
          onReadySent={() => {
            setReadySent(true);
            // Step 6e B1: hit /state once more so the countdown / handoff shows
            // immediately after readying, not on the next 500ms tick.
            pollNowRef.current?.();
          }}
          onAuthError={handleReadyAuthError}
        />
      </div>
    );
  }

  return (
    <div className="game-layout">
      <GameHeader title={`${GAME_LABELS[gameKey]} Lobby`} meta={`${rooms.length} room(s)`} />

      <div className="toolbar">
        <button onClick={fetchRooms} disabled={busy || creating}>Refresh</button>
        {/* Step 6g E: while quick play's create request is in flight the button
            says so and refuses a second click - the silent-swallow bug that
            made "Quick Play does nothing" possible. */}
        <button onClick={() => onQuickPlay(gameKey)} disabled={busy || creating}
          className="btn-primary">
          {creating ? 'Creating room…' : '⚡ Quick Play (Solo)'}
        </button>
        <button onClick={() => setShowCreateForm(true)} disabled={busy || creating}
          className="btn-primary">
          + Create Room
        </button>
      </div>

      {error && <p className="error-line" role="alert">{error}</p>}
      {notice && <p className="error-line" role="alert">{notice}</p>}

      {rooms.length === 0 && !error && (
        <p style={{ color: 'var(--text-muted)', padding: '1rem 0' }}>
          No active rooms. Create one to get started!
        </p>
      )}

      <div style={{ display: 'grid', gap: 10 }}>
        {rooms.map((room) => (
          <div key={room.roomId} style={{
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            border: '1px solid var(--border)', borderRadius: 8, background: 'var(--surface)',
            padding: '12px 16px',
          }}>
            <div>
              <strong>{room.roomName}</strong>
              <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginTop: 4 }}>
                {room.playerCount}/{room.maxPlayers} players
                {room.passwordProtected ? ' 🔒' : ''}
                {room.phase !== 'LOBBY' ? ` — ${room.phase}` : ''}
                {room.timeLimitSeconds > 0 ? ` — ${formatTime(room.timeLimitSeconds)}` : ''}
                {gameKey !== 'blackjack' && room.isSinglePlayer ? ' — Solo' : ''}
              </div>
            </div>
            <button
              onClick={() => handleJoinRoom(room)}
              className="btn-primary"
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
          busy={busy}
        />
      )}
    </div>
  );
}

export default RoomLobby;
