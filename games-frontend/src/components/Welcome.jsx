export function Welcome({ onStart }) {
  return (
    <div className="game-layout compact-game" style={{ textAlign: 'center', paddingTop: '2rem' }}>
      <h2 style={{ fontSize: '2.4rem', marginBottom: '0.5rem' }}>Java Games Collection</h2>
      <p style={{ color: '#526174', fontSize: '1.15rem', maxWidth: 480, margin: '0 auto 2rem', lineHeight: 1.6 }}>
        A modern browser-based gaming platform featuring three classic games.
        Create or join rooms to play with others in real time.
      </p>
      <div style={{ display: 'grid', gap: '14px', maxWidth: 320, margin: '0 auto' }}>
        <button
          onClick={() => onStart('blackjack')}
          style={{ padding: '14px 20px', fontSize: '1.1rem', fontWeight: 700 }}
        >
          🃏 Play Blackjack
        </button>
        <button
          onClick={() => onStart('minesweeper')}
          style={{ padding: '14px 20px', fontSize: '1.1rem', fontWeight: 700 }}
        >
          💣 Play Minesweeper
        </button>
        <button
          onClick={() => onStart('2048')}
          style={{ padding: '14px 20px', fontSize: '1.1rem', fontWeight: 700 }}
        >
          🔢 Play 2048
        </button>
      </div>
    </div>
  );
}

export default Welcome;
