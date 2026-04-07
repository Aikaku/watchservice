/**
 * 파일 이름 : AudioAlert.jsx
 * 기능 : 새 알림 발생 시 심각도(DANGER/WARNING/SAFE)별로 Web Audio API를 이용해 소리를 재생한다.
 *        10초 간격으로 최신 알림을 폴링하며, 처음 로드 시에는 소리 없이 기준 ID만 기록한다.
 * 작성 날짜 : 2026/04/06
 * 작성자 : 시스템
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
      // 위험: 고음 삼중 비프
      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(880, ctx.currentTime);
      osc.frequency.setValueAtTime(440, ctx.currentTime + 0.12);
      osc.frequency.setValueAtTime(880, ctx.currentTime + 0.24);
      gain.gain.setValueAtTime(0.35, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.5);
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + 0.5);
    } else if (label === 'WARNING') {
      // 경고: 중간 이중 비프
      osc.type = 'square';
      osc.frequency.setValueAtTime(660, ctx.currentTime);
      osc.frequency.setValueAtTime(440, ctx.currentTime + 0.18);
      gain.gain.setValueAtTime(0.2, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.4);
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + 0.4);
    } else {
      // SAFE: 부드러운 단음
      osc.type = 'sine';
      osc.frequency.setValueAtTime(523, ctx.currentTime);
      gain.gain.setValueAtTime(0.1, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.25);
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + 0.25);
    }
  } catch (_) {
    // Web Audio API 미지원 환경에서는 조용히 무시
  }
}

function isSoundEnabled() {
  try {
    const raw = localStorage.getItem('watchservice.notifySettings');
    if (!raw) return true; // 설정 없으면 기본 ON
    const s = JSON.parse(raw);
    return s.sound !== false;
  } catch (_) {
    return true;
  }
}

function AudioAlert() {
  const lastIdRef = useRef(null);
  const initializedRef = useRef(false);

  useEffect(() => {
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
          if (isSoundEnabled()) playAlert(latest.aiLabel);
        }
      } catch (_) {
        // 폴링 실패는 조용히 무시
      }
    }

    check();
    const id = setInterval(check, 10000);
    return () => clearInterval(id);
  }, []);

  return null;
}

export default AudioAlert;
