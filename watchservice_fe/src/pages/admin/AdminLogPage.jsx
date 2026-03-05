/**
 * 파일 이름 : AdminLogPage.jsx
 * 기능 : 관리자 전용 로그 관리 페이지. 전체 owner_key의 로그를 조회·삭제·CSV 내보내기한다.
 */
import React, { useState, useEffect, useMemo } from 'react';
import { fetchAdminLogs, deleteAdminLog, deleteAdminLogs } from '../../api/AdminApi';

function AdminLogPage() {
  const [data, setData] = useState({ items: [], total: 0, page: 1, size: 50 });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const [page, setPage] = useState(1);
  const [size, setSize] = useState(50);
  const [keyword, setKeyword] = useState('');
  const [eventType, setEventType] = useState('');
  const [aiLabel, setAiLabel] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [sort, setSort] = useState('collectedAt,desc');

  const [selected, setSelected] = useState(new Set());

  const totalPages = useMemo(() => Math.max(1, Math.ceil((data.total || 0) / size)), [data.total, size]);

  const load = async (p = page) => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetchAdminLogs({ page: p, size, keyword, eventType, aiLabel, from, to, sort });
      setData(res);
      setSelected(new Set());
    } catch (e) {
      setError(e.message || '오류');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(1); setPage(1); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => { setPage(1); load(1); };

  const handleDelete = async (id) => {
    if (!window.confirm(`로그 #${id}를 삭제하시겠습니까?`)) return;
    try {
      await deleteAdminLog(id);
      load(page);
    } catch (e) {
      alert('삭제 실패: ' + e.message);
    }
  };

  const handleBulkDelete = async () => {
    if (selected.size === 0) return;
    if (!window.confirm(`선택된 ${selected.size}개 로그를 삭제하시겠습니까?`)) return;
    try {
      await deleteAdminLogs([...selected]);
      load(page);
    } catch (e) {
      alert('삭제 실패: ' + e.message);
    }
  };

  const toggleSelect = (id) => {
    const next = new Set(selected);
    next.has(id) ? next.delete(id) : next.add(id);
    setSelected(next);
  };

  const toggleAll = () => {
    if (selected.size === data.items.length) {
      setSelected(new Set());
    } else {
      setSelected(new Set(data.items.map(i => i.id)));
    }
  };

  return (
    <div className="page-container">
      <h1>관리자 로그 관리</h1>
      <p style={{ fontSize: 13, color: '#9ca3af', marginTop: 4 }}>
        모든 에이전트의 파일 이벤트 로그를 조회하고 관리합니다.
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
        <select value={eventType} onChange={e => setEventType(e.target.value)}>
          <option value="">이벤트 전체</option>
          <option value="CREATE">CREATE</option>
          <option value="MODIFY">MODIFY</option>
          <option value="DELETE">DELETE</option>
        </select>
        <select value={aiLabel} onChange={e => setAiLabel(e.target.value)}>
          <option value="">AI 라벨 전체</option>
          <option value="DANGER">DANGER</option>
          <option value="WARNING">WARNING</option>
          <option value="SAFE">SAFE</option>
        </select>
        <input type="date" value={from} onChange={e => setFrom(e.target.value)} />
        <input type="date" value={to} onChange={e => setTo(e.target.value)} />
        <input
          type="text"
          placeholder="경로/AI 상세 검색"
          value={keyword}
          onChange={e => setKeyword(e.target.value)}
          style={{ minWidth: 200 }}
        />
        <select value={sort} onChange={e => setSort(e.target.value)}>
          <option value="collectedAt,desc">시간(최신순)</option>
          <option value="collectedAt,asc">시간(오래된순)</option>
          <option value="aiScore,desc">AI 점수(높은순)</option>
          <option value="size,desc">크기(큰순)</option>
        </select>
        <button className="btn" onClick={handleSearch}>검색</button>
        <button className="btn" onClick={() => load(page)}>새로고침</button>

        <div style={{ marginLeft: 'auto', display: 'flex', gap: 8, alignItems: 'center' }}>
          {selected.size > 0 && (
            <button className="btn btn-danger" onClick={handleBulkDelete}>
              선택 삭제 ({selected.size})
            </button>
          )}
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
              <th><input type="checkbox" checked={selected.size === data.items.length && data.items.length > 0} onChange={toggleAll} /></th>
              <th>ID</th>
              <th>OwnerKey</th>
              <th>이벤트</th>
              <th>경로</th>
              <th>AI 라벨</th>
              <th>수집 시각</th>
              <th>삭제</th>
            </tr>
          </thead>
          <tbody>
            {data.items.length === 0 && (
              <tr><td colSpan={8} style={{ textAlign: 'center', color: '#9ca3af' }}>데이터 없음</td></tr>
            )}
            {data.items.map(item => (
              <tr key={item.id}>
                <td><input type="checkbox" checked={selected.has(item.id)} onChange={() => toggleSelect(item.id)} /></td>
                <td>{item.id}</td>
                <td style={{ maxWidth: 120, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                    title={item.ownerKey}>{item.ownerKey ? item.ownerKey.substring(0, 8) + '...' : '-'}</td>
                <td>{item.eventType}</td>
                <td style={{ maxWidth: 240, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                    title={item.path}>{item.path}</td>
                <td>
                  <span style={{ color: item.aiLabel === 'DANGER' ? '#ef4444' : item.aiLabel === 'WARNING' ? '#eab308' : '#22c55e' }}>
                    {item.aiLabel || '-'}
                  </span>
                </td>
                <td>{item.collectedAt}</td>
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

export default AdminLogPage;
