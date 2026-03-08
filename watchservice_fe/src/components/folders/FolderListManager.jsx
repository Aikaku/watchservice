/**
 * 파일 이름 : FolderListManager.jsx
 * 기능 : 폴더 목록 관리 컴포넌트. 감시 대상 폴더 목록을 표시하고 추가/삭제 기능을 제공한다.
 *        폴더 추가는 서버 파일시스템 탐색 모달(FolderPickerModal)을 통해 처리한다.
 * 작성 날짜 : 2025/12/17
 * 작성자 : 시스템
 */
import React, { useState } from 'react';
import FolderPickerModal from './FolderPickerModal';

/**
 * 함수 이름 : FolderListManager
 * 기능 : 폴더 목록 관리 컴포넌트.
 * 매개변수 : folders - 폴더 배열, onAddFolder(path, name) - 폴더 추가 핸들러, onRemoveFolder - 폴더 삭제 핸들러
 * 반환값 : JSX.Element - 폴더 목록 관리 컴포넌트
 * 작성 날짜 : 2025/12/17
 * 작성자 : 시스템
 */
function FolderListManager({ folders, onAddFolder, onRemoveFolder }) {
  const [showPicker, setShowPicker] = useState(false);
  const [error, setError] = useState('');

  const handleSelect = async (path) => {
    setShowPicker(false);
    setError('');
    try {
      await onAddFolder(path, '');
    } catch (e) {
      setError(e.message || '폴더 추가에 실패했습니다.');
    }
  };

  return (
    <>
      <div className="folder-panel">
        <div className="panel-header">
          <h2>감시 대상 폴더</h2>
        </div>

        <div className="folder-list">
          {folders.map((f) => (
            <div key={f.id} className="folder-item">
              <div className="folder-name">
                {f.name}
                <div style={{ fontSize: '11px', color: '#9ca3af', marginTop: '2px' }}>
                  {f.path}
                </div>
              </div>
              <button className="btn-icon" onClick={() => onRemoveFolder?.(f.id)}>
                삭제
              </button>
            </div>
          ))}

          {folders.length === 0 && (
            <div style={{ fontSize: '13px', color: '#9ca3af' }}>
              아직 등록된 감시 폴더가 없습니다. 아래 버튼으로 추가해 주세요.
            </div>
          )}
        </div>

        {error && (
          <div style={{ fontSize: 12, color: '#f87171', marginTop: 8 }}>{error}</div>
        )}

        <button className="btn btn-outline" onClick={() => setShowPicker(true)}>
          폴더 추가
        </button>
      </div>

      {showPicker && (
        <FolderPickerModal
          onSelect={handleSelect}
          onClose={() => setShowPicker(false)}
        />
      )}
    </>
  );
}

export default FolderListManager;
