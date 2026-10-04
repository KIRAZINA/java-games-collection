import { render, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import App from '../App.jsx';
import { roomsApi } from '../api/api.js';

// F1 regression guard (step6b.1): App.jsx used to bracket-destructure the ID
// generator's string result, so every mounted instance sent ownerId === "p".
// The id is only observable on the wire, so it is captured from createRoom.

function stubNetwork() {
  vi.stubGlobal(
    'fetch',
    vi.fn(async () => {
      throw new TypeError('network disabled for this test');
    })
  );
}

describe('F1 - each App instance owns a distinct playerId', () => {
  let createRoom;

  beforeEach(() => {
    createRoom = vi.spyOn(roomsApi, 'createRoom').mockRejectedValue(new Error('stopped after create'));
    vi.stubGlobal('prompt', vi.fn(() => 'Alice'));
    stubNetwork();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
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
