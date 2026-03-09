/**
 * 파일 이름 : ConfirmModal.jsx
 * 기능 : 커스텀 확인 모달 컴포넌트.
 *        ConfirmContext를 통해 전역에서 await confirm(message) 형태로 호출 가능.
 *        확인 → true, 취소/배경 클릭 → false 반환.
 * 작성 날짜 : 2026/03/09
 * 작성자 : 시스템
 */
import React, { createContext, useCallback, useContext, useRef, useState } from 'react';

const ConfirmContext = createContext(null);

export function ConfirmProvider({ children }) {
  const [modal, setModal] = useState(null); // { message } | null
  const resolveRef = useRef(null);

  const confirm = useCallback((message) => {
    return new Promise((resolve) => {
      resolveRef.current = resolve;
      setModal({ message });
    });
  }, []);

  const handleOk = () => {
    setModal(null);
    resolveRef.current?.(true);
  };

  const handleCancel = () => {
    setModal(null);
    resolveRef.current?.(false);
  };

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      {modal && (
        <div
          onClick={handleCancel}
          style={{
            position: 'fixed', inset: 0,
            background: 'rgba(0,0,0,0.55)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            zIndex: 10000,
          }}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{
              background: '#1f2937',
              border: '1px solid #374151',
              borderRadius: 12,
              padding: '28px 32px',
              minWidth: 280,
              maxWidth: 400,
              boxShadow: '0 8px 32px rgba(0,0,0,0.5)',
              display: 'flex',
              flexDirection: 'column',
              gap: 20,
            }}
          >
            <p style={{ margin: 0, color: '#f3f4f6', fontSize: 15, lineHeight: 1.6 }}>
              {modal.message}
            </p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
              <button
                onClick={handleCancel}
                style={{
                  padding: '7px 18px', borderRadius: 6, border: '1px solid #4b5563',
                  background: 'transparent', color: '#9ca3af', fontSize: 13, cursor: 'pointer',
                }}
              >
                취소
              </button>
              <button
                onClick={handleOk}
                style={{
                  padding: '7px 18px', borderRadius: 6, border: 'none',
                  background: '#ef4444', color: '#fff', fontSize: 13,
                  fontWeight: 600, cursor: 'pointer',
                }}
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}
    </ConfirmContext.Provider>
  );
}

/**
 * 함수 이름 : useConfirm
 * 기능 : 확인 모달 함수를 반환하는 훅.
 *        반환값: confirm(message) → Promise<boolean>
 */
export function useConfirm() {
  const ctx = useContext(ConfirmContext);
  if (!ctx) throw new Error('useConfirm must be used within ConfirmProvider');
  return ctx;
}
