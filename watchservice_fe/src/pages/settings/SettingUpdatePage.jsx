import React, { useEffect, useState } from 'react';

/*
 * 함수 이름 : SettingUpdatePage
 * 기능 : 버전/업데이트 정보 페이지 컴포넌트. 현재 앱 버전을 표시하고 업데이트 여부를 확인한다.
 * 매개변수 : 없음
 * 반환값 : JSX.Element
 * 작성 날짜 : 2026/03/08
 * 작성자 : 이상혁
 */
function SettingUpdatePage() {
  const isElectron = typeof window !== 'undefined' && typeof window.electronAPI?.checkForUpdates === 'function';

  const [version, setVersion] = useState('...');
  const [checking, setChecking] = useState(false);
  const [result, setResult] = useState(null);

  useEffect(() => {
    if (isElectron) {
      window.electronAPI.getAppVersion().then((v) => setVersion(v));
    } else {
      setVersion('1.0.0');
    }
  }, [isElectron]);

  /*
   * 함수 이름 : handleCheck
   * 기능 : 업데이트 확인 버튼 클릭 시 Electron API를 통해 최신 버전 여부를 확인한다.
   * 매개변수 : 없음
   * 반환값 : 없음
   * 작성 날짜 : 2026/03/08
   * 작성자 : 이상혁
   */
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

  /*
   * 함수 이름 : renderResult
   * 기능 : 업데이트 확인 결과에 따라 적절한 안내 카드를 렌더링하여 반환한다.
   * 매개변수 : 없음
   * 반환값 : JSX.Element | null
   * 작성 날짜 : 2026/03/08
   * 작성자 : 이상혁
   */
  const renderResult = () => {
    if (!result) return null;

    if (result.error) {
      return (
        <div className="card" style={{ borderColor: 'rgba(239,68,68,0.3)' }}>
          <div style={{ fontSize: 13, color: '#f87171' }}>
            업데이트 확인 실패: {result.error}
          </div>
        </div>
      );
    }

    if (result.hasUpdate) {
      return (
        <div className="card" style={{ borderColor: 'rgba(234,179,8,0.3)' }}>
          <div style={{ fontSize: 13, color: '#fbbf24', marginBottom: 10, fontWeight: 600 }}>
            새 버전 {result.latestVersion} 이 있습니다
          </div>
          {result.downloadUrl && (
            <button className="btn" onClick={() => window.open(result.downloadUrl, '_blank')}>
              다운로드 페이지 열기
            </button>
          )}
        </div>
      );
    }

    return (
      <div className="card" style={{ borderColor: 'rgba(74,222,128,0.3)' }}>
        <div style={{ fontSize: 13, color: '#4ade80' }}>
          최신 버전입니다. ({result.latestVersion ?? version})
        </div>
      </div>
    );
  };

  return (
    <div className="page-container">
      <h1>버전 / 업데이트 정보</h1>
      <p className="page-description">WatchService Agent의 버전 정보를 확인합니다.</p>

      <div style={{ maxWidth: 480, display: 'flex', flexDirection: 'column', gap: 14 }}>
        <div className="card">
          <div className="card-row">
            <span className="card-label">현재 버전</span>
            <span className="card-value" style={{ fontFamily: 'monospace' }}>v{version}</span>
          </div>
        </div>

        {isElectron ? (
          <>
            <button className="btn btn-outline" onClick={handleCheck} disabled={checking}>
              {checking ? '확인 중...' : '업데이트 확인'}
            </button>
            {renderResult()}
          </>
        ) : (
          <div className="card">
            <p style={{ fontSize: 13, color: '#94a3b8', margin: 0 }}>
              업데이트 확인은 Electron 앱에서만 지원됩니다.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

export default SettingUpdatePage;
