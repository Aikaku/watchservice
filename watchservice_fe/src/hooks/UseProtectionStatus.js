/**
 * 파일 이름 : UseProtectionStatus.js
 * 기능 : 보호 상태 조회를 위한 커스텀 훅. 대시보드 요약 정보를 불러온다.
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
import { useCallback, useEffect, useState } from 'react';
import { fetchDashboardSummary } from '../api/DashboardApi';

/*
 * 함수 이름 : useProtectionStatus
 * 기능 : 보호 상태 조회를 위한 커스텀 훅. 대시보드 요약 정보(statusLabel, status, lastEventTime, guidance)를 불러온다.
 * 매개변수 : 없음
 * 반환값 : Object - 요약 데이터(summary), 로딩 상태(loading), 에러(error), 새로고침 함수(refresh)를 포함한 객체
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
export function useProtectionStatus() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  /*
   * 함수 이름 : load
   * 기능 : 대시보드 요약 정보를 서버에서 불러온다.
   * 매개변수 : 없음
   * 반환값 : 없음
   * 작성 날짜 : 2026/03/08
   * 작성자 : 시스템
   */
  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetchDashboardSummary();
      setData(res);
    } catch (e) {
      setError(e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return {
    summary: data,
    loading,
    error,
    refresh: load,
  };
}
