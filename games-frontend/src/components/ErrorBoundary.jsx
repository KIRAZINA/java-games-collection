import { Component } from 'react';

// Whole-app crash recovery (deferred since Step 4's report): React unmounts
// the whole tree on an uncaught render error, leaving a blank white page with
// no way back short of a manual reload. The boundary swaps the tree for a
// reload prompt instead. Mounted outermost in main.jsx so it covers the shell,
// the lobby and every game component.
export class ErrorBoundary extends Component {
  state = { error: null };

  static getDerivedStateFromError(error) {
    return { error };
  }

  render() {
    if (this.state.error) {
      return (
        <div className="error-boundary" role="alert">
          <h2>Something went wrong</h2>
          <p>The page hit an unexpected error and stopped rendering.</p>
          <p className="error-line">{this.state.error.message}</p>
          <button onClick={() => window.location.reload()}>Reload the page</button>
        </div>
      );
    }
    return this.props.children;
  }
}
