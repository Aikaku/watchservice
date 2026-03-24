@echo off
REM ============================================================
REM jre_build.bat — jlink으로 최소 JRE 번들 생성 (Windows)
REM 사용법: jre_build.bat
REM 결과:  watchservice_electron\jre\
REM
REM 요구사항: JDK 17 이상 (JRE가 아닌 JDK — jlink, jmods 포함)
REM ============================================================

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set JRE_OUT=%SCRIPT_DIR%watchservice_electron\jre

REM ── JAVA_HOME 결정 ─────────────────────────────────────────
if "%JAVA_HOME%"=="" (
  for /f "delims=" %%i in ('where java 2^>nul') do (
    set JAVA_EXE=%%i
    goto :found_java
  )
  echo [ERROR] Java가 설치되어 있지 않습니다.
  exit /b 1
  :found_java
  for %%i in ("!JAVA_EXE!") do set JAVA_BIN_DIR=%%~dpi
  REM bin\ 제거하여 JAVA_HOME 추론
  set JAVA_HOME=!JAVA_BIN_DIR:~0,-1!
  for %%i in ("!JAVA_HOME!") do set JAVA_HOME=%%~dpi
  set JAVA_HOME=!JAVA_HOME:~0,-1!
)

set JLINK=%JAVA_HOME%\bin\jlink.exe
set JMODS=%JAVA_HOME%\jmods

if not exist "%JLINK%" (
  echo [ERROR] jlink를 찾을 수 없습니다: %JLINK%
  echo   JRE가 아닌 JDK ^(개발 키트^)가 필요합니다.
  echo   설치: https://adoptium.net
  exit /b 1
)

if not exist "%JMODS%" (
  echo [ERROR] jmods 폴더가 없습니다: %JMODS%
  echo   JDK 설치를 확인하세요.
  exit /b 1
)

REM ── Spring Boot 3.x 구동에 필요한 최소 모듈 목록 ───────────
set MODULES=java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.management.rmi,java.naming,java.net.http,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.xml,jdk.httpserver,jdk.jfr,jdk.management,jdk.management.agent,jdk.naming.rmi,jdk.unsupported,jdk.crypto.ec,jdk.crypto.cryptoki

echo ==============================
echo  JRE 번들 생성 (jlink)
echo ==============================
echo  JAVA_HOME : %JAVA_HOME%
echo  출력 경로 : %JRE_OUT%
echo.

if exist "%JRE_OUT%" (
  echo  기존 JRE 폴더 삭제 중...
  rmdir /s /q "%JRE_OUT%"
)

"%JLINK%" ^
  --module-path "%JMODS%" ^
  --add-modules %MODULES% ^
  --strip-debug ^
  --no-man-pages ^
  --no-header-files ^
  --compress=2 ^
  --output "%JRE_OUT%"

if errorlevel 1 (
  echo [ERROR] jlink 실행 실패
  exit /b 1
)

echo.
echo ==============================
echo  [완료] JRE 번들 생성 성공
echo ==============================
echo  경로: %JRE_OUT%
echo.
echo  다음 단계:
echo    cd watchservice_electron
echo    npm install
echo    npm run build:win
