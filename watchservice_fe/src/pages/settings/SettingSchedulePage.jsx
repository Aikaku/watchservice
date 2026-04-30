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

  if (loading) {
    return (
      <div className="page-container">
        <p style={{ color: '#9ca3af', fontSize: 14 }}>불러오는 중...</p>
      </div>
    );
  }

  return (
    <div className="page-container">
      <h1>감시 스케줄 설정</h1>
      <p className="page-description">
        특정 요일·시간대에만 감시를 활성화합니다. 비활성화하면 항상 감시합니다.
      </p>

      <div style={{ maxWidth: 520, display: 'flex', flexDirection: 'column', gap: 14 }}>
        {/* 스케줄 활성화 */}
        <div className="card">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <div style={{ fontSize: 14, fontWeight: 600, color: '#f1f5f9', marginBottom: 4 }}>
                스케줄 활성화
              </div>
              <div style={{ fontSize: 13, color: '#64748b' }}>
                {schedule.enabled ? '설정한 시간대에만 감시' : '항상 감시 (스케줄 미사용)'}
              </div>
            </div>
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', flexDirection: 'row' }}>
              <input
                type="checkbox"
                checked={schedule.enabled}
                onChange={(e) => setSchedule((prev) => ({ ...prev, enabled: e.target.checked }))}
              />
              <span style={{ fontSize: 13, color: schedule.enabled ? '#4ade80' : '#64748b' }}>
                {schedule.enabled ? '활성' : '비활성'}
              </span>
            </label>
          </div>
        </div>

        {/* 요일 선택 */}
        <div className="card">
          <div style={{ fontSize: 14, fontWeight: 600, color: '#f1f5f9', marginBottom: 14 }}>
            감시 요일
          </div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {DAY_VALUES.map((d, i) => {
              const active = schedule.days.includes(d);
              return (
                <button
                  key={d}
                  onClick={() => toggleDay(d)}
                  style={{
                    width: 44,
                    height: 44,
                    borderRadius: 10,
                    border: active ? '2px solid #3b82f6' : '1px solid rgba(148,163,184,0.2)',
                    cursor: 'pointer',
                    fontWeight: 700,
                    fontSize: 14,
                    background: active
                      ? 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)'
                      : 'rgba(15,23,42,0.4)',
                    color: active ? '#fff' : '#9ca3af',
                    transition: 'all 0.2s',
                  }}
                >
                  {DAY_LABELS[i]}
                </button>
              );
            })}
          </div>
        </div>

        {/* 시간 범위 */}
        <div className="card">
          <div style={{ fontSize: 14, fontWeight: 600, color: '#f1f5f9', marginBottom: 14 }}>
            감시 시간대
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <label style={{ flex: 1 }}>
              시작
              <input
                type="time"
                value={schedule.startTime}
                onChange={(e) => setSchedule((prev) => ({ ...prev, startTime: e.target.value }))}
                style={{ marginTop: 6 }}
              />
            </label>
            <span style={{ color: '#64748b', marginTop: 24, fontSize: 18 }}>—</span>
            <label style={{ flex: 1 }}>
              종료
              <input
                type="time"
                value={schedule.endTime}
                onChange={(e) => setSchedule((prev) => ({ ...prev, endTime: e.target.value }))}
                style={{ marginTop: 6 }}
              />
            </label>
          </div>
          <p style={{ fontSize: 12, color: '#64748b', marginTop: 10, marginBottom: 0 }}>
            예: 09:00 — 18:00 으로 설정하면 해당 요일 오전 9시부터 오후 6시까지만 감시합니다.
          </p>
        </div>

        <button className="btn" onClick={handleSave} disabled={saving}>
          {saving ? '저장 중...' : '저장'}
        </button>
      </div>
    </div>
  );
}

export default SettingSchedulePage;
