/**
 * 파일 이름 : AdminFeedbackPage.jsx
 * 기능 : 관리자 피드백 관리 페이지. 피드백 목록 조회 및 삭제를 제공한다.
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
import React, { useEffect, useState } from 'react';
import { fetchAdminFeedback, deleteAdminFeedback } from '../../api/AdminApi';
import { useToast } from '../../components/common/Toast';
import { useConfirm } from '../../components/common/ConfirmModal';

/*
 * 함수 이름 : AdminFeedbackPage
 * 기능 : 관리자 피드백 관리 페이지 컴포넌트. 피드백 목록을 조회하고 삭제할 수 있다.
 * 매개변수 : 없음
 * 반환값 : JSX.Element
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
function AdminFeedbackPage() {
  const toast = useToast();
  const confirm = useConfirm();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  /*
   * 함수 이름 : load
   * 기능 : 서버에서 피드백 목록을 불러온다.
   * 매개변수 : 없음
   * 반환값 : 없음
   * 작성 날짜 : 2026/03/08
   * 작성자 : 시스템
   */
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

  /*
   * 함수 이름 : handleDelete
   * 기능 : 특정 피드백을 삭제한다. 확인 모달 후 삭제 요청을 보낸다.
   * 매개변수 : id - 삭제할 피드백 ID
   * 반환값 : 없음
   * 작성 날짜 : 2026/03/08
   * 작성자 : 시스템
   */
  const handleDelete = async (id) => {
    if (!await confirm('이 피드백을 삭제하시겠습니까?')) return;
    try {
      await deleteAdminFeedback(id);
      load();
    } catch (e) {
      console.error(e);
      toast('삭제 중 오류가 발생했습니다.', 'error');
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

