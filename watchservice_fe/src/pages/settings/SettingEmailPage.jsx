import React, { useEffect, useState } from 'react';
import { fetchAlertEmail, updateAlertEmail, sendTestEmail } from '../../api/SettingApi';

function SettingEmailPage() {
  const [email, setEmail] = useState('');
  const [savedEmail, setSavedEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [note, setNote] = useState('');
  const [noteColor, setNoteColor] = useState('#6b7280');

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
        setNote('이메일 설정을 불러오지 못했습니다.');
        setNoteColor('#ef4444');
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => { mounted = false; };
  }, []);

  const handleSave = async () => {
    setLoading(true);
    setNote('');
    try {
      await updateAlertEmail(email.trim());
      setSavedEmail(email.trim());
      setNote('저장 완료.');
      setNoteColor('#16a34a');
    } catch {
      setNote('저장에 실패했습니다.');
      setNoteColor('#ef4444');
    } finally {
      setLoading(false);
    }
  };

  const handleTest = async () => {
    const target = email.trim();
    if (!target) {
      setNote('이메일 주소를 먼저 입력해 주세요.');
      setNoteColor('#ef4444');
      return;
    }
    setLoading(true);
    setNote('');
    try {
      await sendTestEmail(target);
      setNote(`테스트 메일을 ${target} 로 발송했습니다. 메일함을 확인해 주세요.`);
      setNoteColor('#2563eb');
    } catch (e) {
      setNote('테스트 메일 발송 실패. SMTP 설정(환경변수)을 확인해 주세요.');
      setNoteColor('#ef4444');
    } finally {
      setLoading(false);
    }
  };

  const isDirty = email.trim() !== savedEmail;

  return (
    <div className="page-container">
      <h1>이메일 알림 설정</h1>
      <p style={{ fontSize: 14, color: '#9ca3af', marginBottom: 20 }}>
        DANGER 탐지 시 경보 메일을 수신할 이메일 주소를 설정합니다.
        비워 두면 이메일 알림이 비활성화됩니다.
      </p>

      <div style={{ maxWidth: 480, display: 'flex', flexDirection: 'column', gap: 12 }}>
        <label style={{ fontSize: 13, color: '#374151', fontWeight: 600 }}>
          수신 이메일 주소
        </label>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="example@gmail.com"
          disabled={loading}
          style={{
            padding: '8px 12px',
            border: '1px solid #d1d5db',
            borderRadius: 8,
            fontSize: 14,
            outline: 'none',
          }}
        />

        <div style={{ display: 'flex', gap: 8 }}>
          <button
            className="btn"
            onClick={handleSave}
            disabled={loading || !isDirty}
            style={{ flex: 1 }}
          >
            {loading ? '처리 중...' : '저장'}
          </button>
          <button
            className="btn"
            onClick={handleTest}
            disabled={loading}
            style={{ flex: 1, background: '#eff6ff', color: '#2563eb', border: '1px solid #bfdbfe' }}
          >
            테스트 메일 발송
          </button>
        </div>

        {note && (
          <p style={{ fontSize: 13, color: noteColor, margin: 0 }}>{note}</p>
        )}

        <div style={{
          marginTop: 8,
          padding: 12,
          background: '#f9fafb',
          border: '1px solid #e5e7eb',
          borderRadius: 8,
          fontSize: 13,
          color: '#6b7280',
          lineHeight: 1.6,
        }}>
          <strong style={{ color: '#374151' }}>SMTP 설정 필요</strong><br />
          이메일 발송을 위해 다음 환경변수를 설정해야 합니다.<br />
          <code style={{ fontSize: 12 }}>SMTP_HOST</code>,{' '}
          <code style={{ fontSize: 12 }}>SMTP_PORT</code>,{' '}
          <code style={{ fontSize: 12 }}>SMTP_USERNAME</code>,{' '}
          <code style={{ fontSize: 12 }}>SMTP_PASSWORD</code><br />
          Gmail 사용 시: 호스트 <code style={{ fontSize: 12 }}>smtp.gmail.com</code>,
          포트 <code style={{ fontSize: 12 }}>587</code>, 앱 비밀번호 사용 권장.
        </div>
      </div>
    </div>
  );
}

export default SettingEmailPage;
