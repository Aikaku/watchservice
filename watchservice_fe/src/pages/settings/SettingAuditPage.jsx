/**
 * 파일 이름 : SettingAuditPage.jsx
 * 기능 : 파일 권한 보안 감사 페이지. others 쓰기·실행 권한이 있는 파일 목록을 표시한다.
 * 작성 날짜 : 2026/04/24
 * 작성자 : 시스템
 */
import React, { useEffect, useState } from 'react';
import { fetchAuditResults, runAudit } from '../../api/SettingApi';
import { useToast } from '../../components/common/Toast';

/**
 * 함수 이름 : SettingAuditPage
 * 기능 : 파일 권한 보안 감사 페이지 컴포넌트.
 * 매개변수 : 없음
 * 반환값 : JSX.Element
 * 작성 날짜 : 2026/04/24
 * 작성자 : 시스템
 */
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
      <p style={{ fontSize: 13, color: '#9ca3af', marginBottom: 16 }}>
        감시 폴더 내 <strong>others</strong>(모든 사용자)에게 쓰기·실행 권한이 부여된 파일을 탐지합니다.
        자동 감사는 매 6시간마다 실행됩니다. macOS/Linux 전용입니다.
      </p>

      <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginBottom: 16 }}>
        <button className="btn" onClick={handleRun} disabled={running || loading}>
          {running ? '감사 중...' : '지금 감사 실행'}
        </button>
        <button className="btn" onClick={load} disabled={loading}>
          새로고침
        </button>
        {lastRunAt && (
          <span style={{ fontSize: 12, color: '#9ca3af' }}>
            마지막 감사: {lastRunAt}
          </span>
        )}
      </div>

      {loading && <p>불러오는 중...</p>}

      {!loading && total === 0 && (
        <p style={{ color: '#4ade80' }}>위험 권한을 가진 파일이 없습니다.</p>
      )}

      {!loading && total > 0 && (
        <>
          <p style={{ color: '#f87171', marginBottom: 10 }}>
            위험 파일 <strong>{total}</strong>건 발견됨
          </p>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
              <thead>
                <tr style={{ background: '#374151', textAlign: 'left' }}>
                  <th style={{ padding: '8px 12px' }}>파일 경로</th>
                  <th style={{ padding: '8px 12px', whiteSpace: 'nowrap' }}>권한</th>
                  <th style={{ padding: '8px 12px', whiteSpace: 'nowrap' }}>문제</th>
                  <th style={{ padding: '8px 12px', whiteSpace: 'nowrap' }}>감사 시각</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item, i) => (
                  <tr
                    key={i}
                    style={{
                      borderBottom: '1px solid #374151',
                      background: i % 2 === 0 ? '#111827' : '#1f2937',
                    }}
                  >
                    <td style={{ padding: '7px 12px', fontFamily: 'monospace', wordBreak: 'break-all' }}>
                      {item.filePath}
                    </td>
                    <td style={{ padding: '7px 12px', fontFamily: 'monospace' }}>
                      {item.permissions}
                    </td>
                    <td style={{ padding: '7px 12px', color: '#f87171' }}>
                      {item.issue}
                    </td>
                    <td style={{ padding: '7px 12px', whiteSpace: 'nowrap', color: '#9ca3af' }}>
                      {item.auditedAt}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}

export default SettingAuditPage;
