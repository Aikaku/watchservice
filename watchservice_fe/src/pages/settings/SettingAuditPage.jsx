import React, { useEffect, useState } from 'react';
import { fetchAuditResults, runAudit } from '../../api/SettingApi';
import { useToast } from '../../components/common/Toast';

function SettingAuditPage() {
  const { showToast } = useToast();
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [lastRunAt, setLastRunAt] = useState('');
  const [loading, setLoading] = useState(false);
  const [running, setRunning] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const res = await fetchAuditResults();
      setItems(res?.items || []);
      setTotal(res?.total ?? 0);
      setLastRunAt(res?.lastRunAt || '');
    } catch (e) {
      showToast('감사 결과 불러오기 실패: ' + e.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleRun = async () => {
    setRunning(true);
    try {
      const res = await runAudit();
      showToast(`감사 완료. 위험 파일 ${res?.found ?? 0}건 발견.`, res?.found > 0 ? 'error' : 'success');
      await load();
    } catch (e) {
      showToast('감사 실행 실패: ' + e.message, 'error');
    } finally {
      setRunning(false);
    }
  };

  return (
    <div className="page-container">
      <h1>파일 권한 보안 감사</h1>
      <p className="page-description">
        감시 폴더 내 <strong style={{ color: '#e2e8f0' }}>others</strong>(모든 사용자)에게
        쓰기·실행 권한이 부여된 파일을 탐지합니다.
        자동 감사는 매 6시간마다 실행됩니다. macOS/Linux 전용.
      </p>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        {/* 액션 바 */}
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          <button className="btn" onClick={handleRun} disabled={running || loading}>
            {running ? '감사 중...' : '지금 감사 실행'}
          </button>
          <button className="btn btn-outline" onClick={load} disabled={loading}>
            새로고침
          </button>
          {lastRunAt && (
            <span style={{ fontSize: 12, color: '#64748b' }}>마지막 감사: {lastRunAt}</span>
          )}
        </div>

        {/* 결과 상태 */}
        {!loading && total === 0 && (
          <div className="card" style={{ borderColor: 'rgba(74,222,128,0.3)', textAlign: 'center', padding: 24 }}>
            <div style={{ fontSize: 22, marginBottom: 8 }}>✅</div>
            <p style={{ color: '#4ade80', margin: 0, fontSize: 14, fontWeight: 600 }}>
              위험 권한을 가진 파일이 없습니다.
            </p>
          </div>
        )}

        {!loading && total > 0 && (
          <>
            <div className="card" style={{ borderColor: 'rgba(239,68,68,0.3)' }}>
              <p style={{ color: '#f87171', margin: 0, fontSize: 14 }}>
                ⚠️ 위험 파일 <strong>{total}</strong>건 발견됨
              </p>
            </div>

            <div className="log-table-wrapper">
              <table className="log-table">
                <thead>
                  <tr>
                    <th>파일 경로</th>
                    <th style={{ whiteSpace: 'nowrap' }}>권한</th>
                    <th style={{ whiteSpace: 'nowrap' }}>문제</th>
                    <th style={{ whiteSpace: 'nowrap' }}>감사 시각</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((item, i) => (
                    <tr key={i}>
                      <td style={{ fontFamily: 'monospace', wordBreak: 'break-all', fontSize: 12 }}>
                        {item.filePath}
                      </td>
                      <td style={{ fontFamily: 'monospace', fontSize: 12 }}>{item.permissions}</td>
                      <td style={{ color: '#f87171', fontSize: 12 }}>{item.issue}</td>
                      <td style={{ whiteSpace: 'nowrap', color: '#64748b', fontSize: 12 }}>{item.auditedAt}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}

        {loading && (
          <p style={{ color: '#9ca3af', fontSize: 14 }}>불러오는 중...</p>
        )}
      </div>
    </div>
  );
}

export default SettingAuditPage;
