/**
 * 파일 이름 : ProtectedRoute.jsx
 * 기능 : 관리자 인증이 필요한 라우트를 보호한다.
 *        세션 유효 → 그대로 렌더링, 미인증 → /admin/login 리다이렉트
 */
import React, { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { get } from '../../api/HttpClient';

function ProtectedRoute({ children }) {
  const [status, setStatus] = useState('loading'); // loading | ok | unauthorized

  useEffect(() => {
    get('/api/admin/check')
      .then(() => setStatus('ok'))
      .catch(() => setStatus('unauthorized'));
  }, []);

  if (status === 'loading') {
    return <div style={{ padding: 40, color: '#9ca3af' }}>인증 확인 중...</div>;
  }
  if (status === 'unauthorized') {
    return <Navigate to="/admin/login" replace />;
  }
  return children;
}

export default ProtectedRoute;
