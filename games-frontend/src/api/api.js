const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

const ROOM_TOKENS_STORAGE_KEY = 'roomTokens';
let roomTokens = {};

function loadRoomTokens() {
  try {
    const raw = sessionStorage.getItem(ROOM_TOKENS_STORAGE_KEY);
    roomTokens = raw ? JSON.parse(raw) : {};
    if (!roomTokens || typeof roomTokens !== 'object' || Array.isArray(roomTokens)) roomTokens = {};
  } catch {
    roomTokens = {};
  }
}

function persistRoomTokens() {
  try {
    sessionStorage.setItem(ROOM_TOKENS_STORAGE_KEY, JSON.stringify(roomTokens));
  } catch {}
}

loadRoomTokens();

export function setRoomToken(roomId, token) {
  if (!roomId || !token) return;
  roomTokens[roomId] = token;
  persistRoomTokens();
}

export function clearRoomToken(roomId) {
  if (!roomId || !(roomId in roomTokens)) return;
  delete roomTokens[roomId];
  persistRoomTokens();
}

export function getRoomToken(roomId) {
  return roomTokens[roomId];
}

export function rehydrateRoomTokens() {
  loadRoomTokens();
}

export function extractRoomIdFromPath(path) {
  const match = /^\/api\/rooms\/(?!player(?:\/|$))([^/?]+)/.exec(path);
  if (!match) return null;
  return match[1];
}

function tokenForPath(path) {
  const roomId = extractRoomIdFromPath(path);
  if (!roomId) return undefined;
  return roomTokens[roomId];
}

export async function api(path, options = {}) {
  let response;
  try {
    const token = tokenForPath(path);
    response = await fetch(`${API_BASE}${path}`, {
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { 'X-Player-Token': token } : {}),
        ...(options.headers ?? {}),
      },
      ...options,
    });
  } catch {
    throw new Error('Unable to reach the server. Please ensure the backend is running and try again.');
  }

  if (!response.ok) {
    let message = response.status >= 500 ? 'Something went wrong' : `Request failed with ${response.status}`;
    try {
      const error = await response.json();
      message = error.message ?? error.error ?? message;
    } catch {}
    const err = new Error(message);
    err.status = response.status;
    if (response.status === 403) {
      const roomId = extractRoomIdFromPath(path);
      if (roomId) clearRoomToken(roomId);
    }
    throw err;
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
  getRoomsForPlayer: (playerId, token) =>
    api(`/api/rooms/player/${playerId}`, {
      headers: token ? { 'X-Player-Token': token } : {},
    }),
  markReady: (roomId, playerId) =>
    api(`/api/rooms/${roomId}/ready`, {
      method: 'POST',
      body: JSON.stringify({ playerId }),
    }),
};
