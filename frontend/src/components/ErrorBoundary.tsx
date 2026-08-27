import { Component, type ErrorInfo, type ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  error: Error | null;
}

/**
 * Class boundary so render failures surface instead of a blank screen.
 * Uses plain DOM only — must not depend on ThemeProvider/MUI.
 */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('UI error boundary caught', error, info.componentStack);
  }

  render() {
    if (this.state.error) {
      return (
        <div style={{ fontFamily: 'system-ui, sans-serif', maxWidth: 560, margin: '64px auto', padding: 24 }}>
          <h1 style={{ fontSize: 22, marginBottom: 12 }}>Something went wrong</h1>
          <p style={{ color: '#555', marginBottom: 16 }}>{this.state.error.message}</p>
          <button
            type="button"
            style={{ padding: '8px 16px', cursor: 'pointer' }}
            onClick={() => {
              window.location.href = '/login';
            }}
          >
            Return to sign in
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
