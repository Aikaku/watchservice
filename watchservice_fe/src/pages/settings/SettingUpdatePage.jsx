/**
 * 파일 이름 : SettingUpdatePage.jsx
 * 기능 : 버전/업데이트 정보 페이지. 현재 버전을 표시한다.
 * 작성 날짜 : 2025/12/17
 * 작성자 : 시스템
 */
import React from 'react';

function SettingUpdatePage() {
  const currentVersion = '0.1.0-dev';

  return (
    <div className="page-container">
      <h1>버전 / 업데이트 정보</h1>
      <p className="page-description">
        현재 WatchService Agent의 버전 정보를 확인하는 화면입니다.
      </p>

      <div className="card">
        <div className="card-row">
          <span className="card-label">현재 버전</span>
          <span className="card-value">{currentVersion}</span>
        </div>
        <div className="card-row">
          <span style={{ color: '#6b7280', fontSize: 13 }}>
            업데이트는 GitHub Releases 페이지에서 확인하세요.
          </span>
        </div>
      </div>
    </div>
  );
}

export default SettingUpdatePage;
