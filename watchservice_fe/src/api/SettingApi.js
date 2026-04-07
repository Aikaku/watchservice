/**
 * 파일 이름 : SettingApi.js
 * 기능 : 설정 관련 API 함수를 제공한다. 감시 폴더, 예외 규칙, 알림 설정, 리셋, 피드백 등의 API를 포함한다.
 * 작성 날짜 : 2025/12/17
 * 작성자 : 시스템
 */
import { get, post, put, del } from './HttpClient';

/** =========================
 * Watched Folders
 * ========================= */
export function fetchWatchedFolders() {
  return get('/settings/folders');
}

export function browseFolders(path = '') {
  const qs = path ? `?path=${encodeURIComponent(path)}` : '';
  return get(`/settings/folders/browse${qs}`);
}

export function pickFolderPath() {
  return get('/settings/folders/pick');
}

export function createWatchedFolder(payload) {
  return post('/settings/folders', payload);
}

export function deleteWatchedFolder(id) {
  return del(`/settings/folders/${encodeURIComponent(id)}`);
}

/** =========================
 * Exception Rules
 * ========================= */
export function fetchExceptionRules() {
  return get('/settings/exceptions');
}

export function createExceptionRule(payload) {
  return post('/settings/exceptions', payload);
}

export function deleteExceptionRule(id) {
  return del(`/settings/exceptions/${encodeURIComponent(id)}`);
}

/** =========================
 * Notification Settings
 * (백엔드 미구현 — 실패 시 프론트에서 localStorage 폴백 처리)
 * ========================= */
export function fetchNotificationSettings() {
  return get('/settings/notification');
}

export function updateNotificationSettings(payload) {
  return put('/settings/notification', payload);
}

/** =========================
 * Reset
 * (백엔드 미구현 — 실패 시 프론트에서 로컬 초기화로 처리)
 * ========================= */
export function resetSettings() {
  return post('/settings/reset');
}

/** =========================
 * Feedback
 * ========================= */
export function sendFeedback(payload) {
  return post('/api/feedback', payload);
}
