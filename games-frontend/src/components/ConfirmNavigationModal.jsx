const GAME_LABELS = {
  blackjack: 'Blackjack',
  minesweeper: 'Minesweeper',
  '2048': '2048',
};

export function ConfirmNavigationModal({ currentGame, targetGame, onConfirm, onCancel }) {
  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)',
      display: 'grid', placeItems: 'center', zIndex: 200,
    }}>
      <div style={{
        background: 'var(--surface)', borderRadius: 10, padding: 24,
        minWidth: 340, maxWidth: 420, display: 'grid', gap: 14,
        border: '1px solid var(--border)',
      }}>
        <h3 style={{ margin: 0, color: 'var(--danger)' }}>Leave Current Game?</h3>
        <p style={{ margin: 0, lineHeight: 1.6, color: 'var(--text)' }}>
          You are currently in an active <strong>{GAME_LABELS[currentGame]}</strong> room.
          Navigating to <strong>{GAME_LABELS[targetGame]}</strong> will close your current room.
          Are you sure you want to leave?
        </p>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', paddingTop: 6 }}>
          <button onClick={onCancel}>Cancel</button>
          <button onClick={onConfirm}
            style={{ background: 'var(--danger)', color: 'var(--bg)', borderColor: 'var(--danger)' }}>
            Leave & Navigate
          </button>
        </div>
      </div>
    </div>
  );
}

export default ConfirmNavigationModal;
