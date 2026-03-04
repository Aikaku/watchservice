/**
 * 파일 이름 : AdminApi.js
 * 기능 : 관리자 로그인 및 피드백/공지 관리 API 래퍼를 제공한다.
 */
import { get, post, del } from './HttpClient';

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

