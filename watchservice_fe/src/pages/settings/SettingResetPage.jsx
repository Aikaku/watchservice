import React, { useState } from 'react';
import { resetSettings } from '../../api/SettingApi';
import { useConfirm } from '../../components/common/ConfirmModal';

function SettingResetPage() {
  const confirm = useConfirm();
  const [loading, setLoading] = useState(false);
  const [note, setNote] = useState({ text: '', type: '' });

  const handleReset = async () => {
    const ok = await confirm('설정을 기본값으로 초기화할까요?');
    if (!ok) return;

    setLoading(true);
    setNote({ text: '', type: '' });

    try {
      localStorage.removeItem('watchservice.watchedFolders');
      localStorage.removeItem('watchservice.exceptions');
      localStorage.removeItem('watchservice.notifySettings');
    } catch { /* ignore */ }

    try {
      await resetSettings();
      setNote({ text: '초기화 완료.', type: 'success' });
    } catch {
      setNote({ text: '초기화 완료 (로컬 기준).', type: 'info' });
    } finally {
      setLoading(false);
    }
  };

  const noteColor = { success: '#4ade80', error: '#f87171', info: '#60a5fa' }[note.type] || '#9ca3af';

  return (
    <div className="page-container">
      <h1>초기화</h1>
      <p className="page-description">
        설정을 기본값으로 되돌립니다.
      </p>

      <div style={{ maxWidth: 520, display: 'flex', flexDirection: 'column', gap: 14 }}>
        <div className="card" style={{ borderColor: 'rgba(239,68,68,0.3)' }}>
          <div style={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: 12,
            marginBottom: 14,
          }}>
            <span style={{ fontSize: 20 }}>⚠️</span>
            <div>
              <div style={{ fontSize: 14, fontWeight: 700, color: '#fca5a5', marginBottom: 6 }}>
                주의 — 되돌릴 수 없는 작업
              </div>
              <div style={{ fontSize: 13, color: '#94a3b8', lineHeight: 1.6 }}>
                감시 폴더, 예외 규칙, 알림 설정 등 모든 로컬 설정이 삭제됩니다.
                필요한 경우 내보내기/백업 후 진행하세요.
              </div>
            </div>
          </div>

          <button className="btn btn-danger" onClick={handleReset} disabled={loading}>
            {loading ? '초기화 중...' : '초기화 실행'}
          </button>
        </div>

        {note.text && (
          <p style={{ fontSize: 13, color: noteColor, margin: 0 }}>{note.text}</p>
        )}
      </div>
    </div>
  );
}

export default SettingResetPage;
