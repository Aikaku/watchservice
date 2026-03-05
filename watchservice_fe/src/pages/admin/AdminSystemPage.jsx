/**
 * 파일 이름 : AdminSystemPage.jsx
 * 기능 : 관리자 전용 시스템 상태 모니터링 페이지. DB 크기, 레코드 수, AI 서버·Watcher 상태를 표시한다.
 */
import React, { useState, useEffect } from 'react';
import { fetchAdminSystem } from '../../api/AdminApi';

function AdminSystemPage() {
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetchAdminSystem();
      setStatus(res);
    } catch (e) {
      setError(e.message || '오류');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const cardStyle = {
    background: 'rgba(30,41,59,0.8)',
    border: '1px solid rgba(59,130,246,0.2)',
    borderRadius: 12,
    padding: '20px 24px',
    minWidth: 180,
    flex: '1 1 180px',
  };

  const labelStyle = { fontSize: 13, color: '#9ca3af', marginBottom: 6 };
  const valueStyle = { fontSize: 24, fontWeight: 'bold', color: '#e5e7eb' };

  return (
    <div className="page-container">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <h1>시스템 상태 모니터링</h1>
          <p style={{ fontSize: 13, color: '#9ca3af', marginTop: 4 }}>
            DB 크기, 레코드 수, AI 서버 및 Watcher 상태를 실시간으로 확인합니다.
          </p>
        </div>
        <button className="btn" onClick={load}>새로고침</button>
      </div>

      {loading && <p>불러오는 중...</p>}
      {error && <p style={{ color: 'red' }}>오류: {error}</p>}

      {!loading && !error && status && (
        <>
          {/* 서버 상태 카드 */}
          <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 24 }}>
            <div style={cardStyle}>
              <div style={labelStyle}>AI 서버 상태</div>
              <div style={{ ...valueStyle, color: status.aiServerStatus === 'UP' ? '#22c55e' : '#ef4444' }}>
                {status.aiServerStatus || '-'}
              </div>
            </div>
            <div style={cardStyle}>
              <div style={labelStyle}>Watcher 상태</div>
              <div style={{ ...valueStyle, color: status.watcherRunning ? '#22c55e' : '#9ca3af' }}>
                {status.watcherRunning ? '실행 중' : '중지됨'}
              </div>
            </div>
            <div style={cardStyle}>
              <div style={labelStyle}>DB 크기</div>
              <div style={valueStyle}>{status.dbSizeMb} MB</div>
              <div style={{ fontSize: 11, color: '#9ca3af', marginTop: 4 }}>
                {status.dbSizeBytes?.toLocaleString()} bytes
              </div>
            </div>
          </div>

          {/* 레코드 수 카드 */}
          <h2 style={{ marginBottom: 12 }}>데이터 현황</h2>
          <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
            <div style={cardStyle}>
              <div style={labelStyle}>총 로그 수</div>
              <div style={valueStyle}>{status.totalLogs?.toLocaleString()}</div>
            </div>
            <div style={cardStyle}>
              <div style={labelStyle}>총 알림 수</div>
              <div style={valueStyle}>{status.totalAlerts?.toLocaleString()}</div>
            </div>
            <div style={cardStyle}>
              <div style={labelStyle}>등록 에이전트 수</div>
              <div style={valueStyle}>{status.totalSessions?.toLocaleString()}</div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

export default AdminSystemPage;
