/**
 * 파일 이름 : AdminLoginPage.jsx
 * 기능 : 관리자 로그인 페이지. 로그인 성공 시 /admin/main 으로 이동한다.
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminLogin } from '../../api/AdminApi';

/*
 * 함수 이름 : AdminLoginPage
 * 기능 : 관리자 로그인 페이지 컴포넌트. 아이디/비밀번호를 입력받아 로그인 요청을 처리한다.
 * 매개변수 : 없음
 * 반환값 : JSX.Element
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
function AdminLoginPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  /*
   * 함수 이름 : handleSubmit
   * 기능 : 로그인 폼 제출 시 관리자 로그인을 시도하고 성공 시 관리자 메인 페이지로 이동한다.
   * 매개변수 : e - 폼 제출 이벤트 객체
   * 반환값 : 없음
   * 작성 날짜 : 2026/03/08
   * 작성자 : 시스템
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await adminLogin(username, password);
      navigate('/admin/main');
    } catch (err) {
      console.error(err);
      setError('로그인 실패: 아이디 또는 비밀번호를 확인하세요.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container">
      <h1>관리자 로그인</h1>
      <form onSubmit={handleSubmit} style={{ maxWidth: 320 }}>
        <div style={{ marginBottom: 8 }}>
          <label>
            아이디
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              style={{ width: '100%' }}
            />
          </label>
        </div>
        <div style={{ marginBottom: 8 }}>
          <label>
            비밀번호
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              style={{ width: '100%' }}
            />
          </label>
        </div>
        <button className="btn" type="submit" disabled={loading}>
          {loading ? '로그인 중...' : '로그인'}
        </button>
        {error && <p style={{ color: 'red', marginTop: 8 }}>{error}</p>}
      </form>
    </div>
  );
}

export default AdminLoginPage;

