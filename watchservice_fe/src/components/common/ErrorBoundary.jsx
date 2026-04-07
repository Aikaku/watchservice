/**
 * 파일 이름 : ErrorBoundary.jsx
 * 기능 : React 런타임 오류를 잡아 흰 화면 대신 에러 안내 페이지를 표시한다.
 */
import React from 'react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    console.error('[ErrorBoundary]', error, info.componentStack);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{ padding: 40 }}>
          <h2 style={{ color: '#f87171' }}>화면을 불러오는 중 오류가 발생했습니다.</h2>
          <p style={{ color: '#9ca3af', marginBottom: 16 }}>
            {this.state.error?.message || '알 수 없는 오류'}
          </p>
          <button
            className="btn"
            onClick={() => this.setState({ hasError: false, error: null })}
          >
            다시 시도
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}

export default ErrorBoundary;
