import React, { useState } from 'react';
import { useExceptions } from '../../hooks/UseExceptions';
import { pickFolderPath } from '../../api/SettingApi';
import { useToast } from '../../components/common/Toast';
import { useConfirm } from '../../components/common/ConfirmModal';

/*
 * 함수 이름 : SettingExceptionsPage
 * 기능 : 예외(화이트리스트) 설정 페이지 컴포넌트. 감시 대상에서 제외할 파일/폴더/확장자 규칙을 추가하고 삭제한다.
 * 매개변수 : 없음
 * 반환값 : JSX.Element
 * 작성 날짜 : 2026/03/08
 * 작성자 : 이상혁
 */
function SettingExceptionsPage() {
  const toast = useToast();
  const confirm = useConfirm();
  const { exceptions, loading, error, refresh, addException, removeException } = useExceptions();

  const [type, setType] = useState('PATH');
  const [pattern, setPattern] = useState('');
  const [memo, setMemo] = useState('');
  const [pickingFolder, setPickingFolder] = useState(false);

  /*
   * 함수 이름 : handlePickFolder
   * 기능 : 폴더 선택 버튼 클릭 시 폴더 경로 선택 다이얼로그를 열고 선택된 경로를 패턴 입력란에 설정한다.
   * 매개변수 : 없음
   * 반환값 : 없음
   * 작성 날짜 : 2026/03/08
   * 작성자 : 이상혁
   */
  const handlePickFolder = async () => {
    try {
      setPickingFolder(true);
      const picked = await pickFolderPath();
      const path = typeof picked === 'string' ? picked : (picked?.path ?? '');
      if (path) setPattern(path);
    } catch {
      const path = window.prompt('예외로 등록할 폴더 경로를 입력하세요');
      if (path) setPattern(path);
    } finally {
      setPickingFolder(false);
    }
  };

  /*
   * 함수 이름 : handleAdd
   * 기능 : 예외 규칙 추가 폼 제출 시 입력된 패턴으로 새 예외 규칙을 서버에 등록한다.
   * 매개변수 : e - 폼 제출 이벤트 객체
   * 반환값 : 없음
   * 작성 날짜 : 2026/03/08
   * 작성자 : 이상혁
   */
  const handleAdd = async (e) => {
    e.preventDefault();
    if (!pattern.trim()) {
      toast('예외로 등록할 경로나 패턴을 입력해주세요.', 'warn');
      return;
    }
    await addException({ type, pattern: pattern.trim(), memo: memo.trim() });
    setPattern('');
    setMemo('');
  };

  /*
   * 함수 이름 : handleRemove
   * 기능 : 예외 규칙을 삭제한다. 확인 모달 후 삭제 요청을 보낸다.
   * 매개변수 : id - 삭제할 예외 규칙 ID
   * 반환값 : 없음
   * 작성 날짜 : 2026/03/08
   * 작성자 : 이상혁
   */
  const handleRemove = async (id) => {
    if (!await confirm('이 예외 규칙을 삭제하시겠습니까?')) return;
    removeException(id);
  };

  return (
    <div className="page-container">
      <h1>예외(화이트리스트) 설정</h1>
      <p className="page-description">
        감시 대상에서 제외할 파일/폴더/확장자를 등록합니다.
      </p>

      <div style={{ maxWidth: 580, display: 'flex', flexDirection: 'column', gap: 16 }}>
        {/* 추가 폼 */}
        <div className="card">
          <div style={{ fontSize: 14, fontWeight: 600, color: '#f1f5f9', marginBottom: 14 }}>
            예외 규칙 추가
          </div>
          <form onSubmit={handleAdd} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <label>
              예외 종류
              <select
                value={type}
                onChange={(e) => { setType(e.target.value); setPattern(''); }}
                style={{ marginTop: 6 }}
              >
                <option value="PATH">경로(파일/폴더)</option>
                <option value="EXT">확장자</option>
              </select>
            </label>

            <label>
              패턴
              <div style={{ display: 'flex', gap: 8, marginTop: 6 }}>
                <input
                  type="text"
                  value={pattern}
                  onChange={type === 'EXT' ? (e) => setPattern(e.target.value) : undefined}
                  placeholder={
                    type === 'PATH'
                      ? '폴더 선택 버튼을 눌러 경로를 선택하세요'
                      : '.log, .tmp 처럼 확장자를 입력하세요'
                  }
                  readOnly={type === 'PATH'}
                  style={{ flex: 1, cursor: type === 'PATH' ? 'default' : 'text' }}
                />
                {type === 'PATH' && (
                  <button
                    type="button"
                    className="btn btn-outline"
                    onClick={handlePickFolder}
                    disabled={pickingFolder}
                    style={{ whiteSpace: 'nowrap' }}
                  >
                    {pickingFolder ? '선택 중...' : '폴더 선택'}
                  </button>
                )}
              </div>
            </label>

            <label>
              메모 <span style={{ fontSize: 12, color: '#64748b', fontWeight: 400 }}>(선택)</span>
              <input
                type="text"
                value={memo}
                onChange={(e) => setMemo(e.target.value)}
                placeholder="예: 백업 폴더, 로그 폴더 등"
                style={{ marginTop: 6 }}
              />
            </label>

            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <button type="submit" className="btn">예외 추가</button>
              <button type="button" className="btn btn-outline" onClick={refresh} disabled={loading}>
                새로고침
              </button>
              {loading && <span style={{ fontSize: 13, color: '#9ca3af' }}>불러오는 중...</span>}
              {error && <span style={{ fontSize: 13, color: '#f87171' }}>오류: {error.message}</span>}
            </div>
          </form>
        </div>

        {/* 목록 */}
        <div>
          <div style={{ fontSize: 14, fontWeight: 600, color: '#f1f5f9', marginBottom: 10 }}>
            등록된 예외 규칙
            {exceptions.length > 0 && (
              <span style={{ marginLeft: 8, fontSize: 12, color: '#64748b', fontWeight: 400 }}>
                {exceptions.length}건
              </span>
            )}
          </div>

          {exceptions.length === 0 ? (
            <div className="card" style={{ textAlign: 'center', padding: 24 }}>
              <p style={{ fontSize: 13, color: '#64748b', margin: 0 }}>
                등록된 예외 규칙이 없습니다.
              </p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {exceptions.map((ex) => (
                <div key={ex.id} className="exception-item">
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span style={{
                        fontSize: 11,
                        fontWeight: 700,
                        padding: '2px 7px',
                        borderRadius: 4,
                        background: ex.type === 'PATH' ? 'rgba(59,130,246,0.15)' : 'rgba(168,85,247,0.15)',
                        color: ex.type === 'PATH' ? '#60a5fa' : '#c084fc',
                      }}>
                        {ex.type}
                      </span>
                      <span style={{ fontSize: 13, fontWeight: 500, color: '#e2e8f0', fontFamily: 'monospace' }}>
                        {ex.pattern}
                      </span>
                    </div>
                    {ex.memo && (
                      <div style={{ fontSize: 12, color: '#64748b', marginTop: 4, paddingLeft: 2 }}>
                        {ex.memo}
                      </div>
                    )}
                  </div>
                  <button className="btn-icon" onClick={() => handleRemove(ex.id)}>삭제</button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default SettingExceptionsPage;
