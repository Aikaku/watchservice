/**
 * 파일 이름 : SettingGuidePage.jsx
 * 기능 : 사용 가이드 조회 페이지. 관리자가 작성한 가이드 내용을 마크다운으로 표시한다.
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
import React, { useEffect, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import { fetchGuide } from '../../api/AdminApi';

const mdStyle = {
  color: '#e5e7eb',
  lineHeight: 1.8,
  fontSize: 14,
};

const components = {
  h1: ({ children }) => (
    <h1 style={{ color: '#f9fafb', fontSize: 20, fontWeight: 700, marginTop: 0, marginBottom: 16, borderBottom: '1px solid #374151', paddingBottom: 8 }}>{children}</h1>
  ),
  h2: ({ children }) => (
    <h2 style={{ color: '#f3f4f6', fontSize: 17, fontWeight: 600, marginTop: 28, marginBottom: 10, borderBottom: '1px solid #2d3748', paddingBottom: 6 }}>{children}</h2>
  ),
  h3: ({ children }) => (
    <h3 style={{ color: '#e5e7eb', fontSize: 15, fontWeight: 600, marginTop: 20, marginBottom: 8 }}>{children}</h3>
  ),
  p: ({ children }) => (
    <p style={{ marginTop: 0, marginBottom: 10, color: '#d1d5db' }}>{children}</p>
  ),
  ul: ({ children }) => (
    <ul style={{ paddingLeft: 20, marginBottom: 10, color: '#d1d5db' }}>{children}</ul>
  ),
  ol: ({ children }) => (
    <ol style={{ paddingLeft: 20, marginBottom: 10, color: '#d1d5db' }}>{children}</ol>
  ),
  li: ({ children }) => (
    <li style={{ marginBottom: 4 }}>{children}</li>
  ),
  table: ({ children }) => (
    <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 16, fontSize: 13 }}>{children}</table>
  ),
  thead: ({ children }) => (
    <thead style={{ background: '#374151' }}>{children}</thead>
  ),
  th: ({ children }) => (
    <th style={{ padding: '7px 12px', textAlign: 'left', color: '#f3f4f6', fontWeight: 600, borderBottom: '1px solid #4b5563' }}>{children}</th>
  ),
  td: ({ children }) => (
    <td style={{ padding: '6px 12px', borderBottom: '1px solid #2d3748', color: '#d1d5db' }}>{children}</td>
  ),
  blockquote: ({ children }) => (
    <blockquote style={{ borderLeft: '3px solid #3b82f6', margin: '12px 0', paddingLeft: 12, color: '#9ca3af', fontStyle: 'italic' }}>{children}</blockquote>
  ),
  code: ({ children }) => (
    <code style={{ background: '#111827', color: '#93c5fd', padding: '1px 6px', borderRadius: 4, fontSize: 12, fontFamily: 'monospace' }}>{children}</code>
  ),
  strong: ({ children }) => (
    <strong style={{ color: '#f9fafb', fontWeight: 600 }}>{children}</strong>
  ),
  hr: () => (
    <hr style={{ border: 'none', borderTop: '1px solid #374151', margin: '20px 0' }} />
  ),
};

function SettingGuidePage() {
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchGuide()
      .then((data) => setContent(data.content || ''))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="page-container">
      <h1>사용 가이드</h1>
      <p style={{ fontSize: 14, color: '#9ca3af', marginBottom: 20 }}>
        WatchService Agent 사용 방법을 안내합니다.
      </p>

      {loading && <div style={{ color: '#9ca3af', fontSize: 14 }}>불러오는 중...</div>}
      {error && <div style={{ color: '#f87171', fontSize: 14 }}>오류: {error}</div>}

      {!loading && !error && (
        <div
          style={{
            background: '#1f2937',
            border: '1px solid #374151',
            borderRadius: 8,
            padding: '24px 28px',
            ...mdStyle,
          }}
        >
          <ReactMarkdown components={components}>{content}</ReactMarkdown>
        </div>
      )}
    </div>
  );
}

export default SettingGuidePage;
