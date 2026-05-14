/**
 * 파일 이름 : UserProtectedRoute.jsx
 * 기능 : 일반 사용자 인증이 필요한 라우트를 보호한다.
 *        USER_AUTH_ENABLED=false(기본) 이면 항상 통과.
 *        활성화 시 미인증 → /login 리다이렉트
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
import React, { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { get } from '../../api/HttpClient';

/*
 * 함수 이름 : UserProtectedRoute
 * 기능 : 일반 사용자 인증이 필요한 라우트를 보호하는 컴포넌트. USER_AUTH_ENABLED가 비활성화면 항상 통과하고, 활성화 시 미인증이면 /login으로 리다이렉트한다.
 * 매개변수 : children - 보호할 자식 컴포넌트
 * 반환값 : JSX.Element
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
function UserProtectedRoute({ children }) {
  const [status, setStatus] = useState('loading'); // loading | ok | unauthorized

  useEffect(() => {
    get('/api/user/check')
      .then(() => setStatus('ok'))
      .catch((e) => {
        // HTTP 401 이면 인증 필요, 그 외(네트워크 오류 등)는 통과
        const is401 = e.message && e.message.startsWith('HTTP 401');
        setStatus(is401 ? 'unauthorized' : 'ok');
      });
  }, []);

  if (status === 'loading') return null;
  if (status === 'unauthorized') return <Navigate to="/login" replace />;
  return children;
}

export default UserProtectedRoute;
