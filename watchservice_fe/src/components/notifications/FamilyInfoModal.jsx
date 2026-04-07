/**
 * 파일 이름 : FamilyInfoModal.jsx
 * 기능 : 랜섬웨어 패밀리명 클릭 시 공격 방식·주요 타깃·알려진 확장자·대응 방법을 모달로 표시한다.
 * 작성 날짜 : 2026/04/07
 * 작성자 : 시스템
 */
import React, { useEffect, useState } from 'react';
import { get } from '../../api/HttpClient';

function FamilyInfoModal({ familyName, onClose }) {
  const [info, setInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!familyName) return;
    setLoading(true);
    setError(null);

    get(`/api/families/${encodeURIComponent(familyName)}`)
      .then(setInfo)
      .catch(() => setError('패밀리 정보를 찾을 수 없습니다.'))
      .finally(() => setLoading(false));
  }, [familyName]);

  if (!familyName) return null;

  return (
    <div
      style={{
        position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        zIndex: 1000,
      }}
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div style={{
        background: '#1e293b', border: '1px solid #334155', borderRadius: 12,
        padding: '28px 32px', width: 480, maxWidth: '95vw', maxHeight: '80vh',
        overflowY: 'auto', boxShadow: '0 8px 32px rgba(0,0,0,0.5)',
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
          <h2 style={{ margin: 0, fontSize: 18, color: '#f1f5f9' }}>
            {familyName}
          </h2>
          <button
            onClick={onClose}
            style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', fontSize: 20, lineHeight: 1 }}
          >×</button>
        </div>

        {loading && <p style={{ color: '#94a3b8', fontSize: 14 }}>불러오는 중...</p>}

        {error && <p style={{ color: '#f87171', fontSize: 14 }}>{error}</p>}

        {info && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14, fontSize: 13, color: '#cbd5e1' }}>
            <InfoRow label="최초 발견" value={info.firstSeen} />
            <InfoSection label="공격 방식" value={info.attackMethod} />
            <InfoSection label="주요 타깃" value={Array.isArray(info.targets) ? info.targets.join(', ') : info.targets} />
            <InfoRow
              label="알려진 확장자"
              value={Array.isArray(info.knownExtensions) ? info.knownExtensions.join('  ') : info.knownExtensions}
              mono
            />
            <InfoSection label="대응 방법" value={info.mitigation} />
          </div>
        )}
      </div>
    </div>
  );
}

function InfoRow({ label, value, mono }) {
  if (!value) return null;
  return (
    <div style={{ display: 'flex', gap: 8 }}>
      <span style={{ color: '#64748b', minWidth: 90, flexShrink: 0 }}>{label}</span>
      <span style={mono ? { fontFamily: 'monospace', color: '#38bdf8' } : {}}>{value}</span>
    </div>
  );
}

function InfoSection({ label, value }) {
  if (!value) return null;
  return (
    <div>
      <div style={{ color: '#64748b', marginBottom: 4 }}>{label}</div>
      <div style={{ lineHeight: 1.6 }}>{value}</div>
    </div>
  );
}

export default FamilyInfoModal;
