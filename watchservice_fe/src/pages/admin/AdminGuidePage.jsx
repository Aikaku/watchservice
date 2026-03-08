/**
 * 파일 이름 : AdminGuidePage.jsx
 * 기능 : 관리자 사용 가이드 편집 페이지. 가이드 내용을 조회하고 수정하여 저장할 수 있다.
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
import React, { useEffect, useState } from 'react';
import { fetchGuide, updateGuide } from '../../api/AdminApi';

/**
 * 함수 이름 : AdminGuidePage
 * 기능 : 관리자 사용 가이드 편집 페이지 컴포넌트.
 * 매개변수 : 없음
 * 반환값 : JSX.Element
 */
function AdminGuidePage() {
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    fetchGuide()
      .then((data) => setContent(data.content || ''))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async () => {
    setSaving(true);
    setError('');
    setSaved(false);
    try {
      await updateGuide(content);
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    } catch (e) {
      setError(e.message || '저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="page-container">
      <h1>사용 가이드 편집</h1>
      <p style={{ fontSize: 14, color: '#9ca3af', marginBottom: 16 }}>
        사용자에게 표시될 가이드 내용을 작성합니다. 마크다운 형식을 지원합니다.
      </p>

      {loading && <div style={{ color: '#9ca3af', fontSize: 14 }}>불러오는 중...</div>}

      {!loading && (
        <>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={28}
            style={{
              width: '100%',
              boxSizing: 'border-box',
              background: '#1f2937',
              border: '1px solid #374151',
              borderRadius: 8,
              color: '#f9fafb',
              fontSize: 13,
              fontFamily: 'monospace',
              lineHeight: 1.7,
              padding: '12px 14px',
              resize: 'vertical',
              outline: 'none',
            }}
          />

          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 12 }}>
            <button className="btn" onClick={handleSave} disabled={saving}>
              {saving ? '저장 중...' : '저장'}
            </button>
            {saved && (
              <span style={{ fontSize: 13, color: '#34d399' }}>저장되었습니다.</span>
            )}
            {error && (
              <span style={{ fontSize: 13, color: '#f87171' }}>{error}</span>
            )}
          </div>

          {/* 미리보기 */}
          <h2 style={{ marginTop: 28, marginBottom: 10, fontSize: 15 }}>미리보기</h2>
          <div
            style={{
              background: '#1f2937',
              border: '1px solid #374151',
              borderRadius: 8,
              padding: '20px 24px',
              fontSize: 14,
              color: '#e5e7eb',
              lineHeight: 1.8,
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
            }}
          >
            {content}
          </div>
        </>
      )}
    </div>
  );
}

export default AdminGuidePage;
