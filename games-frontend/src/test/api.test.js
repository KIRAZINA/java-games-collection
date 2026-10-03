import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import {
  extractRoomIdFromPath, roomsApi, blackjackApi,
  setRoomToken, getRoomToken, rehydrateRoomTokens,
} from '../api/api';

describe('extractRoomIdFromPath', () => {
  it('extracts roomId from a nested room path', () => {
    expect(extractRoomIdFromPath('/api/rooms/abc/leave')).toBe('abc');
  });

  it('extracts roomId from a bare room path', () => {
    expect(extractRoomIdFromPath('/api/rooms/abc')).toBe('abc');
  });

  it('returns null for /api/rooms/player/{playerId} (never auto-attach)', () => {
    expect(extractRoomIdFromPath('/api/rooms/player/xyz')).toBeNull();
  });

  it('returns null for the room list path', () => {
    expect(extractRoomIdFromPath('/api/rooms')).toBeNull();
  });

  it('returns null for the room list path with query string', () => {
    expect(extractRoomIdFromPath('/api/rooms?type=BLACKJACK')).toBeNull();
  });
});

describe('Area 5 - 403 responses expose the status and drop the stale room token', () => {
  beforeEach(() => {
    sessionStorage.clear();
    rehydrateRoomTokens();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  function stub403(body) {
    vi.stubGlobal('fetch', vi.fn(async () => ({
      ok: false,
      status: 403,
      json: async () => body,
    })));
  }

  it('attaches the HTTP status to the thrown error (403 on a room endpoint)', async () => {
    stub403({ error: 'Invalid or missing player token', status: 403 });
    const err = await roomsApi.markReady('r-1', 'p-1').catch((e) => e);
    expect(err).toBeInstanceOf(Error);
    expect(err.status).toBe(403);
  });

  it('attaches the HTTP status for other failures too (404)', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => ({
      ok: false,
      status: 404,
      json: async () => ({ error: 'Room not found: r-9', status: 404 }),
    })));
    const err = await roomsApi.getRoomState('r-9').catch((e) => e);
    expect(err.status).toBe(404);
  });

  it('clears the stored room token when a room endpoint answers 403', async () => {
    setRoomToken('r-1', 'stale-token');
    stub403({ error: 'Invalid or missing player token', status: 403 });
    await expect(roomsApi.markReady('r-1', 'p-1'))
      .rejects.toThrow('Invalid or missing player token');
    expect(getRoomToken('r-1')).toBeUndefined();
  });

  it('omits the X-Player-Token header on the next request after a 403 cleared the token', async () => {
    setRoomToken('r-1', 'stale-token');
    stub403({ error: 'Invalid or missing player token', status: 403 });
    await roomsApi.markReady('r-1', 'p-1').catch(() => {});

    const successFetch = vi.fn(async () => ({ ok: true, status: 200, json: async () => ({}) }));
    vi.stubGlobal('fetch', successFetch);
    await roomsApi.getRoomState('r-1');

    const headers = successFetch.mock.calls[0][1].headers;
    expect(headers['X-Player-Token']).toBeUndefined();
  });

  it('does NOT clear a room token when a non-room endpoint answers 403', async () => {
    setRoomToken('r-1', 'valid-token');
    vi.stubGlobal('fetch', vi.fn(async () => ({
      ok: false,
      status: 403,
      json: async () => ({ error: 'Forbidden', status: 403 }),
    })));
    await blackjackApi.getState('s-1').catch(() => {});
    expect(getRoomToken('r-1')).toBe('valid-token');
  });
});
