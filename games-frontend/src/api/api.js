const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

export async function api(path, options = {}) {
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers ?? {}),
      },
      ...options,
    });
  } catch {
    throw new Error('Unable to reach the server. Please ensure the backend is running and try again.');
  }

  if (!response.ok) {
    let message = `Request failed with ${response.status}`;
    try {
      const error = await response.json();
      message = error.message ?? message;
    } catch {}
    throw new Error(message);
  }

  if (response.status === 204) return null;
  return response.json();
}

export const blackjackApi = {
  createSession: (initialBalance, difficulty) =>
    api('/api/blackjack/sessions', {
      method: 'POST',
      body: JSON.stringify({ initialBalance, difficulty }),
    }),
  getState: (sessionId) => api(`/api/blackjack/sessions/${sessionId}`),
  startRound: (sessionId) =>
    api(`/api/blackjack/sessions/${sessionId}/rounds`, { method: 'POST' }),
  placeBet: (sessionId, amount) =>
    api(`/api/blackjack/sessions/${sessionId}/bets`, {
      method: 'POST',
      body: JSON.stringify({ amount }),
    }),
  hit: (sessionId) =>
    api(`/api/blackjack/sessions/${sessionId}/hit`, { method: 'POST' }),
  stand: (sessionId) =>
    api(`/api/blackjack/sessions/${sessionId}/stand`, { method: 'POST' }),
  closeSession: (sessionId) =>
    api(`/api/blackjack/sessions/${sessionId}`, { method: 'DELETE' }),
};

export const minesweeperApi = {
  createSession: (rows, cols, mines) =>
    api('/api/minesweeper/sessions', {
      method: 'POST',
      body: JSON.stringify({ rows, cols, mines }),
    }),
  getState: (sessionId) => api(`/api/minesweeper/sessions/${sessionId}`),
  open: (sessionId, row, col) =>
    api(`/api/minesweeper/sessions/${sessionId}/open`, {
      method: 'POST',
      body: JSON.stringify({ row, col }),
    }),
  toggleFlag: (sessionId, row, col) =>
    api(`/api/minesweeper/sessions/${sessionId}/flag`, {
      method: 'POST',
      body: JSON.stringify({ row, col }),
    }),
   reset: (sessionId) =>
      api(`/api/minesweeper/sessions/${sessionId}/reset`, { method: 'POST' }),
   nextBoard: (sessionId) =>
      api(`/api/minesweeper/sessions/${sessionId}/next-board`, { method: 'POST' }),
   closeSession: (sessionId) =>
      api(`/api/minesweeper/sessions/${sessionId}`, { method: 'DELETE' }),
};

export const game2048Api = {
  createSession: () => api('/api/2048/sessions', { method: 'POST' }),
  getState: (sessionId) => api(`/api/2048/sessions/${sessionId}`),
  move: (sessionId, direction) =>
    api(`/api/2048/sessions/${sessionId}/moves`, {
      method: 'POST',
      body: JSON.stringify({ direction }),
    }),
  reset: (sessionId) =>
    api(`/api/2048/sessions/${sessionId}/reset`, { method: 'POST' }),
  closeSession: (sessionId) =>
    api(`/api/2048/sessions/${sessionId}`, { method: 'DELETE' }),
};

export const roomsApi = {
  listRooms: (gameType) => api(`/api/rooms?type=${gameType}`),
  listAllRooms: () => api('/api/rooms'),
  getRoom: (roomId) => api(`/api/rooms/${roomId}`),
  createRoom: (roomName, gameType, settings, ownerId, ownerName) =>
    api('/api/rooms', {
      method: 'POST',
      body: JSON.stringify({ roomName, gameType, settings, ownerId, ownerName }),
    }),
  joinRoom: (roomId, playerId, playerName, password) =>
    api(`/api/rooms/${roomId}/join`, {
      method: 'POST',
      body: JSON.stringify({ playerId, playerName, password }),
    }),
  leaveRoom: (roomId, playerId) =>
    api(`/api/rooms/${roomId}/leave`, {
      method: 'DELETE',
      body: JSON.stringify({ playerId }),
    }),
  deleteRoom: (roomId, requesterId) =>
    api(`/api/rooms/${roomId}`, {
      method: 'DELETE',
      body: JSON.stringify({ requesterId }),
    }),
   getRoomState: (roomId) => api(`/api/rooms/${roomId}/state`),
   getRoomProgress: (roomId) => api(`/api/rooms/${roomId}/progress`),
  registerSession: (roomId, playerId, sessionId) =>
    api(`/api/rooms/${roomId}/sessions`, {
      method: 'POST',
      body: JSON.stringify({ playerId, sessionId }),
    }),
  getRoomsForPlayer: (playerId) => api(`/api/rooms/player/${playerId}`),
  markReady: (roomId, playerId) =>
    api(`/api/rooms/${roomId}/ready`, {
      method: 'POST',
      body: JSON.stringify({ playerId }),
    }),
};
