/**
 * 파일 이름 : AudioAlert.jsx
 * 기능 : 새 알림 발생 시 설정에 따라 소리·팝업 알림을 실행한다.
 *        10초 간격으로 최신 알림을 폴링하며, 처음 로드 시에는 기준 ID만 기록한다.
 * 작성 날짜 : 2026/04/06
 * 작성자 : 이상혁
 */
import { useEffect, useRef } from 'react';
import { fetchAlerts } from '../../api/NotificationsApi';

function playAlert(label) {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.connect(gain);
    gain.connect(ctx.destination);

    if (label === 'DANGER') {
      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(880, ctx.currentTime);
      osc.frequency.setValueAtTime(440, ctx.currentTime + 0.12);
      osc.frequency.setValueAtTime(880, ctx.currentTime + 0.24);
      gain.gain.setValueAtTime(0.35, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.5);
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + 0.5);
    } else if (label === 'WARNING') {
      osc.type = 'square';
      osc.frequency.setValueAtTime(660, ctx.currentTime);
      osc.frequency.setValueAtTime(440, ctx.currentTime + 0.18);
      gain.gain.setValueAtTime(0.2, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.4);
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + 0.4);
    } else {
      osc.type = 'sine';
      osc.frequency.setValueAtTime(523, ctx.currentTime);
      gain.gain.setValueAtTime(0.1, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.25);
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + 0.25);
    }
  } catch (_) {}
}

function getSettings() {
  try {
    const raw = localStorage.getItem('watchservice.notifySettings');
    if (!raw) return { sound: false, popup: true }; // SettingNotifyPage 기본값과 일치
    return JSON.parse(raw);
  } catch (_) {
    return { sound: false, popup: true };
  }
}

function showPopup(label, path) {
  if (!('Notification' in window)) return;
  const title = label === 'DANGER' ? '🚨 위험 탐지' : label === 'WARNING' ? '⚠️ 경고 탐지' : 'WatchService Agent';
  const body = path ? `경로: ${path}` : '파일 이벤트가 감지되었습니다.';
  if (Notification.permission === 'granted') {
    new Notification(title, { body });
  } else if (Notification.permission !== 'denied') {
    Notification.requestPermission().then(perm => {
      if (perm === 'granted') new Notification(title, { body });
    });
  }
}

function AudioAlert() {
  const lastIdRef = useRef(null);
  const initializedRef = useRef(false);

  useEffect(() => {
    // 팝업 알림 권한 사전 요청
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }

    async function check() {
      try {
        const res = await fetchAlerts({ page: 0, size: 1, sort: 'createdAt,desc' });
        if (!res?.items?.length) return;

        const latest = res.items[0];

        if (!initializedRef.current) {
          lastIdRef.current = latest.id;
          initializedRef.current = true;
          return;
        }

        if (latest.id !== lastIdRef.current) {
          lastIdRef.current = latest.id;
          const { sound, popup } = getSettings();
          if (sound) playAlert(latest.aiLabel);
          if (popup) showPopup(latest.aiLabel, latest.path);
        }
      } catch (_) {}
    }

    check();
    const id = setInterval(check, 10000);
    return () => clearInterval(id);
  }, []);

  return null;
}

export default AudioAlert;
