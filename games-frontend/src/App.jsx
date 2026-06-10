import { useMemo, useState } from 'react';
import { Welcome } from './components/Welcome.jsx';
import { RoomLobby } from './components/RoomLobby.jsx';
import { Blackjack } from './components/Blackjack.jsx';
import { Minesweeper } from './components/Minesweeper.jsx';
import { Game2048 } from './components/Game2048.jsx';
import { roomsApi } from './api/api.js';

function App() {
  const [page, setPage] = useState('welcome');
  const [activeGame, setActiveGame] = useState(null);
  const [currentRoom, setCurrentRoom] = useState(null);
  const [playerId] = useMemo(() => `player-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`, []);
  const [playerName, setPlayerName] = useState('');

  function handleSelectGame(game) {
    setActiveGame(game);
    if (!playerName) {
      const name = prompt('Enter your display name:', 'Player');
      setPlayerName(name || `Player-${playerId.slice(-4)}`);
    }
    setPage('lobby');
    setCurrentRoom(null);
  }

  function handleEnterGame(enteredRoomId, gameType) {
    setCurrentRoom({ roomId: enteredRoomId, gameType, playerId });
    setActiveGame(gameType);
    setPage('game');
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
  }

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
            onClick={() => handleSelectGame('blackjack')}
            role="tab"
            aria-selected={activeGame === 'blackjack'}
          >
            🃏 Blackjack
          </button>
          <button
            id="nav-minesweeper"
            className={activeGame === 'minesweeper' ? 'active' : ''}
            onClick={() => handleSelectGame('minesweeper')}
            role="tab"
            aria-selected={activeGame === 'minesweeper'}
          >
            💣 Minesweeper
          </button>
          <button
            id="nav-2048"
            className={activeGame === '2048' ? 'active' : ''}
            onClick={() => handleSelectGame('2048')}
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
        {page === 'welcome' && <Welcome onStart={handleSelectGame} />}
        {page === 'lobby' && activeGame && (
          <RoomLobby
            gameKey={activeGame}
            playerId={playerId}
            playerName={playerName}
            onEnterGame={handleEnterGame}
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
    </main>
  );
}

export default App;
