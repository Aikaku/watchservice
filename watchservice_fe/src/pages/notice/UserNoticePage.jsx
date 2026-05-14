/**
 * 파일 이름 : UserNoticePage.jsx
 * 기능 : 사용자 공지사항 페이지. 공지 목록을 읽기 전용으로 표시한다.
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
import React, { useEffect, useState } from 'react';
import { fetchNotices } from '../../api/AdminApi';

/*
 * 함수 이름 : UserNoticePage
 * 기능 : 사용자 공지사항 페이지 컴포넌트. 공지사항 목록을 읽기 전용으로 조회하여 표시한다.
 * 매개변수 : 없음
 * 반환값 : JSX.Element
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
function UserNoticePage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  /*
   * 함수 이름 : load
   * 기능 : 서버에서 공지사항 목록을 불러온다.
   * 매개변수 : 없음
   * 반환값 : 없음
   * 작성 날짜 : 2026/03/08
   * 작성자 : 시스템
   */
  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchNotices();
      setItems(Array.isArray(data) ? data : []);
    } catch (e) {
      console.error(e);
      setError(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <div className="page-container">
      <h1>공지사항</h1>
      {loading && <p>불러오는 중...</p>}
      {error && <p style={{ color: 'red' }}>로드 오류: {error.message}</p>}
      {!loading && !error && items.length === 0 && <p>등록된 공지사항이 없습니다.</p>}

      {!loading && !error && items.length > 0 && (
        <div>
          {items.map((n) => (
            <div
              key={n.id}
              style={{
                border: '1px solid #1f2933',
                borderRadius: 8,
                padding: 12,
                marginBottom: 8,
                background: '#020617',
              }}
            >
              <h3 style={{ marginBottom: 4 }}>{n.title || '(제목 없음)'}</h3>
              <div style={{ fontSize: 12, color: '#9ca3af', marginBottom: 8 }}>{n.createdAt}</div>
              <pre style={{ whiteSpace: 'pre-wrap', fontFamily: 'inherit', fontSize: 13 }}>
                {n.content}
              </pre>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default UserNoticePage;

