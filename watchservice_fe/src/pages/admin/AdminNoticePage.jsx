/**
 * 파일 이름 : AdminNoticePage.jsx
 * 기능 : 관리자 공지사항 관리 페이지. 공지 등록 및 삭제를 제공한다.
 */
import React, { useEffect, useState } from 'react';
import { fetchNotices, createNotice, deleteNotice } from '../../api/AdminApi';
import { useToast } from '../../components/common/Toast';
import { useConfirm } from '../../components/common/ConfirmModal';

function AdminNoticePage() {
  const toast = useToast();
  const confirm = useConfirm();
  const [items, setItems] = useState([]);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchNotices();
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

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!content.trim()) {
      toast('내용을 입력하세요.', 'warn');
      return;
    }
    try {
      await createNotice({ title: title.trim(), content });
      setTitle('');
      setContent('');
      load();
      toast('공지사항이 등록되었습니다.', 'success');
    } catch (e) {
      console.error(e);
      toast('등록 중 오류가 발생했습니다.', 'error');
    }
  };

  const handleDelete = async (id) => {
    if (!await confirm('이 공지를 삭제하시겠습니까?')) return;
    try {
      await deleteNotice(id);
      load();
    } catch (e) {
      console.error(e);
      toast('삭제 중 오류가 발생했습니다.', 'error');
    }
  };

  return (
    <div className="page-container">
      <h1>공지사항 관리</h1>

      <form onSubmit={handleCreate} style={{ marginBottom: 16, maxWidth: 480 }}>
        <div style={{ marginBottom: 8 }}>
          <label>
            제목(선택)
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              style={{ width: '100%' }}
            />
          </label>
        </div>
        <div style={{ marginBottom: 8 }}>
          <label>
            내용
            <textarea
              rows={4}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              style={{ width: '100%' }}
            />
          </label>
        </div>
        <button className="btn" type="submit">
          등록
        </button>
      </form>

      {loading && <p>불러오는 중...</p>}
      {error && <p style={{ color: 'red' }}>로드 오류: {error.message}</p>}
      {!loading && !error && items.length === 0 && <p>등록된 공지사항이 없습니다.</p>}

      {!loading && !error && items.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>제목</th>
              <th>내용</th>
              <th>작성시간</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            {items.map((n) => (
              <tr key={n.id}>
                <td>{n.id}</td>
                <td>{n.title || '-'}</td>
                <td style={{ maxWidth: 400, whiteSpace: 'pre-wrap' }}>{n.content}</td>
                <td>{n.createdAt}</td>
                <td>
                  <button className="btn btn-outline" onClick={() => handleDelete(n.id)}>
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

export default AdminNoticePage;

