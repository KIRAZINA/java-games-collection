export function Welcome({ onStart, onQuickPlay }) {
  return (
    <div className="game-layout compact-game" style={{ textAlign: 'center', paddingTop: '2rem' }}>
      <h2 style={{ fontSize: '2.4rem', marginBottom: '0.5rem' }}>Java Games Collection</h2>
      <p style={{ color: '#526174', fontSize: '1.15rem', maxWidth: 480, margin: '0 auto 2rem', lineHeight: 1.6 }}>
        A modern browser-based gaming platform featuring three classic games.
        Create or join rooms to play with others in real time.
      </p>

      <h3 style={{ marginBottom: '0.75rem', color: '#243e68' }}>Multiplayer</h3>
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

      <h3 style={{ marginBottom: '0.75rem', color: '#526174' }}>Quick Play (Practice)</h3>
      <div style={{ display: 'grid', gap: '14px', maxWidth: 320, margin: '0 auto' }}>
        <button
          onClick={() => onQuickPlay('blackjack')}
          style={{ padding: '10px 20px', fontSize: '0.95rem', fontWeight: 600, background: '#e8f0fd', color: '#1d3a6f', borderColor: '#3466a8' }}
        >
          🃏 Blackjack (Solo)
        </button>
        <button
          onClick={() => onQuickPlay('minesweeper')}
          style={{ padding: '10px 20px', fontSize: '0.95rem', fontWeight: 600, background: '#e8f0fd', color: '#1d3a6f', borderColor: '#3466a8' }}
        >
          💣 Minesweeper (Solo)
        </button>
        <button
          onClick={() => onQuickPlay('2048')}
          style={{ padding: '10px 20px', fontSize: '0.95rem', fontWeight: 600, background: '#e8f0fd', color: '#1d3a6f', borderColor: '#3466a8' }}
        >
          🔢 2048 (Solo)
        </button>
      </div>
    </div>
  );
}

export default Welcome;
