import { useMemo, useState } from 'react';
import { Welcome } from './components/Welcome.jsx';
import { RoomLobby } from './components/RoomLobby.jsx';
import { Blackjack } from './components/Blackjack.jsx';
import { Minesweeper } from './components/Minesweeper.jsx';
import { Game2048 } from './components/Game2048.jsx';
import { ConfirmNavigationModal } from './components/ConfirmNavigationModal.jsx';
import { roomsApi } from './api/api.js';

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

function App() {
  const [page, setPage] = useState('welcome');
  const [activeGame, setActiveGame] = useState(null);
  const [currentRoom, setCurrentRoom] = useState(null);
  const [pendingNavigationTarget, setPendingNavigationTarget] = useState(null);
  const [playerId] = useMemo(() => `player-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`, []);
  const [playerName, setPlayerName] = useState('');

  function handleSelectGame(game) {
    setActiveGame(game);
    if (!playerName) {
      const name = prompt('Enter your display name:', 'Player');
      setPlayerName(name || `Player-${playerId.slice(-4)}`);
    }
    setPage('lobby');
  }

  function handleSidebarClick(targetGame) {
    if (!playerName) {
      const name = prompt('Enter your display name:', 'Player');
      setPlayerName(name || `Player-${playerId.slice(-4)}`);
    }

    if (!currentRoom) {
      setActiveGame(targetGame);
      setPage('lobby');
      return;
    }

    if (currentRoom.gameType === targetGame) {
      setActiveGame(targetGame);
      setPage('game');
      return;
    }

    setActiveGame(targetGame);
    setPendingNavigationTarget(targetGame);
  }

  async function handleConfirmNavigation() {
    const targetGame = pendingNavigationTarget;
    if (!targetGame) return;

    const room = currentRoom;
    setCurrentRoom(null);
    setPendingNavigationTarget(null);

    if (room) {
      try {
        await roomsApi.leaveRoom(room.roomId, room.playerId);
      } catch {
        // Room may already be gone — proceed
      }
    }

    setPage('lobby');
  }

  function handleCancelNavigation() {
    setPendingNavigationTarget(null);
    if (currentRoom) {
      setActiveGame(currentRoom.gameType);
      setPage('game');
    } else {
      setPage('lobby');
    }
  }

  function handleEnterGame(enteredRoomId, gameType) {
    setCurrentRoom({ roomId: enteredRoomId, gameType, playerId });
    setActiveGame(gameType);
    setPage('game');
  }

  async function handleQuickPlay(gameKey) {
    if (!playerName) {
      const name = prompt('Enter your display name:', 'Player');
      setPlayerName(name || `Player-${playerId.slice(-4)}`);
    }

    if (currentRoom) {
      try {
        await roomsApi.leaveRoom(currentRoom.roomId, currentRoom.playerId);
      } catch {}
      setCurrentRoom(null);
    }

    setActiveGame(gameKey);
    setPage('lobby');

    const defaults = DEFAULT_SETTINGS[gameKey];
    const gameType = GAME_TYPE_MAP[gameKey];
    const gameSettings = {
      gameType,
      settings: defaults.settings,
      passwordProtected: false,
      passwordHash: '',
      allowBots: false,
      maxPlayers: 1,
      timeLimitSeconds: gameKey === 'blackjack' ? 0 : 60,
      isSinglePlayer: true,
    };

    try {
      const summary = await roomsApi.createRoom(
        `${playerName}'s Practice`,
        gameType,
        gameSettings,
        playerId,
        playerName
      );
      handleEnterGame(summary.roomId, gameKey);
    } catch {
      setPage('lobby');
    }
  }

  async function handleExitRoom() {
    if (!currentRoom) return;
    try {
      await roomsApi.leaveRoom(currentRoom.roomId, currentRoom.playerId);
    } catch {
      // Room may already be gone — proceed
    }
    setCurrentRoom(null);
    setPage('lobby');
  }

  function handleGoHome() {
    setPage('welcome');
    setActiveGame(null);
    setCurrentRoom(null);
    if (currentRoom) {
      roomsApi.leaveRoom(currentRoom.roomId, currentRoom.playerId).catch(() => {});
    }
  }

  const navGame = pendingNavigationTarget || activeGame;

  return (
    <main className="app-shell">
      <aside className="game-nav" aria-label="Game selection">
        <div>
          <p className="eyebrow">Games Hub</p>
          <h1>Play Desk</h1>
        </div>
        {page !== 'welcome' && (
          <button onClick={handleGoHome} style={{ marginBottom: 8 }}>
            ← Home
          </button>
        )}
        <div className="nav-buttons" role="tablist" aria-label="Available games">
          <button
            id="nav-blackjack"
            className={activeGame === 'blackjack' ? 'active' : ''}
            onClick={() => handleSidebarClick('blackjack')}
            role="tab"
            aria-selected={activeGame === 'blackjack'}
          >
            🃏 Blackjack
          </button>
          <button
            id="nav-minesweeper"
            className={activeGame === 'minesweeper' ? 'active' : ''}
            onClick={() => handleSidebarClick('minesweeper')}
            role="tab"
            aria-selected={activeGame === 'minesweeper'}
          >
            💣 Minesweeper
          </button>
          <button
            id="nav-2048"
            className={activeGame === '2048' ? 'active' : ''}
            onClick={() => handleSidebarClick('2048')}
            role="tab"
            aria-selected={activeGame === '2048'}
          >
            🔢 2048
          </button>
        </div>
        {playerName && (
          <p style={{ margin: 'auto 0 0', fontSize: '0.78rem', color: '#607088' }}>
            Playing as <strong>{playerName}</strong>
          </p>
        )}
      </aside>

      <section className="game-stage">
        {page === 'welcome' && (
          <Welcome onStart={handleSelectGame} onQuickPlay={handleQuickPlay} />
        )}
        {page === 'lobby' && activeGame && (
          <RoomLobby
            gameKey={activeGame}
            playerId={playerId}
            playerName={playerName}
            onEnterGame={handleEnterGame}
            onQuickPlay={handleQuickPlay}
          />
        )}
        {page === 'game' && activeGame === 'blackjack' && (
          <Blackjack roomId={currentRoom?.roomId} playerId={playerId} playerName={playerName} onExit={handleExitRoom} />
        )}
        {page === 'game' && activeGame === 'minesweeper' && (
          <Minesweeper roomId={currentRoom?.roomId} playerId={playerId} playerName={playerName} onExit={handleExitRoom} />
        )}
        {page === 'game' && activeGame === '2048' && (
          <Game2048 roomId={currentRoom?.roomId} playerId={playerId} playerName={playerName} onExit={handleExitRoom} />
        )}
      </section>

      {pendingNavigationTarget && currentRoom && (
        <ConfirmNavigationModal
          currentGame={currentRoom.gameType}
          targetGame={pendingNavigationTarget}
          onConfirm={handleConfirmNavigation}
          onCancel={handleCancelNavigation}
        />
      )}
    </main>
  );
}

export default App;
