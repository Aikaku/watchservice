/**
 * 파일 이름 : AdminAlertPage.jsx
 * 기능 : 관리자 전용 알림/탐지 관리 페이지. 전체 owner_key의 알림(notification)을 조회·삭제한다.
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
import React, { useState, useEffect, useMemo } from 'react';
import { fetchAdminAlerts, deleteAdminAlert } from '../../api/AdminApi';
import { useToast } from '../../components/common/Toast';
import { useConfirm } from '../../components/common/ConfirmModal';

/*
 * 함수 이름 : AdminAlertPage
 * 기능 : 관리자 전용 알림/탐지 관리 페이지 컴포넌트. 전체 에이전트의 AI 분석 알림을 페이지네이션, 필터링하여 조회하고 삭제한다.
 * 매개변수 : 없음
 * 반환값 : JSX.Element
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
function AdminAlertPage() {
  const toast = useToast();
  const confirm = useConfirm();
  const [data, setData] = useState({ items: [], total: 0, page: 1, size: 50 });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const [page, setPage] = useState(1);
  const [size, setSize] = useState(50);
  const [level, setLevel] = useState('');
  const [keyword, setKeyword] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [sort, setSort] = useState('createdAt,desc');

  const totalPages = useMemo(() => Math.max(1, Math.ceil((data.total || 0) / size)), [data.total, size]);

  /*
   * 함수 이름 : load
   * 기능 : 현재 필터 조건으로 알림 목록을 서버에서 불러온다.
   * 매개변수 : p - 조회할 페이지 번호 (기본값: 현재 page 상태)
   * 반환값 : 없음
   * 작성 날짜 : 2026/03/08
   * 작성자 : 시스템
   */
  const load = async (p = page) => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetchAdminAlerts({ page: p, size, level, keyword, from, to, sort });
      setData(res);
    } catch (e) {
      setError(e.message || '오류');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(1); setPage(1); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  /*
   * 함수 이름 : handleSearch
   * 기능 : 검색 버튼 클릭 시 첫 페이지에서 목록을 다시 불러온다.
   * 매개변수 : 없음
   * 반환값 : 없음
   * 작성 날짜 : 2026/03/08
   * 작성자 : 시스템
   */
  const handleSearch = () => { setPage(1); load(1); };

  /*
   * 함수 이름 : handleDelete
   * 기능 : 특정 알림을 삭제한다. 확인 모달 후 삭제 요청을 보낸다.
   * 매개변수 : id - 삭제할 알림 ID
   * 반환값 : 없음
   * 작성 날짜 : 2026/03/08
   * 작성자 : 시스템
   */
  const handleDelete = async (id) => {
    if (!await confirm(`알림 #${id}를 삭제하시겠습니까?`)) return;
    try {
      await deleteAdminAlert(id);
      load(page);
    } catch (e) {
      toast('삭제 실패: ' + e.message, 'error');
    }
  };

  /*
   * 함수 이름 : labelColor
   * 기능 : AI 라벨(위험도)에 따른 색상 코드를 반환한다.
   * 매개변수 : label - AI 라벨 (DANGER|WARNING|SAFE)
   * 반환값 : string - 색상 코드
   * 작성 날짜 : 2026/03/08
   * 작성자 : 시스템
   */
  const labelColor = (label) => {
    if (label === 'DANGER') return '#ef4444';
    if (label === 'WARNING') return '#eab308';
    if (label === 'SAFE') return '#22c55e';
    return '#9ca3af';
  };

  return (
    <div className="page-container">
      <h1>관리자 알림/탐지 관리</h1>
      <p style={{ fontSize: 13, color: '#9ca3af', marginTop: 4 }}>
        모든 에이전트의 AI 분석 알림을 조회하고 관리합니다.
      </p>

      {/* 필터 */}
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center', marginBottom: 12 }}>
        <label>
          표시 수:&nbsp;
          <select value={size} onChange={e => setSize(Number(e.target.value))}>
            <option value={20}>20</option>
            <option value={50}>50</option>
            <option value={100}>100</option>
          </select>
        </label>
        <select value={level} onChange={e => setLevel(e.target.value)}>
          <option value="">위험도 전체</option>
          <option value="DANGER">DANGER</option>
          <option value="WARNING">WARNING</option>
          <option value="SAFE">SAFE</option>
        </select>
        <input type="date" value={from} onChange={e => setFrom(e.target.value)} />
        <input type="date" value={to} onChange={e => setTo(e.target.value)} />
        <input
          type="text"
          placeholder="경로/AI 상세/패밀리 검색"
          value={keyword}
          onChange={e => setKeyword(e.target.value)}
          style={{ minWidth: 200 }}
        />
        <select value={sort} onChange={e => setSort(e.target.value)}>
          <option value="createdAt,desc">시간(최신순)</option>
          <option value="createdAt,asc">시간(오래된순)</option>
          <option value="aiScore,desc">AI 점수(높은순)</option>
          <option value="aiScore,asc">AI 점수(낮은순)</option>
        </select>
        <button className="btn" onClick={handleSearch}>검색</button>
        <button className="btn" onClick={() => load(page)}>새로고침</button>

        <div style={{ marginLeft: 'auto', display: 'flex', gap: 8, alignItems: 'center' }}>
          <button className="btn" disabled={page <= 1} onClick={() => { setPage(p => p - 1); load(page - 1); }}>이전</button>
          <span>페이지: {page} / {totalPages} (총 {data.total}건)</span>
          <button className="btn" disabled={page >= totalPages} onClick={() => { setPage(p => p + 1); load(page + 1); }}>다음</button>
        </div>
      </div>

      {loading && <p>불러오는 중...</p>}
      {error && <p style={{ color: 'red' }}>오류: {error}</p>}

      {!loading && !error && (
        <table className="table" style={{ width: '100%', fontSize: 12 }}>
          <thead>
            <tr>
              <th>ID</th>
              <th>OwnerKey</th>
              <th>위험도</th>
              <th>패밀리</th>
              <th>AI 점수</th>
              <th>영향 파일</th>
              <th>생성 시각</th>
              <th>삭제</th>
            </tr>
          </thead>
          <tbody>
            {data.items.length === 0 && (
              <tr><td colSpan={8} style={{ textAlign: 'center', color: '#9ca3af' }}>데이터 없음</td></tr>
            )}
            {data.items.map(item => (
              <tr key={item.id}>
                <td>{item.id}</td>
                <td style={{ maxWidth: 100, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                    title={item.ownerKey}>{item.ownerKey ? item.ownerKey.substring(0, 8) + '...' : '-'}</td>
                <td>
                  <span style={{ color: labelColor(item.aiLabel), fontWeight: 'bold' }}>
                    {item.aiLabel || 'UNKNOWN'}
                  </span>
                </td>
                <td>{item.topFamily || '-'}</td>
                <td>{item.aiScore != null ? (item.aiScore * 100).toFixed(1) + '%' : '-'}</td>
                <td>{item.affectedFilesCount}</td>
                <td>{item.createdAt}</td>
                <td>
                  <button className="btn btn-outline" onClick={() => handleDelete(item.id)} style={{ fontSize: 11, padding: '2px 8px' }}>삭제</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default AdminAlertPage;
