#!/bin/zsh
# ============================================================
# 스크립트 이름 : simulate_ransomware.sh
# 기능 : WatchService Agent의 랜섬웨어 탐지 데모/테스트용 시뮬레이터
#        감시 폴더 안에서 파일 생성 → 고엔트로피 덮어쓰기 → 의심 확장자 rename → 일부 삭제
#        를 짧은 시간(3초 윈도우) 안에 발생시켜 fileEncryptLikeCount / randomExtensionFlag 등을 트리거한다.
# 사용법 : ./simulate_ransomware.sh [감시폴더경로] [파일개수]
#   예) ./simulate_ransomware.sh /Users/me/Desktop/watch_test 30
# 작성 날짜 : 2026/06/16
# 작성자 : 이상혁
# ============================================================

set -e

TARGET_DIR="${1:-./watch_test}"
FILE_COUNT="${2:-30}"
SUFFIX=".locked"   # randomExtMinCount(기본 2개 이상) 트리거용 의심 확장자

mkdir -p "$TARGET_DIR"
echo "▶ 대상 폴더: $TARGET_DIR"
echo "▶ 파일 개수: $FILE_COUNT"

# ------------------------------------------------------------
# 1단계: 정상 파일처럼 보이는 원본 생성 (낮은 엔트로피, 텍스트)
#   → 잠시 후 baseline으로 잡히도록 약간의 시간차를 둠
# ------------------------------------------------------------
echo "▶ 1) 정상 원본 파일 생성 중..."
for i in $(seq 1 "$FILE_COUNT"); do
    f="$TARGET_DIR/document_${i}.txt"
    {
        echo "이것은 정상적인 업무 문서입니다."
        echo "파일 번호: ${i}"
        yes "일반 텍스트 내용 반복 일반 텍스트 내용 반복" | head -n 20
    } > "$f"
done

sleep 4   # baseline이 먼저 메모리에 잡히도록 윈도우 한 번 흘려보냄 (3초 윈도우 + 여유)

# ------------------------------------------------------------
# 2단계: 암호화 버스트 시뮬레이션
#   - /dev/urandom으로 고엔트로피(엔트로피 ≈ 8.0) 데이터로 덮어씀 → fileEncryptLikeCount↑
#   - 곧바로 의심 확장자로 rename → fileRenameCount↑, randomExtensionFlag=1
#   - 전체를 최대한 빠르게 실행해서 3초 윈도우 안에 몰리도록 함 → "암호화 버스트 패턴"
# ------------------------------------------------------------
echo "▶ 2) 암호화 버스트 시뮬레이션 시작 (고엔트로피 덮어쓰기 + rename)..."
for i in $(seq 1 "$FILE_COUNT"); do
    f="$TARGET_DIR/document_${i}.txt"
    # 원본 파일 크기(4KB 이상)만큼 랜덤 바이트로 덮어씀 → 엔트로피 급상승
    dd if=/dev/urandom of="$f" bs=1024 count=8 2>/dev/null
    # 즉시 의심 확장자로 이름 변경 (DELETE+CREATE로 감지되어 rename으로 추론됨)
    mv "$f" "${f}${SUFFIX}"
done

# ------------------------------------------------------------
# 3단계: 일부 파일 삭제 (fileDeleteCount↑)
# ------------------------------------------------------------
echo "▶ 3) 일부 파일 삭제 중..."
for i in $(seq 1 5); do
    rm -f "$TARGET_DIR/document_${i}.txt${SUFFIX}"
done

echo "▶ 완료. 대시보드에서 DANGER 알림이 뜨는지 확인하세요."
echo "▶ 정리하려면: rm -rf $TARGET_DIR"
