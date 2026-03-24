@echo off
REM ============================================================
REM build.bat — 전체 빌드 스크립트 (Windows)
REM 사용법:
REM   build.bat              React + Spring Boot JAR만 빌드
REM   build.bat --electron   JRE 번들 + Electron .msi 인스톨러까지 빌드
REM 결과 (기본):    watchservice_be\build\libs\watchservice-agent-*.jar
REM 결과 (--electron): watchservice_electron\dist\*.msi
REM ============================================================

setlocal enabledelayedexpansion

set BUILD_ELECTRON=false
for %%a in (%*) do if "%%a"=="--electron" set BUILD_ELECTRON=true

set SCRIPT_DIR=%~dp0
set FE_DIR=%SCRIPT_DIR%watchservice_fe
set BE_DIR=%SCRIPT_DIR%watchservice_be
set STATIC_DIR=%BE_DIR%\src\main\resources\static
set ELECTRON_DIR=%SCRIPT_DIR%watchservice_electron

echo ==============================
echo  [1/3] React 빌드
echo ==============================
cd /d "%FE_DIR%"
call npm install
if errorlevel 1 ( echo [ERROR] npm install 실패 & exit /b 1 )
call npm run build
if errorlevel 1 ( echo [ERROR] npm run build 실패 & exit /b 1 )

echo.
echo ==============================
echo  [2/3] React 빌드 결과물 복사
echo ==============================
if exist "%STATIC_DIR%" rmdir /s /q "%STATIC_DIR%"
mkdir "%STATIC_DIR%"
xcopy /e /i /q "%FE_DIR%\build\*" "%STATIC_DIR%\"
if errorlevel 1 ( echo [ERROR] 파일 복사 실패 & exit /b 1 )
echo 복사 완료: %STATIC_DIR%

echo.
echo ==============================
echo  [3/3] Spring Boot 빌드
echo ==============================
cd /d "%BE_DIR%"
call gradlew.bat build -x test
if errorlevel 1 ( echo [ERROR] Gradle 빌드 실패 & exit /b 1 )

echo.
echo ==============================
echo  [완료] JAR 빌드 성공
echo ==============================

if "!BUILD_ELECTRON!"=="false" (
  echo.
  echo 실행 방법:
  echo   java -jar watchservice_be\build\libs\watchservice-agent-*.jar
  echo 접속:
  echo   http://localhost:8080
  echo.
  echo Electron 인스톨러 빌드: build.bat --electron
  goto :eof
)

echo.
echo ==============================
echo  [4/5] JRE 번들 생성 (jlink)
echo ==============================
call "%SCRIPT_DIR%jre_build.bat"
if errorlevel 1 ( echo [ERROR] JRE 빌드 실패 & exit /b 1 )

echo.
echo ==============================
echo  [5/5] Electron 인스톨러 빌드
echo ==============================
cd /d "%ELECTRON_DIR%"
call npm install
if errorlevel 1 ( echo [ERROR] npm install 실패 & exit /b 1 )
call npm run build:win
if errorlevel 1 ( echo [ERROR] Electron 빌드 실패 & exit /b 1 )

echo.
echo ==============================
echo  [완료] 전체 빌드 성공
echo ==============================
echo.
echo 인스톨러 위치: watchservice_electron\dist\
echo 배포: 위 .msi 파일을 사용자에게 전달
