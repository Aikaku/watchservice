/**
 * 파일 이름 : AdminSessionPage.jsx
 * 기능 : 관리자 전용 세션(에이전트) 현황 페이지. 등록된 owner_key별 통계를 표시한다.
 */
import React, { useState, useEffect } from 'react';
import { fetchAdminSessions } from '../../api/AdminApi';

function AdminSessionPage() {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetchAdminSessions();
      setSessions(Array.isArray(res) ? res : []);
    } catch (e) {
      setError(e.message || '오류');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  return (
    <div className="page-container">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <div>
          <h1>사용자 세션 관리</h1>
          <p style={{ fontSize: 13, color: '#9ca3af', marginTop: 4 }}>
            등록된 에이전트(owner_key)별 활동 현황을 확인합니다.
          </p>
        </div>
        <button className="btn" onClick={load}>새로고침</button>
      </div>

      {loading && <p>불러오는 중...</p>}
      {error && <p style={{ color: 'red' }}>오류: {error}</p>}

      {!loading && !error && (
        <>
          <p style={{ marginBottom: 8, color: '#9ca3af', fontSize: 13 }}>
            총 에이전트 수: <strong style={{ color: '#e5e7eb' }}>{sessions.length}</strong>
          </p>
          <table className="table" style={{ width: '100%' }}>
            <thead>
              <tr>
                <th>#</th>
                <th>Owner Key</th>
                <th>로그 수</th>
                <th>알림 수</th>
                <th>감시 폴더</th>
                <th>예외 규칙</th>
              </tr>
            </thead>
            <tbody>
              {sessions.length === 0 && (
                <tr><td colSpan={6} style={{ textAlign: 'center', color: '#9ca3af' }}>등록된 세션 없음</td></tr>
              )}
              {sessions.map((s, idx) => (
                <tr key={s.ownerKey}>
                  <td>{idx + 1}</td>
                  <td style={{ fontFamily: 'monospace', fontSize: 12 }}>{s.ownerKey}</td>
                  <td>{s.logCount?.toLocaleString()}</td>
                  <td>
                    <span style={{ color: s.alertCount > 0 ? '#ef4444' : '#9ca3af' }}>
                      {s.alertCount?.toLocaleString()}
                    </span>
                  </td>
                  <td>{s.folderCount}</td>
                  <td>{s.exceptionCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </div>
  );
}

export default AdminSessionPage;
