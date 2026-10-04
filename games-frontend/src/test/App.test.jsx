import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import App from '../App.jsx';
import { roomsApi } from '../api/api.js';

// Step 6b.1 regression guards. playerId and the quick-play owner name are only
// observable on the wire, so both tests capture them from createRoom.
// The API is stubbed to reject AFTER the call is recorded: the assertions are
// about the request payload, and rejecting keeps the app in the room list
// instead of mounting a game component. Reaching the game is covered by the
// real-browser B4 scenario in e2e/step6b.spec.ts.

function stubNetwork() {
  vi.stubGlobal(
    'fetch',
    vi.fn(async () => {
      throw new TypeError('network disabled for this test');
    })
  );
}

function stubQuickPlay() {
  vi.stubGlobal('prompt', vi.fn(() => 'Alice'));
  stubNetwork();
}

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('F1 - each App instance owns a distinct playerId', () => {
  let createRoom;

  beforeEach(() => {
    createRoom = vi.spyOn(roomsApi, 'createRoom').mockRejectedValue(new Error('stopped after create'));
    stubQuickPlay();
  });

  it('two mounted App instances produce different, non-trivial playerIds', async () => {
    const user = userEvent.setup();
    const first = render(<App />);
    const second = render(<App />);

    await user.click(within(first.container).getByRole('button', { name: /Blackjack \(Solo\)/ }));
    await user.click(within(second.container).getByRole('button', { name: /Blackjack \(Solo\)/ }));

    await waitFor(() => expect(createRoom).toHaveBeenCalledTimes(2));

    const idA = createRoom.mock.calls[0][3];
    const idB = createRoom.mock.calls[1][3];

    expect(idA, 'first instance playerId').toBeTruthy();
    expect(idB, 'second instance playerId').toBeTruthy();
    expect(idA.length, 'playerId longer than the single "p" character').toBeGreaterThan(1);
    expect(idB.length, 'playerId longer than the single "p" character').toBeGreaterThan(1);
    expect(idA).toMatch(/^player-/);
    expect(idB).toMatch(/^player-/);
    expect(idA, 'the two instances must not share one identity').not.toBe(idB);
    expect(idA).not.toBe('p');
    expect(idB).not.toBe('p');
  });
});

describe('F2 - quick play uses the prompted name instead of stale state', () => {
  let createRoom;

  beforeEach(() => {
    createRoom = vi.spyOn(roomsApi, 'createRoom').mockRejectedValue(new Error('stopped after create'));
    stubQuickPlay();
  });

  it('sends ownerName "Alice" to createRoom even though playerName state is still empty', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /Blackjack \(Solo\)/ }));

    await waitFor(() => expect(createRoom).toHaveBeenCalledTimes(1));

    const [roomName, gameType, settings, ownerId, ownerName] = createRoom.mock.calls[0];
    expect(ownerName).toBe('Alice');
    expect(roomName).toBe("Alice's Practice");
    expect(gameType).toBe('BLACKJACK');
    expect(settings.isSinglePlayer).toBe(true);
    expect(ownerId).toMatch(/^player-.+/);

    expect(window.prompt).toHaveBeenCalledWith('Enter your display name:', 'Player');
    await screen.findByText('Playing as');
    expect(screen.getByText('Alice')).toBeInTheDocument();
  });
});
