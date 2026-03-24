#!/bin/bash
# ============================================================
# build.sh — 전체 빌드 스크립트 (macOS / Linux)
# 사용법:
#   ./build.sh           # React + Spring Boot JAR만 빌드
#   ./build.sh --electron  # JRE 번들 + Electron .dmg 인스톨러까지 빌드
# 결과 (기본):  watchservice_be/build/libs/watchservice-agent-*.jar
# 결과 (--electron): watchservice_electron/dist/*.dmg
# ============================================================

set -e  # 오류 발생 시 즉시 중단

BUILD_ELECTRON=false
for arg in "$@"; do
  [ "$arg" = "--electron" ] && BUILD_ELECTRON=true
done

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FE_DIR="$SCRIPT_DIR/watchservice_fe"
BE_DIR="$SCRIPT_DIR/watchservice_be"
STATIC_DIR="$BE_DIR/src/main/resources/static"
ELECTRON_DIR="$SCRIPT_DIR/watchservice_electron"

echo "=============================="
echo " [1/3] React 빌드"
echo "=============================="
cd "$FE_DIR"
npm install
npm run build

echo ""
echo "=============================="
echo " [2/3] React 빌드 결과물 복사"
echo "=============================="
rm -rf "$STATIC_DIR"
mkdir -p "$STATIC_DIR"
cp -r "$FE_DIR/build/." "$STATIC_DIR/"
echo "복사 완료: $STATIC_DIR"

echo ""
echo "=============================="
echo " [3/3] Spring Boot 빌드"
echo "=============================="
cd "$BE_DIR"
./gradlew build -x test

echo ""
echo "=============================="
echo " ✅ JAR 빌드 완료"
echo "=============================="
JAR_PATH=$(find "$BE_DIR/build/libs" -name "*.jar" ! -name "*plain*" | head -1)
echo " JAR: $JAR_PATH"

if [ "$BUILD_ELECTRON" = false ]; then
  echo ""
  echo " 실행 방법:"
  echo "   java -jar $JAR_PATH"
  echo " 접속:"
  echo "   http://localhost:8080"
  echo ""
  echo " Electron 인스톨러 빌드: ./build.sh --electron"
  exit 0
fi

echo ""
echo "=============================="
echo " [4/5] JRE 번들 생성 (jlink)"
echo "=============================="
"$SCRIPT_DIR/jre_build.sh"

echo ""
echo "=============================="
echo " [5/5] Electron 인스톨러 빌드"
echo "=============================="
cd "$ELECTRON_DIR"
npm install
npm run build:mac

echo ""
echo "=============================="
echo " ✅ 전체 빌드 완료"
echo "=============================="
DMG_PATH=$(find "$ELECTRON_DIR/dist" -name "*.dmg" | head -1)
echo " 인스톨러: $DMG_PATH"
echo ""
echo " 배포: 위 .dmg 파일을 사용자에게 전달"
