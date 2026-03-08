/**
 * 파일 이름 : AdminApi.js
 * 기능 : 관리자 로그인 및 피드백/공지 관리 API 래퍼를 제공한다.
 */
import { get, post, del, put } from './HttpClient';

export function adminLogin(username, password) {
  return post('/api/admin/login', { username, password });
}

export function adminLogout() {
  return post('/api/admin/logout', {});
}

export function fetchAdminFeedback() {
  return get('/api/admin/feedback');
}

export function deleteAdminFeedback(id) {
  return del(`/api/admin/feedback/${id}`);
}

export function fetchNotices() {
  return get('/api/notifications');
}

export function createNotice({ title, content }) {
  return post('/api/admin/notifications', { title, content });
}

export function deleteNotice(id) {
  return del(`/api/admin/notifications/${id}`);
}

// ===== 관리자 로그 관리 =====
export function fetchAdminLogs({ page = 1, size = 50, from, to, keyword, aiLabel, eventType, sort } = {}) {
  const params = new URLSearchParams();
  params.set('page', page);
  params.set('size', size);
  if (from) params.set('from', from);
  if (to) params.set('to', to);
  if (keyword) params.set('keyword', keyword);
  if (aiLabel) params.set('aiLabel', aiLabel);
  if (eventType) params.set('eventType', eventType);
  if (sort) params.set('sort', sort);
  return get(`/api/admin/logs?${params.toString()}`);
}

export function deleteAdminLog(id) {
  return del(`/api/admin/logs/${id}`);
}

export function deleteAdminLogs(ids) {
  return post('/api/admin/logs/delete', { ids });
}

export function exportAdminLogs({ from, to, keyword, aiLabel, eventType, format = 'CSV' } = {}) {
  const params = new URLSearchParams();
  params.set('format', format);
  if (from) params.set('from', from);
  if (to) params.set('to', to);
  if (keyword) params.set('keyword', keyword);
  if (aiLabel) params.set('aiLabel', aiLabel);
  if (eventType) params.set('eventType', eventType);
  return get(`/api/admin/logs/export?${params.toString()}`);
}

// ===== 관리자 알림 관리 =====
export function fetchAdminAlerts({ page = 1, size = 50, from, to, level, keyword, sort } = {}) {
  const params = new URLSearchParams();
  params.set('page', page);
  params.set('size', size);
  if (from) params.set('from', from);
  if (to) params.set('to', to);
  if (level) params.set('level', level);
  if (keyword) params.set('keyword', keyword);
  if (sort) params.set('sort', sort);
  return get(`/api/admin/alerts?${params.toString()}`);
}

export function deleteAdminAlert(id) {
  return del(`/api/admin/alerts/${id}`);
}

// ===== 세션 관리 =====
export function fetchAdminSessions() {
  return get('/api/admin/sessions');
}

// ===== 시스템 상태 =====
export function fetchAdminSystem() {
  return get('/api/admin/system');
}

// ===== 사용 가이드 =====
export function fetchGuide() {
  return get('/api/guide');
}

export function updateGuide(content) {
  return put('/api/admin/guide', { content });
}

