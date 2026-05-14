import React, { useEffect, useState } from 'react';
import { fetchNotificationSettings, updateNotificationSettings } from '../../api/SettingApi';

const STORAGE_KEY = 'watchservice.notifySettings';

const isElectron = typeof window !== 'undefined' && !!window.electronAPI;

/*
 * 함수 이름 : SettingNotifyPage
 * 기능 : 알림 방식 설정 페이지 컴포넌트. 팝업·소리 알림 활성화 여부와 자동 실행 설정을 관리한다.
 * 매개변수 : 없음
 * 반환값 : JSX.Element
 * 작성 날짜 : 2026/03/08
 * 작성자 : 이상혁
 */
function SettingNotifyPage() {
  const [popup, setPopup] = useState(true);
  const [sound, setSound] = useState(false);
  const [autoLaunch, setAutoLaunch] = useState(false);
  const [loading, setLoading] = useState(false);
  const [note, setNote] = useState({ text: '', type: '' });

  useEffect(() => {
    let mounted = true;
    (async () => {
      setLoading(true);
      try {
        const s = await fetchNotificationSettings();
        if (!mounted) return;
        setPopup(!!s.popup);
        setSound(!!s.sound);
        localStorage.setItem(STORAGE_KEY, JSON.stringify({ popup: !!s.popup, sound: !!s.sound }));
        setNote({ text: '서버 설정을 불러왔습니다.', type: 'success' });
      } catch {
        try {
          const raw = localStorage.getItem(STORAGE_KEY);
          if (raw) {
            const v = JSON.parse(raw);
            setPopup(!!v.popup);
            setSound(!!v.sound);
            setNote({ text: '로컬 설정을 불러왔습니다.', type: 'info' });
          }
        } catch { /* ignore */ }
      } finally {
        if (mounted) setLoading(false);
      }
    })();

    if (isElectron) {
      window.electronAPI.getLoginItem().then((val) => {
        if (mounted) setAutoLaunch(!!val);
      });
    }

    return () => { mounted = false; };
  }, []);

  /*
   * 함수 이름 : handleAutoLaunchChange
   * 기능 : 자동 실행 체크박스 변경 시 Electron loginItem 설정을 갱신한다.
   * 매개변수 : e - 체크박스 변경 이벤트 객체
   * 반환값 : 없음
   * 작성 날짜 : 2026/03/08
   * 작성자 : 이상혁
   */
  const handleAutoLaunchChange = async (e) => {
    const enabled = e.target.checked;
    setAutoLaunch(enabled);
    if (isElectron) {
      await window.electronAPI.setLoginItem(enabled);
    }
  };

  /*
   * 함수 이름 : handleSave
   * 기능 : 저장 버튼 클릭 시 알림 방식 설정을 로컬 스토리지와 서버에 저장한다.
   * 매개변수 : 없음
   * 반환값 : 없음
   * 작성 날짜 : 2026/03/08
   * 작성자 : 이상혁
   */
  const handleSave = async () => {
    setLoading(true);
    const payload = { popup, sound };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
    try {
      await updateNotificationSettings(payload);
      setNote({ text: '저장 완료.', type: 'success' });
    } catch {
      setNote({ text: '저장 완료 (로컬 저장).', type: 'info' });
    } finally {
      setLoading(false);
    }
  };

  const noteColor = { success: '#4ade80', error: '#f87171', info: '#60a5fa' }[note.type] || '#9ca3af';

  /*
   * 함수 이름 : ToggleRow
   * 기능 : 레이블과 체크박스로 구성된 토글 행 컴포넌트를 렌더링한다.
   * 매개변수 : label - 표시할 텍스트, checked - 체크 여부, onChange - 체크박스 변경 핸들러
   * 반환값 : JSX.Element
   * 작성 날짜 : 2026/03/08
   * 작성자 : 이상혁
   */
  const ToggleRow = ({ label, checked, onChange }) => (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '14px 16px',
      borderRadius: 10,
      background: 'rgba(15,23,42,0.4)',
      border: '1px solid rgba(148,163,184,0.1)',
    }}>
      <span style={{ fontSize: 14, color: '#e2e8f0', fontWeight: 500 }}>{label}</span>
      <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', flexDirection: 'row' }}>
        <input type="checkbox" checked={checked} onChange={onChange} />
        <span style={{ fontSize: 13, color: checked ? '#60a5fa' : '#64748b' }}>
          {checked ? '활성' : '비활성'}
        </span>
      </label>
    </div>
  );

  return (
    <div className="page-container">
      <h1>알림 방식 설정</h1>
      <p className="page-description">
        알림을 받을 방식을 선택합니다.
      </p>

      <div style={{ maxWidth: 480, display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <ToggleRow
            label="팝업 알림"
            checked={popup}
            onChange={(e) => setPopup(e.target.checked)}
          />
          <ToggleRow
            label="소리 알림"
            checked={sound}
            onChange={(e) => setSound(e.target.checked)}
          />
        </div>

        {isElectron && (
          <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            <div style={{ fontSize: 13, color: '#94a3b8', marginBottom: 2 }}>시스템 설정</div>
            <ToggleRow
              label="컴퓨터 시작 시 자동 실행"
              checked={autoLaunch}
              onChange={handleAutoLaunchChange}
            />
          </div>
        )}

        <button className="btn" onClick={handleSave} disabled={loading}>
          {loading ? '처리 중...' : '저장'}
        </button>

        {note.text && (
          <p style={{ fontSize: 13, color: noteColor, margin: 0 }}>{note.text}</p>
        )}
      </div>
    </div>
  );
}

export default SettingNotifyPage;
