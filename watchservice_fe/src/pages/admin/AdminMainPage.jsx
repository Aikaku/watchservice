/**
 * 파일 이름 : AdminMainPage.jsx
 * 기능 : 관리자 메인 페이지. 피드백/공지 관리 페이지로 진입하는 메뉴를 제공한다.
 */
import React from 'react';
import { Link } from 'react-router-dom';
import { adminLogout } from '../../api/AdminApi';

function AdminMainPage() {
  const handleLogout = async () => {
    try {
      await adminLogout();
      window.location.href = '/admin/login';
    } catch (e) {
      console.error(e);
      alert('로그아웃 중 오류가 발생했습니다.');
    }
  };

  return (
    <div className="page-container">
      <h1>관리자 메인</h1>
      <div style={{ marginBottom: 16 }}>
        <button className="btn" onClick={handleLogout}>
          로그아웃
        </button>
      </div>
      <ul>
        <li>
          <Link to="/admin/feedback">피드백 관리</Link>
        </li>
        <li>
          <Link to="/admin/notification">공지사항 관리</Link>
        </li>
      </ul>
    </div>
  );
}

export default AdminMainPage;

