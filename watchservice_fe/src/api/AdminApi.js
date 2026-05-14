/**
 * 파일 이름 : AdminApi.js
 * 기능 : 관리자 로그인 및 피드백/공지 관리 API 래퍼를 제공한다.
 */
import { get, post, del, put } from './HttpClient';

/*
 * 함수 이름 : adminLogin
 * 기능 : 관리자 로그인 요청을 보낸다. 성공 시 서버 세션이 생성된다.
 * 매개변수 : username - 관리자 아이디, password - 관리자 비밀번호
 * 반환값 : Promise - 로그인 결과
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function adminLogin(username, password) {
  return post('/api/admin/login', { username, password });
}

/*
 * 함수 이름 : adminLogout
 * 기능 : 관리자 로그아웃 요청을 보낸다. 서버 세션이 무효화된다.
 * 매개변수 : 없음
 * 반환값 : Promise - 로그아웃 결과
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function adminLogout() {
  return post('/api/admin/logout', {});
}

/*
 * 함수 이름 : adminCheckSession
 * 기능 : 관리자 세션이 유효한지 확인한다.
 * 매개변수 : 없음
 * 반환값 : Promise - 세션 확인 결과
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function adminCheckSession() {
  return get('/api/admin/check');
}

/*
 * 함수 이름 : fetchAdminFeedback
 * 기능 : 관리자 피드백 목록을 조회한다.
 * 매개변수 : 없음
 * 반환값 : Promise - 피드백 목록
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function fetchAdminFeedback() {
  return get('/api/admin/feedback');
}

/*
 * 함수 이름 : deleteAdminFeedback
 * 기능 : 특정 피드백을 삭제한다.
 * 매개변수 : id - 삭제할 피드백 ID
 * 반환값 : Promise - 삭제 결과
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function deleteAdminFeedback(id) {
  return del(`/api/admin/feedback/${id}`);
}

/*
 * 함수 이름 : fetchNotices
 * 기능 : 공지사항 목록을 조회한다.
 * 매개변수 : 없음
 * 반환값 : Promise - 공지사항 목록
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function fetchNotices() {
  return get('/api/notifications');
}

/*
 * 함수 이름 : createNotice
 * 기능 : 새 공지사항을 등록한다.
 * 매개변수 : title - 공지 제목, content - 공지 내용
 * 반환값 : Promise - 등록 결과
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function createNotice({ title, content }) {
  return post('/api/admin/notifications', { title, content });
}

/*
 * 함수 이름 : deleteNotice
 * 기능 : 특정 공지사항을 삭제한다.
 * 매개변수 : id - 삭제할 공지사항 ID
 * 반환값 : Promise - 삭제 결과
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function deleteNotice(id) {
  return del(`/api/admin/notifications/${id}`);
}

// ===== 관리자 로그 관리 =====
/*
 * 함수 이름 : fetchAdminLogs
 * 기능 : 관리자용 로그 목록을 페이지네이션, 필터링, 정렬을 지원하여 조회한다.
 * 매개변수 : page - 페이지 번호(1-based), size - 페이지 크기, from - 시작 날짜, to - 종료 날짜, keyword - 검색 키워드, aiLabel - AI 라벨 필터, eventType - 이벤트 타입 필터, sort - 정렬 기준
 * 반환값 : Promise - 페이지네이션된 로그 목록
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
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

/*
 * 함수 이름 : deleteAdminLog
 * 기능 : 특정 로그를 삭제한다.
 * 매개변수 : id - 삭제할 로그 ID
 * 반환값 : Promise - 삭제 결과
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function deleteAdminLog(id) {
  return del(`/api/admin/logs/${id}`);
}

/*
 * 함수 이름 : deleteAdminLogs
 * 기능 : 여러 로그를 일괄 삭제한다.
 * 매개변수 : ids - 삭제할 로그 ID 배열
 * 반환값 : Promise - 삭제 결과
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function deleteAdminLogs(ids) {
  return post('/api/admin/logs/delete', { ids });
}

/*
 * 함수 이름 : exportAdminLogs
 * 기능 : 관리자용 로그를 CSV 또는 JSON 형식으로 내보낸다.
 * 매개변수 : from - 시작 날짜, to - 종료 날짜, keyword - 검색 키워드, aiLabel - AI 라벨 필터, eventType - 이벤트 타입 필터, format - 내보내기 형식 (CSV|JSON, 기본: CSV)
 * 반환값 : Promise - 내보내기 결과 텍스트 또는 JSON
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
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
/*
 * 함수 이름 : fetchAdminAlerts
 * 기능 : 관리자용 알림 목록을 페이지네이션, 필터링, 정렬을 지원하여 조회한다.
 * 매개변수 : page - 페이지 번호(1-based), size - 페이지 크기, from - 시작 날짜, to - 종료 날짜, level - 위험도 필터, keyword - 검색 키워드, sort - 정렬 기준
 * 반환값 : Promise - 페이지네이션된 알림 목록
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
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

/*
 * 함수 이름 : deleteAdminAlert
 * 기능 : 특정 알림을 삭제한다.
 * 매개변수 : id - 삭제할 알림 ID
 * 반환값 : Promise - 삭제 결과
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function deleteAdminAlert(id) {
  return del(`/api/admin/alerts/${id}`);
}

// ===== 세션 관리 =====
/*
 * 함수 이름 : fetchAdminSessions
 * 기능 : 등록된 에이전트(owner_key)별 세션 현황을 조회한다.
 * 매개변수 : 없음
 * 반환값 : Promise - 세션 목록
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function fetchAdminSessions() {
  return get('/api/admin/sessions');
}

// ===== 시스템 상태 =====
/*
 * 함수 이름 : fetchAdminSystem
 * 기능 : 시스템 상태 정보(DB 크기, 레코드 수, AI 서버·Watcher 상태)를 조회한다.
 * 매개변수 : 없음
 * 반환값 : Promise - 시스템 상태 정보
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function fetchAdminSystem() {
  return get('/api/admin/system');
}

// ===== 사용 가이드 =====
/*
 * 함수 이름 : fetchGuide
 * 기능 : 사용 가이드 내용을 조회한다.
 * 매개변수 : 없음
 * 반환값 : Promise - 가이드 내용 객체
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function fetchGuide() {
  return get('/api/guide');
}

/*
 * 함수 이름 : updateGuide
 * 기능 : 사용 가이드 내용을 저장한다.
 * 매개변수 : content - 저장할 가이드 내용 문자열
 * 반환값 : Promise - 저장 결과
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function updateGuide(content) {
  return put('/api/admin/guide', { content });
}
