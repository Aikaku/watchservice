/**
 * 파일 이름 : SettingGuidePage.jsx
 * 기능 : 사용 가이드 조회 페이지. 관리자가 작성한 가이드 내용을 사용자에게 표시한다.
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
import React, { useEffect, useState } from 'react';
import { fetchGuide } from '../../api/AdminApi';

/**
 * 함수 이름 : SettingGuidePage
 * 기능 : 사용 가이드 조회 페이지 컴포넌트.
 * 매개변수 : 없음
 * 반환값 : JSX.Element
 */
function SettingGuidePage() {
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchGuide()
      .then((data) => setContent(data.content || ''))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="page-container">
      <h1>사용 가이드</h1>
      <p style={{ fontSize: 14, color: '#9ca3af', marginBottom: 20 }}>
        WatchService Agent 사용 방법을 안내합니다.
      </p>

      {loading && <div style={{ color: '#9ca3af', fontSize: 14 }}>불러오는 중...</div>}
      {error && <div style={{ color: '#f87171', fontSize: 14 }}>오류: {error}</div>}

      {!loading && !error && (
        <div
          style={{
            background: '#1f2937',
            border: '1px solid #374151',
            borderRadius: 8,
            padding: '20px 24px',
            fontSize: 14,
            color: '#e5e7eb',
            lineHeight: 1.8,
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word',
          }}
        >
          {content}
        </div>
      )}
    </div>
  );
}

export default SettingGuidePage;
