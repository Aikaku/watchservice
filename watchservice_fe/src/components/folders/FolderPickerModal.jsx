/**
 * 파일 이름 : FolderPickerModal.jsx
 * 기능 : 서버 파일시스템을 탐색하여 폴더를 선택하는 모달 컴포넌트.
 *        백엔드 /settings/folders/browse API를 통해 디렉토리를 탐색하고
 *        선택한 폴더 경로를 상위 컴포넌트에 전달한다.
 * 작성 날짜 : 2026/03/08
 * 작성자 : 시스템
 */
import React, { useCallback, useEffect, useState } from 'react';
import { browseFolders } from '../../api/SettingApi';

/**
 * 함수 이름 : FolderPickerModal
 * 기능 : 폴더 선택 모달 컴포넌트.
 * 매개변수 :
 *   onSelect(path) - 폴더 선택 완료 시 호출
 *   onClose       - 모달 닫기 시 호출
 * 반환값 : JSX.Element
 */
function FolderPickerModal({ onSelect, onClose }) {
  const [currentPath, setCurrentPath] = useState('');
  const [parentPath, setParentPath] = useState(null);
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const navigate = useCallback(async (path) => {
    setLoading(true);
    setError('');
    try {
      const data = await browseFolders(path);
      setCurrentPath(data.current);
      setParentPath(data.parent);
      setEntries(data.entries || []);
    } catch (e) {
      setError(e.message || '디렉토리를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    navigate('');
  }, [navigate]);

  // 모달 외부 클릭 시 닫기
  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) onClose();
  };

  return (
    <div
      onClick={handleBackdropClick}
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.6)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
      }}
    >
      <div
        style={{
          background: '#1f2937',
          border: '1px solid #374151',
          borderRadius: 10,
          width: 520,
          maxHeight: '70vh',
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
        }}
      >
        {/* 헤더 */}
        <div
          style={{
            padding: '14px 16px',
            borderBottom: '1px solid #374151',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <span style={{ fontWeight: 600, fontSize: 15, color: '#f9fafb' }}>
            폴더 선택
          </span>
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              color: '#9ca3af',
              fontSize: 18,
              cursor: 'pointer',
              lineHeight: 1,
            }}
          >
            ✕
          </button>
        </div>

        {/* 현재 경로 표시 */}
        <div
          style={{
            padding: '8px 16px',
            borderBottom: '1px solid #374151',
            fontSize: 12,
            color: '#9ca3af',
            fontFamily: 'monospace',
            wordBreak: 'break-all',
          }}
        >
          {currentPath || '로딩 중...'}
        </div>

        {/* 상위 폴더 버튼 */}
        {parentPath && (
          <div style={{ padding: '6px 16px', borderBottom: '1px solid #2d3748' }}>
            <button
              onClick={() => navigate(parentPath)}
              disabled={loading}
              style={{
                background: 'none',
                border: 'none',
                color: '#60a5fa',
                fontSize: 13,
                cursor: 'pointer',
                padding: '4px 0',
                display: 'flex',
                alignItems: 'center',
                gap: 6,
              }}
            >
              ← 상위 폴더
            </button>
          </div>
        )}

        {/* 디렉토리 목록 */}
        <div style={{ flex: 1, overflowY: 'auto' }}>
          {loading && (
            <div style={{ padding: 16, fontSize: 13, color: '#9ca3af' }}>
              불러오는 중...
            </div>
          )}
          {!loading && error && (
            <div style={{ padding: 16, fontSize: 13, color: '#f87171' }}>{error}</div>
          )}
          {!loading && !error && entries.length === 0 && (
            <div style={{ padding: 16, fontSize: 13, color: '#9ca3af' }}>
              하위 폴더가 없습니다.
            </div>
          )}
          {!loading &&
            entries.map((entry) => (
              <div
                key={entry.path}
                onClick={() => navigate(entry.path)}
                style={{
                  padding: '10px 16px',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  fontSize: 13,
                  color: '#f9fafb',
                  borderBottom: '1px solid #2d3748',
                }}
                onMouseEnter={(e) => (e.currentTarget.style.background = '#374151')}
                onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
              >
                <span style={{ fontSize: 16 }}>📁</span>
                {entry.name}
              </div>
            ))}
        </div>

        {/* 하단 버튼 */}
        <div
          style={{
            padding: '12px 16px',
            borderTop: '1px solid #374151',
            display: 'flex',
            gap: 8,
            justifyContent: 'flex-end',
          }}
        >
          <button className="btn btn-outline" onClick={onClose}>
            취소
          </button>
          <button
            className="btn"
            onClick={() => currentPath && onSelect(currentPath)}
            disabled={!currentPath}
          >
            이 폴더 선택
          </button>
        </div>
      </div>
    </div>
  );
}

export default FolderPickerModal;
