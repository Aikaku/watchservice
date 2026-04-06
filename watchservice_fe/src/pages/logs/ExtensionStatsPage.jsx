/**
 * 파일 이름 : ExtensionStatsPage.jsx
 * 기능 : 파일 확장자 분포 차트 페이지. 로그에 기록된 파일 확장자별 빈도를 가로 막대 차트로 표시한다.
 * 작성 날짜 : 2026/04/06
 * 작성자 : 시스템
 */
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchExtensionStats } from '../../api/LogsApi';

const BAR_COLORS = [
  '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6',
  '#06b6d4', '#f97316', '#ec4899', '#84cc16', '#14b8a6',
  '#6366f1', '#a78bfa', '#fb923c', '#34d399', '#fbbf24',
  '#f472b6', '#4ade80', '#38bdf8', '#e879f9', '#a3e635',
];

function ExtensionStatsPage() {
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
        const data = await fetchExtensionStats(20);
        if (mounted) setItems(Array.isArray(data) ? data : []);
      } catch (e) {
        if (mounted) setError(e.message);
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => { mounted = false; };
  }, []);

  const total = items.reduce((s, i) => s + i.count, 0);
  const maxCount = items.length > 0 ? items[0].count : 1;

  return (
    <div className="page-container">
      <h1>파일 확장자 분포</h1>
      <p style={{ color: '#9ca3af', marginBottom: 24 }}>
        감시 로그에 기록된 파일 확장자별 이벤트 횟수 (상위 20개)
      </p>

      <button className="btn" style={{ marginBottom: 24 }} onClick={() => navigate('/logs')}>
        로그 목록으로 돌아가기
      </button>

      {loading && <p>불러오는 중...</p>}
      {error && <p style={{ color: '#f87171' }}>오류: {error}</p>}

      {!loading && !error && items.length === 0 && (
        <p style={{ color: '#9ca3af' }}>데이터가 없습니다.</p>
      )}

      {!loading && !error && items.length > 0 && (
        <>
          <p style={{ color: '#6b7280', fontSize: 13, marginBottom: 16 }}>
            총 이벤트 수: <strong style={{ color: '#e5e7eb' }}>{total.toLocaleString()}</strong>
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {items.map((item, idx) => {
              const barWidth = Math.max(2, Math.round((item.count / maxCount) * 100));
              const pct = total > 0 ? ((item.count / total) * 100).toFixed(1) : '0.0';
              const color = BAR_COLORS[idx % BAR_COLORS.length];
              return (
                <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ width: 80, textAlign: 'right', fontFamily: 'monospace', fontSize: 13, color: '#e5e7eb', flexShrink: 0 }}>
                    {item.ext}
                  </div>
                  <div style={{ flex: 1, background: '#1f2937', borderRadius: 4, height: 20 }}>
                    <div
                      style={{
                        width: `${barWidth}%`,
                        height: '100%',
                        borderRadius: 4,
                        background: color,
                        transition: 'width 0.3s',
                      }}
                    />
                  </div>
                  <div style={{ width: 100, fontSize: 12, color: '#9ca3af', flexShrink: 0 }}>
                    {item.count.toLocaleString()}건 ({pct}%)
                  </div>
                </div>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}

export default ExtensionStatsPage;
