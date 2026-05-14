/**
 * 파일 이름 : AdminMainPage.jsx
 * 기능 : 관리자 메인 페이지. 피드백/공지 관리 페이지로 진입하는 메뉴를 제공한다.
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
import React from 'react';
import { Link } from 'react-router-dom';
import { adminLogout } from '../../api/AdminApi';
import { useToast } from '../../components/common/Toast';

/*
 * 함수 이름 : AdminMainPage
 * 기능 : 관리자 메인 페이지 컴포넌트. 각 관리 메뉴로 이동하는 링크와 로그아웃 기능을 제공한다.
 * 매개변수 : 없음
 * 반환값 : JSX.Element
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
function AdminMainPage() {
  const toast = useToast();

  /*
   * 함수 이름 : handleLogout
   * 기능 : 관리자 로그아웃을 처리하고 로그인 페이지로 이동한다.
   * 매개변수 : 없음
   * 반환값 : 없음
   * 작성 날짜 : 2026/03/08
   * 작성자 : 시스템
   */
  const handleLogout = async () => {
    try {
      await adminLogout();
      window.location.href = '/admin/login';
    } catch (e) {
      console.error(e);
      toast('로그아웃 중 오류가 발생했습니다.', 'error');
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
        <li>
          <Link to="/admin/logs">로그 관리</Link>
        </li>
        <li>
          <Link to="/admin/alerts">알림/탐지 관리</Link>
        </li>
        <li>
          <Link to="/admin/sessions">사용자 세션 관리</Link>
        </li>
        <li>
          <Link to="/admin/system">시스템 상태 모니터링</Link>
        </li>
        <li>
          <Link to="/admin/guide">사용 가이드 편집</Link>
        </li>
      </ul>
    </div>
  );
}

export default AdminMainPage;

