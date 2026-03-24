#!/bin/bash
# ============================================================
# jre_build.sh — jlink으로 최소 JRE 번들 생성 (macOS / Linux)
# 사용법: ./jre_build.sh
# 결과:  watchservice_electron/jre/
#
# 요구사항: JDK 17 이상 (JRE가 아닌 JDK — jlink, jmods 포함)
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JRE_OUT="$SCRIPT_DIR/watchservice_electron/jre"

# ── JAVA_HOME 결정 ──────────────────────────────────────────
if [ -z "$JAVA_HOME" ]; then
  JAVA_BIN=$(which java 2>/dev/null || true)
  if [ -z "$JAVA_BIN" ]; then
    echo "[ERROR] Java가 설치되어 있지 않습니다."
    exit 1
  fi
  # symlink 해소
  JAVA_BIN=$(readlink -f "$JAVA_BIN" 2>/dev/null || realpath "$JAVA_BIN" 2>/dev/null || echo "$JAVA_BIN")
  JAVA_HOME="$(dirname "$(dirname "$JAVA_BIN")")"
fi

JLINK="$JAVA_HOME/bin/jlink"
JMODS="$JAVA_HOME/jmods"

if [ ! -f "$JLINK" ]; then
  echo "[ERROR] jlink를 찾을 수 없습니다: $JLINK"
  echo "  JRE가 아닌 JDK(개발 키트)가 필요합니다."
  echo "  설치: https://adoptium.net"
  exit 1
fi

if [ ! -d "$JMODS" ]; then
  echo "[ERROR] jmods 폴더가 없습니다: $JMODS"
  echo "  JDK 설치를 확인하세요."
  exit 1
fi

# ── Spring Boot 3.x 구동에 필요한 최소 모듈 목록 ────────────
# (Spring Web MVC + Spring Data JDBC + Spring Security +
#  Actuator + NIO WatchService + SQLite JDBC 기준)
MODULES="\
java.base,\
java.compiler,\
java.desktop,\
java.instrument,\
java.logging,\
java.management,\
java.management.rmi,\
java.naming,\
java.net.http,\
java.rmi,\
java.scripting,\
java.security.jgss,\
java.security.sasl,\
java.sql,\
java.xml,\
jdk.httpserver,\
jdk.jfr,\
jdk.management,\
jdk.management.agent,\
jdk.naming.rmi,\
jdk.unsupported,\
jdk.crypto.ec,\
jdk.crypto.cryptoki"

# 줄바꿈 제거
MODULES=$(echo "$MODULES" | tr -d '\\\n ')

echo "=============================="
echo " JRE 번들 생성 (jlink)"
echo "=============================="
echo " JAVA_HOME : $JAVA_HOME"
echo " 출력 경로 : $JRE_OUT"
echo ""

if [ -d "$JRE_OUT" ]; then
  echo " 기존 JRE 폴더 삭제 중..."
  rm -rf "$JRE_OUT"
fi

"$JLINK" \
  --module-path "$JMODS" \
  --add-modules "$MODULES" \
  --strip-debug \
  --no-man-pages \
  --no-header-files \
  --compress=2 \
  --output "$JRE_OUT"

echo ""
echo "=============================="
echo " ✅ JRE 번들 생성 완료"
echo "=============================="
echo " 경로 : $JRE_OUT"
echo " 크기 : $(du -sh "$JRE_OUT" | cut -f1)"
echo ""
echo " 다음 단계:"
echo "   cd watchservice_electron && npm install && npm run build:mac"
