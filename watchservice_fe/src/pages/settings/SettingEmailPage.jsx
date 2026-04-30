import React, { useEffect, useState } from 'react';
import { fetchAlertEmail, updateAlertEmail, sendTestEmail } from '../../api/SettingApi';

function SettingEmailPage() {
  const [email, setEmail] = useState('');
  const [savedEmail, setSavedEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [note, setNote] = useState({ text: '', type: '' }); // type: success | error | info

  useEffect(() => {
    let mounted = true;
    (async () => {
      setLoading(true);
      try {
        const data = await fetchAlertEmail();
        if (!mounted) return;
        const v = data?.email ?? '';
        setEmail(v);
        setSavedEmail(v);
      } catch {
        setNote({ text: '이메일 설정을 불러오지 못했습니다.', type: 'error' });
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => { mounted = false; };
  }, []);

  const handleSave = async () => {
    setLoading(true);
    setNote({ text: '', type: '' });
    try {
      await updateAlertEmail(email.trim());
      setSavedEmail(email.trim());
      setNote({ text: '저장 완료.', type: 'success' });
    } catch {
      setNote({ text: '저장에 실패했습니다.', type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleTest = async () => {
    const target = email.trim();
    if (!target) {
      setNote({ text: '이메일 주소를 먼저 입력해 주세요.', type: 'error' });
      return;
    }
    setLoading(true);
    setNote({ text: '', type: '' });
    try {
      await sendTestEmail(target);
      setNote({ text: `테스트 메일을 ${target} 로 발송했습니다.`, type: 'info' });
    } catch {
      setNote({ text: '테스트 메일 발송 실패. SMTP 설정을 확인해 주세요.', type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const noteColor = { success: '#4ade80', error: '#f87171', info: '#60a5fa' }[note.type] || '#9ca3af';
  const isDirty = email.trim() !== savedEmail;

  return (
    <div className="page-container">
      <h1>이메일 알림 설정</h1>
      <p className="page-description">
        DANGER 탐지 시 경보 메일을 수신할 이메일 주소를 설정합니다.
        비워 두면 이메일 알림이 비활성화됩니다.
      </p>

      <div style={{ maxWidth: 520, display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <label>
            수신 이메일 주소
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="example@gmail.com"
              disabled={loading}
              style={{ marginTop: 6 }}
            />
          </label>

          <div style={{ display: 'flex', gap: 10 }}>
            <button className="btn" onClick={handleSave} disabled={loading || !isDirty} style={{ flex: 1 }}>
              {loading ? '처리 중...' : '저장'}
            </button>
            <button className="btn btn-outline" onClick={handleTest} disabled={loading} style={{ flex: 1 }}>
              테스트 발송
            </button>
          </div>

          {note.text && (
            <p style={{ fontSize: 13, color: noteColor, margin: 0 }}>{note.text}</p>
          )}
        </div>

        <div className="card" style={{ borderColor: 'rgba(59,130,246,0.25)' }}>
          <div style={{ fontSize: 13, fontWeight: 600, color: '#93c5fd', marginBottom: 10 }}>
            SMTP 설정 안내
          </div>
          <div style={{ fontSize: 13, color: '#94a3b8', lineHeight: 1.7 }}>
            이메일 발송을 위해 서버에 다음 환경변수를 설정해야 합니다.<br />
            <code style={{ color: '#e2e8f0', background: 'rgba(15,23,42,0.6)', padding: '1px 5px', borderRadius: 4 }}>SMTP_HOST</code>
            {' · '}
            <code style={{ color: '#e2e8f0', background: 'rgba(15,23,42,0.6)', padding: '1px 5px', borderRadius: 4 }}>SMTP_PORT</code>
            {' · '}
            <code style={{ color: '#e2e8f0', background: 'rgba(15,23,42,0.6)', padding: '1px 5px', borderRadius: 4 }}>SMTP_USERNAME</code>
            {' · '}
            <code style={{ color: '#e2e8f0', background: 'rgba(15,23,42,0.6)', padding: '1px 5px', borderRadius: 4 }}>SMTP_PASSWORD</code>
            <br />
            Gmail: 호스트 <code style={{ color: '#e2e8f0', background: 'rgba(15,23,42,0.6)', padding: '1px 5px', borderRadius: 4 }}>smtp.gmail.com</code>,
            포트 <code style={{ color: '#e2e8f0', background: 'rgba(15,23,42,0.6)', padding: '1px 5px', borderRadius: 4 }}>587</code>, 앱 비밀번호 사용 권장
          </div>
        </div>
      </div>
    </div>
  );
}

export default SettingEmailPage;
