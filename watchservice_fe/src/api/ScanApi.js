// src/api/ScanApi.js
import { get, post } from './HttpClient';

// 명세: POST /scan/start  Body: { paths:[...], autoStartWatcher:true/false } -> {scanId}
/*
 * 함수 이름 : startScan
 * 기능 : 지정된 경로에 대한 즉시 검사를 시작한다. 완료 후 autoStartWatcher 옵션으로 감시를 자동 시작할 수 있다.
 * 매개변수 : paths - 검사할 폴더 경로 배열, autoStartWatcher - 검사 완료 후 감시 자동 시작 여부 (기본값: true)
 * 반환값 : Promise - 스캔 ID를 포함한 결과 객체 ({scanId})
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function startScan(paths = [], autoStartWatcher = true) {
  return post('/scan/start', { paths, autoStartWatcher });
}

/*
 * 함수 이름 : pauseScan
 * 기능 : 진행 중인 스캔을 일시 중지한다.
 * 매개변수 : scanId - 중지할 스캔 ID
 * 반환값 : Promise - 중지 결과
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function pauseScan(scanId) {
  return post(`/scan/${scanId}/pause`);
}

/*
 * 함수 이름 : fetchScanProgress
 * 기능 : 진행 중인 스캔의 진행률 및 상태를 조회한다.
 * 매개변수 : scanId - 조회할 스캔 ID
 * 반환값 : Promise - 진행률 정보 (percent, status, scanned, total 포함)
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function fetchScanProgress(scanId) {
  return get(`/scan/${scanId}/progress`);
}
