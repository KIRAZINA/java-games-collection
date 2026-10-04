export function Welcome({ onStart, onQuickPlay }) {
  return (
    <div className="game-layout compact-game" style={{ textAlign: 'center', paddingTop: '2rem' }}>
      <h2 style={{ fontSize: '2.4rem', marginBottom: '0.5rem' }}>Java Games Collection</h2>
      <p style={{ color: 'var(--text-muted)', fontSize: '1.15rem', maxWidth: 480, margin: '0 auto 2rem', lineHeight: 1.6 }}>
        A modern browser-based gaming platform featuring three classic games.
        Create or join rooms to play with others in real time.
      </p>

      <h3 style={{ marginBottom: '0.75rem', color: 'var(--text)' }}>Multiplayer</h3>
      <div style={{ display: 'grid', gap: '14px', maxWidth: 320, margin: '0 auto 2rem' }}>
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

      <h3 style={{ marginBottom: '0.75rem', color: 'var(--text)' }}>Quick Play (Practice)</h3>
      <div style={{ display: 'grid', gap: '14px', maxWidth: 320, margin: '0 auto' }}>
        <button
          onClick={() => onQuickPlay('blackjack')}
          className="btn-primary"
          style={{ padding: '10px 20px', fontSize: '0.95rem', fontWeight: 600 }}
        >
          🃏 Blackjack (Solo)
        </button>
        <button
          onClick={() => onQuickPlay('minesweeper')}
          className="btn-primary"
          style={{ padding: '10px 20px', fontSize: '0.95rem', fontWeight: 600 }}
        >
          💣 Minesweeper (Solo)
        </button>
        <button
          onClick={() => onQuickPlay('2048')}
          className="btn-primary"
          style={{ padding: '10px 20px', fontSize: '0.95rem', fontWeight: 600 }}
        >
          🔢 2048 (Solo)
        </button>
      </div>
    </div>
  );
}

export default Welcome;
