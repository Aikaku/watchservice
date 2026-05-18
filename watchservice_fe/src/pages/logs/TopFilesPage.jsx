/**
 * 파일 이름 : TopFilesPage.jsx
 * 기능 : 자주 변경되는 파일 TOP 10 페이지. 로그 기반으로 이벤트 발생 횟수가 많은 파일 순으로 표시한다.
 * 작성 날짜 : 2026/04/06
 * 작성자 : 시스템
 */
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchTopFiles } from '../../api/LogsApi';

/*
 * 함수 이름 : TopFilesPage
 * 기능 : 자주 변경되는 파일 TOP 10 페이지 컴포넌트. 로그 기반으로 이벤트 발생 횟수가 많은 파일 순으로 표시한다.
 * 매개변수 : 없음
 * 반환값 : JSX.Element
 * 작성 날짜 : 2026/04/06
 * 작성자 : 이상혁
 */
function TopFilesPage({ embedded = false }) {
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let mounted = true;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchTopFiles(10);
        if (mounted) setItems(Array.isArray(data) ? data : []);
      } catch (e) {
        if (mounted) setError(e.message);
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => { mounted = false; };
  }, []);

  const maxCount = items.length > 0 ? items[0].changeCount : 1;

  return (
    <div className={embedded ? undefined : 'page-container'}>
      {!embedded && <h1>자주 변경되는 파일 TOP 10</h1>}
      {!embedded && (
        <p style={{ color: '#9ca3af', marginBottom: 24 }}>
          누적 이벤트(CREATE / MODIFY / DELETE 등) 횟수 기준
        </p>
      )}
      {!embedded && (
        <button className="btn" style={{ marginBottom: 24 }} onClick={() => navigate('/logs')}>
          로그 목록으로 돌아가기
        </button>
      )}

      {loading && <p>불러오는 중...</p>}
      {error && <p style={{ color: '#f87171' }}>오류: {error}</p>}

      {!loading && !error && items.length === 0 && (
        <p style={{ color: '#9ca3af' }}>데이터가 없습니다.</p>
      )}

      {!loading && !error && items.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {items.map((item, idx) => {
            const barWidth = Math.max(4, Math.round((item.changeCount / maxCount) * 100));
            return (
              <div key={idx} style={{ background: '#1f2937', borderRadius: 8, padding: '12px 16px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                  <span style={{ fontSize: 13, fontFamily: 'monospace', color: '#e5e7eb', wordBreak: 'break-all' }}>
                    <span style={{ color: '#6b7280', marginRight: 8 }}>#{idx + 1}</span>
                    {item.path}
                  </span>
                  <span style={{ fontSize: 13, fontWeight: 700, color: '#60a5fa', whiteSpace: 'nowrap', marginLeft: 16 }}>
                    {item.changeCount}회
                  </span>
                </div>
                <div style={{ background: '#374151', borderRadius: 4, height: 6 }}>
                  <div
                    style={{
                      width: `${barWidth}%`,
                      height: '100%',
                      borderRadius: 4,
                      background: idx === 0 ? '#ef4444' : idx < 3 ? '#f97316' : '#3b82f6',
                    }}
                  />
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default TopFilesPage;
