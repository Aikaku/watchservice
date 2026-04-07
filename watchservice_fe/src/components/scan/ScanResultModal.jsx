/**
 * 파일 이름 : ScanResultModal.jsx
 * 기능 : 즉시 검사(스캔) 완료 후 결과 요약을 모달로 표시한다.
 * 작성 날짜 : 2026/04/07
 * 작성자 : 시스템
 */
import React from 'react';

function ScanResultModal({ result, onClose }) {
  if (!result) return null;

  const { scanned, total } = result;

  return (
    <div
      style={{
        position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.55)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        zIndex: 1000,
      }}
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div style={{
        background: '#1e293b', border: '1px solid #334155', borderRadius: 12,
        padding: '28px 32px', minWidth: 340, maxWidth: 460,
        boxShadow: '0 8px 32px rgba(0,0,0,0.5)',
      }}>
        <h2 style={{ margin: '0 0 18px', fontSize: 18, color: '#f1f5f9' }}>
          스캔 완료
        </h2>

        <div style={{
          display: 'flex', flexDirection: 'column', gap: 10,
          fontSize: 14, color: '#cbd5e1',
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span>검사한 파일 수</span>
            <strong style={{ color: '#38bdf8' }}>{scanned.toLocaleString()} 개</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span>전체 파일 수</span>
            <strong style={{ color: '#94a3b8' }}>{total.toLocaleString()} 개</strong>
          </div>
        </div>

        <p style={{ marginTop: 16, fontSize: 12, color: '#64748b' }}>
          스냅샷이 갱신되었습니다. 감시가 자동으로 시작됩니다.
        </p>

        <button
          className="btn"
          style={{ marginTop: 20, width: '100%' }}
          onClick={onClose}
        >
          확인
        </button>
      </div>
    </div>
  );
}

export default ScanResultModal;
