/**
 * 파일 이름 : SettingSchedulePage.jsx
 * 기능 : 감시 스케줄 설정 페이지. 특정 요일·시간대에만 감시를 활성화하도록 설정한다.
 * 작성 날짜 : 2026/04/24
 * 작성자 : 시스템
 */
import React, { useEffect, useState } from 'react';
import { fetchWatchSchedule, updateWatchSchedule } from '../../api/SettingApi';
import { useToast } from '../../components/common/Toast';

const DAY_LABELS = ['월', '화', '수', '목', '금', '토', '일'];
const DAY_VALUES = [1, 2, 3, 4, 5, 6, 7];

const DEFAULT_SCHEDULE = {
  enabled: false,
  days: [1, 2, 3, 4, 5],
  startTime: '09:00',
  endTime: '18:00',
};

/**
 * 함수 이름 : SettingSchedulePage
 * 기능 : 감시 스케줄 설정 페이지 컴포넌트.
 * 매개변수 : 없음
 * 반환값 : JSX.Element
 * 작성 날짜 : 2026/04/24
 * 작성자 : 시스템
 */
function SettingSchedulePage() {
  const { showToast } = useToast();

  const [schedule, setSchedule] = useState(DEFAULT_SCHEDULE);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const res = await fetchWatchSchedule();
        const raw = res?.schedule;
        if (raw) {
          const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw;
          setSchedule({ ...DEFAULT_SCHEDULE, ...parsed });
        }
      } catch (e) {
        showToast('스케줄 불러오기 실패: ' + e.message, 'error');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const toggleDay = (day) => {
    setSchedule((prev) => {
      const days = prev.days.includes(day)
        ? prev.days.filter((d) => d !== day)
        : [...prev.days, day].sort((a, b) => a - b);
      return { ...prev, days };
    });
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await updateWatchSchedule(schedule);
      showToast('스케줄이 저장되었습니다.', 'success');
    } catch (e) {
      showToast('저장 실패: ' + e.message, 'error');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="page-container"><p>불러오는 중...</p></div>;

  return (
    <div className="page-container">
      <h1>감시 스케줄 설정</h1>
      <p style={{ fontSize: 13, color: '#9ca3af', marginBottom: 20 }}>
        특정 요일·시간대에만 감시를 활성화할 수 있습니다. 비활성화하면 항상 감시합니다.
        변경사항은 다음 1분 체크 시 자동 적용됩니다.
      </p>

      {/* 스케줄 활성화 토글 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 28 }}>
        <label style={{ fontWeight: 700, fontSize: 14 }}>스케줄 활성화</label>
        <input
          type="checkbox"
          checked={schedule.enabled}
          onChange={(e) => setSchedule((prev) => ({ ...prev, enabled: e.target.checked }))}
          style={{ width: 18, height: 18 }}
        />
        <span style={{ fontSize: 13, color: schedule.enabled ? '#4ade80' : '#9ca3af' }}>
          {schedule.enabled ? '활성 — 설정한 시간대에만 감시' : '비활성 — 항상 감시'}
        </span>
      </div>

      {/* 요일 선택 */}
      <div style={{ marginBottom: 24 }}>
        <p style={{ fontWeight: 700, fontSize: 14, marginBottom: 10 }}>감시할 요일</p>
        <div style={{ display: 'flex', gap: 8 }}>
          {DAY_VALUES.map((d, i) => {
            const active = schedule.days.includes(d);
            return (
              <button
                key={d}
                onClick={() => toggleDay(d)}
                style={{
                  width: 40, height: 40, borderRadius: 8, border: 'none',
                  cursor: 'pointer', fontWeight: 700, fontSize: 13,
                  background: active ? '#2563eb' : '#374151',
                  color: active ? '#fff' : '#9ca3af',
                }}
              >
                {DAY_LABELS[i]}
              </button>
            );
          })}
        </div>
      </div>

      {/* 시간 범위 */}
      <div style={{ marginBottom: 28 }}>
        <p style={{ fontWeight: 700, fontSize: 14, marginBottom: 10 }}>감시 시간대</p>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <label style={{ fontSize: 13 }}>
            시작
            <input
              type="time"
              value={schedule.startTime}
              onChange={(e) => setSchedule((prev) => ({ ...prev, startTime: e.target.value }))}
              style={{ marginLeft: 8, padding: '4px 8px' }}
            />
          </label>
          <span style={{ color: '#9ca3af' }}>~</span>
          <label style={{ fontSize: 13 }}>
            종료
            <input
              type="time"
              value={schedule.endTime}
              onChange={(e) => setSchedule((prev) => ({ ...prev, endTime: e.target.value }))}
              style={{ marginLeft: 8, padding: '4px 8px' }}
            />
          </label>
        </div>
        <p style={{ fontSize: 12, color: '#9ca3af', marginTop: 8 }}>
          예: 09:00 ~ 18:00 으로 설정하면 평일 오전 9시부터 오후 6시까지만 감시합니다.
        </p>
      </div>

      <button className="btn" onClick={handleSave} disabled={saving}>
        {saving ? '저장 중...' : '저장'}
      </button>
    </div>
  );
}

export default SettingSchedulePage;
