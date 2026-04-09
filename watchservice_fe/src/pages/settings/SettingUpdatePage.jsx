/**
 * 파일 이름 : SettingUpdatePage.jsx
 * 기능 : 버전/업데이트 정보 페이지. 현재 버전을 표시하고 GitHub Releases에서 최신 버전을 확인한다.
 * 작성 날짜 : 2025/12/17
 * 작성자 : 시스템
 */
import React, { useEffect, useState } from 'react';

function SettingUpdatePage() {
  const isElectron = typeof window !== 'undefined' && typeof window.electronAPI?.checkForUpdates === 'function';

  const [version, setVersion] = useState('...');
  const [checking, setChecking] = useState(false);
  const [result, setResult] = useState(null); // { latestVersion, downloadUrl, hasUpdate, error }

  useEffect(() => {
    if (isElectron) {
      window.electronAPI.getAppVersion().then((v) => setVersion(v));
    } else {
      setVersion('1.0.0');
    }
  }, [isElectron]);

  const handleCheck = async () => {
    setChecking(true);
    setResult(null);
    try {
      const res = await window.electronAPI.checkForUpdates();
      setResult(res);
    } finally {
      setChecking(false);
    }
  };

  const statusBox = () => {
    if (!result) return null;
    if (result.error) {
      return (
        <div style={{ marginTop: 12, padding: '10px 14px', background: '#fef2f2', border: '1px solid #fca5a5', borderRadius: 8, fontSize: 13, color: '#b91c1c' }}>
          업데이트 확인 실패: {result.error}
        </div>
      );
    }
    if (result.hasUpdate) {
      return (
        <div style={{ marginTop: 12, padding: '10px 14px', background: '#fffbeb', border: '1px solid #fcd34d', borderRadius: 8, fontSize: 13, color: '#92400e' }}>
          새 버전 <strong>{result.latestVersion}</strong> 이 있습니다.{' '}
          {result.downloadUrl && (
            <button
              onClick={() => window.open(result.downloadUrl, '_blank')}
              style={{ marginLeft: 8, padding: '2px 10px', background: '#f59e0b', color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 13 }}
            >
              다운로드 페이지 열기
            </button>
          )}
        </div>
      );
    }
    return (
      <div style={{ marginTop: 12, padding: '10px 14px', background: '#f0fdf4', border: '1px solid #86efac', borderRadius: 8, fontSize: 13, color: '#166534' }}>
        현재 최신 버전입니다. ({result.latestVersion ?? version})
      </div>
    );
  };

  return (
    <div className="page-container">
      <h1>버전 / 업데이트 정보</h1>
      <p style={{ fontSize: 14, color: '#9ca3af', marginBottom: 16 }}>
        WatchService Agent의 버전 정보를 확인합니다.
      </p>

      <div className="card" style={{ padding: 16 }}>
        <div className="card-row">
          <span className="card-label">현재 버전</span>
          <span className="card-value">{version}</span>
        </div>

        {isElectron ? (
          <>
            <div style={{ marginTop: 12 }}>
              <button
                onClick={handleCheck}
                disabled={checking}
                style={{
                  padding: '7px 18px',
                  background: checking ? '#e5e7eb' : '#2563eb',
                  color: checking ? '#9ca3af' : '#fff',
                  border: 'none',
                  borderRadius: 8,
                  cursor: checking ? 'not-allowed' : 'pointer',
                  fontSize: 14,
                }}
              >
                {checking ? '확인 중...' : '업데이트 확인'}
              </button>
            </div>
            {statusBox()}
          </>
        ) : (
          <div style={{ marginTop: 10, fontSize: 13, color: '#6b7280' }}>
            업데이트 확인은 Electron 앱에서만 지원됩니다.
            GitHub Releases 페이지에서 직접 확인해 주세요.
          </div>
        )}
      </div>
    </div>
  );
}

export default SettingUpdatePage;
