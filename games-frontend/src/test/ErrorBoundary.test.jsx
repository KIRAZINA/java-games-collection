import { render, screen } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ErrorBoundary } from '../components/ErrorBoundary.jsx';

function Boom({ message = 'kaboom' }) {
  throw new Error(message);
}

// Step 6b.3 (deferred item from Step 4): whole-app crash recovery. React logs
// the caught error through console.error, which this suite does not want as
// noise, hence the spy.
describe('ErrorBoundary', () => {
  let consoleError;

  beforeEach(() => {
    consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleError.mockRestore();
  });

  it('renders its children untouched when nothing throws', () => {
    render(
      <ErrorBoundary>
        <p>all good</p>
      </ErrorBoundary>
    );
    expect(screen.getByText('all good')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('replaces a crashed tree with the fallback instead of a blank page', () => {
    render(
      <ErrorBoundary>
        <Boom />
      </ErrorBoundary>
    );
    expect(screen.getByRole('alert')).toHaveTextContent('Something went wrong');
    expect(screen.getByText('kaboom')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Reload the page' })
    ).toBeInTheDocument();
  });

  it('reports the real error message for the crash it caught', () => {
    render(
      <ErrorBoundary>
        <Boom message="Cannot read properties of undefined (reading 'map')" />
      </ErrorBoundary>
    );
    expect(screen.getByRole('alert')).toHaveTextContent(
      "Cannot read properties of undefined (reading 'map')"
    );
  });

  it('keeps the fallback up (it does not silently resume) until a reload', () => {
    const { rerender } = render(
      <ErrorBoundary>
        <Boom />
      </ErrorBoundary>
    );
    expect(screen.getByRole('alert')).toBeInTheDocument();

    // Even if the parent re-renders with healthy children, the boundary stays
    // in its error state - only a reload clears it, which is what the button
    // does in the browser.
    rerender(
      <ErrorBoundary>
        <p>healthy now</p>
      </ErrorBoundary>
    );
    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.queryByText('healthy now')).not.toBeInTheDocument();
  });
});
