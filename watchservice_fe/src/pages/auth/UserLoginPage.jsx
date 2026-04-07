/**
 * 파일 이름 : UserLoginPage.jsx
 * 기능 : 일반 사용자 로그인 페이지. USER_AUTH_ENABLED=true 일 때만 표시된다.
 *        인증 성공 시 홈(/)으로 이동한다.
 */
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { post } from '../../api/HttpClient';

function UserLoginPage() {
  const navigate = useNavigate();
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const data = await post('/api/user/login', { password });
      if (data.authEnabled === false) {
        // 인증 비활성화 상태면 바로 통과
        navigate('/', { replace: true });
        return;
      }
      navigate('/', { replace: true });
    } catch (err) {
      setError('비밀번호가 올바르지 않습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
      <div style={{ width: 360 }}>
        <h1 style={{ marginBottom: 8 }}>WatchService 접속</h1>
        <p style={{ color: '#9ca3af', marginBottom: 24 }}>
          비밀번호를 입력하여 로그인하세요.
        </p>
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 12 }}>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="비밀번호"
              style={{ width: '100%', boxSizing: 'border-box' }}
              autoFocus
            />
          </div>
          <button className="btn" type="submit" disabled={loading} style={{ width: '100%' }}>
            {loading ? '확인 중...' : '로그인'}
          </button>
          {error && <p style={{ color: '#f87171', marginTop: 8 }}>{error}</p>}
        </form>
      </div>
    </div>
  );
}

export default UserLoginPage;
