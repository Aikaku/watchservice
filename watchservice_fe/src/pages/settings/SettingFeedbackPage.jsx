import React, { useState } from 'react';
import { sendFeedback } from '../../api/SettingApi';
import { useToast } from '../../components/common/Toast';

/*
 * 함수 이름 : SettingFeedbackPage
 * 기능 : 문의/피드백 제출 페이지 컴포넌트. 버그 제보나 문의 내용을 입력하여 서버로 전송한다.
 * 매개변수 : 없음
 * 반환값 : JSX.Element
 * 작성 날짜 : 2026/03/08
 * 작성자 : 이상혁
 */
function SettingFeedbackPage() {
  const toast = useToast();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(false);
  const [note, setNote] = useState('');

  /*
   * 함수 이름 : handleSubmit
   * 기능 : 피드백 폼 제출 시 입력된 내용을 서버로 전송한다.
   * 매개변수 : e - 폼 제출 이벤트 객체
   * 반환값 : 없음
   * 작성 날짜 : 2026/03/08
   * 작성자 : 이상혁
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!content.trim()) {
      toast('내용을 입력해주세요.', 'warn');
      return;
    }
    setLoading(true);
    setNote('');
    try {
      const res = await sendFeedback({ name, email, content });
      setNote(`전송 완료${res?.ticketId ? ` (티켓: ${res.ticketId})` : ''}`);
      setContent('');
    } catch {
      setNote('전송 실패. 내용을 복사하여 직접 전달해 주세요.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container">
      <h1>문의 / 피드백</h1>
      <p className="page-description">버그 제보나 문의 내용을 전송합니다.</p>

      <div style={{ maxWidth: 540 }}>
        <div className="card">
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <label>
              이름 <span style={{ fontSize: 12, color: '#64748b', fontWeight: 400 }}>(선택)</span>
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="홍길동"
                style={{ marginTop: 6 }}
              />
            </label>

            <label>
              이메일 <span style={{ fontSize: 12, color: '#64748b', fontWeight: 400 }}>(선택)</span>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="example@gmail.com"
                style={{ marginTop: 6 }}
              />
            </label>

            <label>
              내용
              <textarea
                rows={6}
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder="문제 상황, 재현 방법, 스크린샷 설명 등을 적어주세요."
                style={{ marginTop: 6 }}
              />
            </label>

            <button className="btn" type="submit" disabled={loading}>
              {loading ? '전송 중...' : '제출'}
            </button>

            {note && (
              <p style={{ fontSize: 13, color: '#94a3b8', margin: 0 }}>{note}</p>
            )}
          </form>
        </div>
      </div>
    </div>
  );
}

export default SettingFeedbackPage;
