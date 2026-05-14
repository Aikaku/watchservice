/**
 * 파일 이름 : ErrorBoundary.jsx
 * 기능 : React 런타임 오류를 잡아 흰 화면 대신 에러 안내 페이지를 표시한다.
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
import React from 'react';

/*
 * 함수 이름 : ErrorBoundary
 * 기능 : React 런타임 오류를 잡아 에러 안내 화면을 표시하는 클래스 컴포넌트. hasError 상태일 때 에러 메시지와 재시도 버튼을 표시한다.
 * 매개변수 : children - 자식 컴포넌트
 * 반환값 : JSX.Element
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
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
