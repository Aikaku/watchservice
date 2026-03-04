/**
 * 파일 이름 : AdminFeedbackPage.jsx
 * 기능 : 관리자 피드백 관리 페이지. 피드백 목록 조회 및 삭제를 제공한다.
 */
import React, { useEffect, useState } from 'react';
import { fetchAdminFeedback, deleteAdminFeedback } from '../../api/AdminApi';

function AdminFeedbackPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchAdminFeedback();
      setItems(Array.isArray(data) ? data : []);
    } catch (e) {
      console.error(e);
      setError(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const handleDelete = async (id) => {
    if (!window.confirm('이 피드백을 삭제하시겠습니까?')) return;
    try {
      await deleteAdminFeedback(id);
      load();
    } catch (e) {
      console.error(e);
      alert('삭제 중 오류가 발생했습니다.');
    }
  };

  return (
    <div className="page-container">
      <h1>피드백 관리</h1>
      {loading && <p>불러오는 중...</p>}
      {error && <p style={{ color: 'red' }}>로드 오류: {error.message}</p>}
      {!loading && !error && items.length === 0 && <p>표시할 피드백이 없습니다.</p>}

      {!loading && !error && items.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>이메일</th>
              <th>내용</th>
              <th>작성시간</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            {items.map((f) => (
              <tr key={f.id}>
                <td>{f.id}</td>
                <td>{f.email || '-'}</td>
                <td style={{ maxWidth: 400, whiteSpace: 'pre-wrap' }}>{f.content}</td>
                <td>{f.createdAt}</td>
                <td>
                  <button className="btn btn-outline" onClick={() => handleDelete(f.id)}>
                    삭제
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default AdminFeedbackPage;

