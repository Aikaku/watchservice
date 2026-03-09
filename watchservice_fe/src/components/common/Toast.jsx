/**
 * 파일 이름 : Toast.jsx
 * 기능 : 화면 우하단에 표시되는 Toast 알림 컴포넌트.
 *        ToastContext를 통해 전역에서 호출 가능. 3초 후 자동 사라짐.
 * 작성 날짜 : 2026/03/09
 * 작성자 : 시스템
 */
import React, { createContext, useCallback, useContext, useState } from 'react';

const ToastContext = createContext(null);

const TYPE_STYLE = {
  error:   { background: '#ef4444', icon: '✕' },
  success: { background: '#10b981', icon: '✓' },
  info:    { background: '#3b82f6', icon: 'ℹ' },
  warn:    { background: '#f59e0b', icon: '!' },
};

let uid = 0;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const dismiss = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const show = useCallback((message, type = 'info', duration = 3000) => {
    const id = ++uid;
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => dismiss(id), duration);
  }, [dismiss]);

  return (
    <ToastContext.Provider value={show}>
      {children}
      <div
        style={{
          position: 'fixed',
          bottom: 24,
          right: 24,
          display: 'flex',
          flexDirection: 'column',
          gap: 8,
          zIndex: 9999,
          pointerEvents: 'none',
        }}
      >
        {toasts.map((t) => {
          const s = TYPE_STYLE[t.type] || TYPE_STYLE.info;
          return (
            <div
              key={t.id}
              style={{
                background: s.background,
                color: '#fff',
                padding: '10px 14px',
                borderRadius: 8,
                fontSize: 13,
                display: 'flex',
                alignItems: 'flex-start',
                gap: 8,
                maxWidth: 340,
                boxShadow: '0 4px 12px rgba(0,0,0,0.3)',
                pointerEvents: 'auto',
                animation: 'toastIn 0.2s ease',
              }}
            >
              <span style={{ fontWeight: 700, flexShrink: 0 }}>{s.icon}</span>
              <span style={{ flex: 1, lineHeight: 1.5 }}>{t.message}</span>
              <button
                onClick={() => dismiss(t.id)}
                style={{
                  background: 'none', border: 'none', color: '#fff',
                  cursor: 'pointer', fontSize: 14, lineHeight: 1, flexShrink: 0, opacity: 0.8,
                }}
              >
                ✕
              </button>
            </div>
          );
        })}
      </div>
      <style>{`
        @keyframes toastIn {
          from { opacity: 0; transform: translateY(8px); }
          to   { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </ToastContext.Provider>
  );
}

/**
 * 함수 이름 : useToast
 * 기능 : Toast 표시 함수를 반환하는 훅.
 *        반환값: toast(message, type?) — type: 'info'|'success'|'error'|'warn'
 */
export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}
