import React from 'react';
import { useNavigate } from 'react-router-dom';

const ITEMS = [
  { icon: '🗂️', title: '감시 폴더', desc: '감시 대상 폴더 추가/삭제', path: '/settings/folders' },
  { icon: '🚫', title: '예외 관리', desc: '화이트리스트(감시 제외) 규칙', path: '/settings/exceptions' },
  { icon: '🔔', title: '알림 방식', desc: '팝업/소리 설정', path: '/settings/notify' },
  { icon: '📧', title: '이메일 알림', desc: 'DANGER 탐지 시 이메일 경보', path: '/settings/email' },
  { icon: '🕐', title: '감시 스케줄', desc: '요일·시간대별 자동 활성화', path: '/settings/schedule' },
  { icon: '🔒', title: '파일 권한 감사', desc: 'others 쓰기·실행 권한 파일 탐지', path: '/settings/audit' },
  { icon: '🔄', title: '초기화', desc: '설정/캐시 초기화', path: '/settings/reset' },
  { icon: '⬆️', title: '업데이트', desc: '버전/업데이트 확인', path: '/settings/update' },
  { icon: '💬', title: '문의/피드백', desc: '버그·문의 제출', path: '/settings/feedback' },
  { icon: '📖', title: '사용 가이드', desc: 'WatchService 사용 방법 안내', path: '/settings/guide' },
];

function SettingHomePage() {
  const navigate = useNavigate();

  return (
    <div className="page-container">
      <h1>설정</h1>
      <p className="page-description">WatchService Agent 설정 메뉴입니다.</p>

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))',
        gap: 14,
      }}>
        {ITEMS.map((it) => (
          <div
            key={it.path}
            className="card"
            onClick={() => navigate(it.path)}
            style={{ cursor: 'pointer', display: 'flex', flexDirection: 'column', gap: 8 }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span style={{ fontSize: 22 }}>{it.icon}</span>
              <span style={{ fontSize: 16, color: '#475569' }}>›</span>
            </div>
            <div style={{ fontWeight: 700, fontSize: 15, color: '#f1f5f9' }}>{it.title}</div>
            <div style={{ fontSize: 13, color: '#94a3b8', lineHeight: 1.4 }}>{it.desc}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default SettingHomePage;
