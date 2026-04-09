const { app, BrowserWindow, Tray, Menu, nativeImage, shell, screen, ipcMain } = require('electron');
const { spawn } = require('child_process');
const path = require('path');
const http = require('http');
const https = require('https');
const fs = require('fs');

// 업데이트 확인용 GitHub 저장소 (실제 저장소 경로로 변경)
const GITHUB_REPO = process.env.GITHUB_REPO || 'your-org/watchservice-agent';

// ──────────────────────────────────────────────
// 설정
// ──────────────────────────────────────────────
const PORT = 8080;
const SPRING_READY_POLL_MS = 500;   // 헬스체크 폴링 간격
const SPRING_READY_TIMEOUT_MS = 60000; // 최대 대기 시간 (60초)

let mainWindow = null;
let tray = null;
let springProcess = null;

// ──────────────────────────────────────────────
// 긴급 경보 상태
// ──────────────────────────────────────────────
let emergencyWindow = null;
let lastDangerNotificationId = -1;
let dangerPollInterval = null;

// ──────────────────────────────────────────────
// Spring Boot JAR 경로 결정
// ──────────────────────────────────────────────
function getJarPath() {
  // 패키징된 앱: extraResources에서 복사된 위치
  if (app.isPackaged) {
    const resourcesPath = process.resourcesPath;
    const files = fs.readdirSync(resourcesPath).filter(f => f.endsWith('.jar') && !f.includes('plain'));
    if (files.length === 0) throw new Error('JAR 파일을 찾을 수 없습니다: ' + resourcesPath);
    return path.join(resourcesPath, files[0]);
  }
  // 개발 환경: 상위 폴더의 Gradle 빌드 결과물
  const libsDir = path.join(__dirname, '..', 'watchservice_be', 'build', 'libs');
  const files = fs.readdirSync(libsDir).filter(f => f.endsWith('.jar') && !f.includes('plain'));
  if (files.length === 0) throw new Error('JAR 없음. 먼저 build.sh를 실행하세요: ' + libsDir);
  return path.join(libsDir, files[0]);
}

// ──────────────────────────────────────────────
// JRE 경로 결정
// ──────────────────────────────────────────────
function getJavaPath() {
  if (app.isPackaged) {
    const jreDir = path.join(process.resourcesPath, 'jre');
    const javaBin = process.platform === 'win32' ? 'java.exe' : 'java';
    const javaPath = path.join(jreDir, 'bin', javaBin);
    if (fs.existsSync(javaPath)) return javaPath;
  }
  // 개발 환경 또는 JRE 번들 없으면 시스템 java 사용
  return 'java';
}

// ──────────────────────────────────────────────
// 아이콘 로드 (파일 없으면 빈 이미지 반환)
// ──────────────────────────────────────────────
function loadIcon(filename) {
  const iconPath = path.join(__dirname, 'assets', filename);
  if (fs.existsSync(iconPath)) {
    return nativeImage.createFromPath(iconPath);
  }
  console.warn('[Electron] 아이콘 파일 없음:', iconPath);
  return nativeImage.createEmpty();
}

// ──────────────────────────────────────────────
// Spring Boot 시작
// ──────────────────────────────────────────────
function startSpringBoot() {
  const jarPath = getJarPath();
  const javaPath = getJavaPath();

  console.log('[Electron] Spring Boot 시작:', jarPath);
  console.log('[Electron] Java 경로:', javaPath);

  springProcess = spawn(javaPath, ['-jar', jarPath], {
    stdio: ['ignore', 'pipe', 'pipe'],
    detached: false,
  });

  springProcess.stdout.on('data', (data) => {
    console.log('[Spring]', data.toString().trim());
  });

  springProcess.stderr.on('data', (data) => {
    console.error('[Spring ERR]', data.toString().trim());
  });

  springProcess.on('exit', (code) => {
    console.log('[Electron] Spring Boot 종료. code=', code);
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.loadFile(path.join(__dirname, 'assets', 'error.html'))
        .catch(() => {});
    }
  });
}

// ──────────────────────────────────────────────
// Spring Boot 준비 완료 대기 (헬스체크 폴링)
// ──────────────────────────────────────────────
function waitForSpring() {
  return new Promise((resolve, reject) => {
    const start = Date.now();

    const check = () => {
      const req = http.get(`http://localhost:${PORT}/actuator/health`, (res) => {
        if (res.statusCode === 200) {
          resolve();
        } else {
          retry();
        }
      });
      req.on('error', retry);
      req.setTimeout(1000, () => { req.destroy(); retry(); });
    };

    const retry = () => {
      if (Date.now() - start > SPRING_READY_TIMEOUT_MS) {
        reject(new Error('Spring Boot 시작 타임아웃'));
        return;
      }
      setTimeout(check, SPRING_READY_POLL_MS);
    };

    check();
  });
}

// ──────────────────────────────────────────────
// 메인 창 생성
// ──────────────────────────────────────────────
function createWindow() {
  const icon = process.platform === 'win32'
    ? loadIcon('icon.ico')
    : loadIcon('icon.png');

  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 900,
    minHeight: 600,
    title: 'WatchService Agent',
    icon: icon.isEmpty() ? undefined : icon,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });

  mainWindow.loadURL(`http://localhost:${PORT}`);

  // 창 닫기 → 트레이로 최소화 (앱 종료 아님)
  mainWindow.on('close', (e) => {
    if (!app.isQuiting) {
      e.preventDefault();
      mainWindow.hide();
    }
  });

  // 외부 링크는 기본 브라우저로 열기
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });
}

// ──────────────────────────────────────────────
// 시스템 트레이
// ──────────────────────────────────────────────
function createTray() {
  const icon = process.platform === 'win32'
    ? loadIcon('tray.ico')
    : loadIcon('tray.png');

  tray = new Tray(icon.isEmpty() ? loadIcon('icon.png') : icon);

  const buildContextMenu = () => {
    const loginItem = app.getLoginItemSettings();
    return Menu.buildFromTemplate([
      {
        label: '대시보드 열기',
        click: () => {
          if (mainWindow) {
            mainWindow.show();
            mainWindow.focus();
          }
        },
      },
      { type: 'separator' },
      {
        label: loginItem.openAtLogin ? '시작 프로그램 해제' : '시작 프로그램 등록',
        click: () => {
          const current = app.getLoginItemSettings().openAtLogin;
          app.setLoginItemSettings({ openAtLogin: !current });
          tray.setContextMenu(buildContextMenu());
        },
      },
      { type: 'separator' },
      {
        label: '종료',
        click: () => {
          app.isQuiting = true;
          app.quit();
        },
      },
    ]);
  };

  tray.setToolTip('WatchService Agent — 감시 중');
  tray.setContextMenu(buildContextMenu());

  tray.on('double-click', () => {
    if (mainWindow) {
      mainWindow.show();
      mainWindow.focus();
    }
  });
}

// ──────────────────────────────────────────────
// IPC 핸들러
// ──────────────────────────────────────────────
ipcMain.handle('get-app-version', () => app.getVersion());

ipcMain.handle('check-for-updates', () => {
  return new Promise((resolve) => {
    const currentVersion = app.getVersion();
    const url = `https://api.github.com/repos/${GITHUB_REPO}/releases/latest`;

    const req = https.get(url, { headers: { 'User-Agent': 'WatchService-Agent' } }, (res) => {
      let body = '';
      res.on('data', (chunk) => { body += chunk; });
      res.on('end', () => {
        try {
          const json = JSON.parse(body);
          if (json.message === 'Not Found' || !json.tag_name) {
            resolve({ currentVersion, latestVersion: null, downloadUrl: null, hasUpdate: false, error: '릴리즈 없음' });
            return;
          }
          const latestVersion = json.tag_name.replace(/^v/, '');
          resolve({
            currentVersion,
            latestVersion,
            downloadUrl: json.html_url || null,
            hasUpdate: latestVersion !== currentVersion,
          });
        } catch {
          resolve({ currentVersion, latestVersion: null, downloadUrl: null, hasUpdate: false, error: '응답 파싱 실패' });
        }
      });
    });
    req.on('error', () => {
      resolve({ currentVersion, latestVersion: null, downloadUrl: null, hasUpdate: false, error: '네트워크 오류' });
    });
    req.setTimeout(8000, () => {
      req.destroy();
      resolve({ currentVersion, latestVersion: null, downloadUrl: null, hasUpdate: false, error: '타임아웃' });
    });
  });
});

// ──────────────────────────────────────────────
// 앱 시작
// ──────────────────────────────────────────────
app.whenReady().then(async () => {
  createTray();

  // 패키징된 앱 첫 실행 시 시작 프로그램 자동 등록
  if (app.isPackaged && !app.getLoginItemSettings().openAtLogin) {
    app.setLoginItemSettings({ openAtLogin: true });
  }

  // 로딩 창 표시
  mainWindow = new BrowserWindow({
    width: 420,
    height: 260,
    frame: false,
    resizable: false,
    center: true,
    icon: process.platform === 'win32'
      ? (fs.existsSync(path.join(__dirname, 'assets', 'icon.ico'))
          ? path.join(__dirname, 'assets', 'icon.ico') : undefined)
      : (fs.existsSync(path.join(__dirname, 'assets', 'icon.png'))
          ? path.join(__dirname, 'assets', 'icon.png') : undefined),
    webPreferences: { contextIsolation: true },
  });
  mainWindow.loadFile(path.join(__dirname, 'assets', 'loading.html'));

  try {
    startSpringBoot();
    await waitForSpring();

    // Spring Boot 준비 완료 → 메인 창으로 전환
    mainWindow.close();
    mainWindow = null;
    createWindow();
    startDangerPolling();
  } catch (err) {
    console.error('[Electron] 시작 실패:', err.message);
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.loadFile(path.join(__dirname, 'assets', 'error.html'));
    }
  }
});

// ──────────────────────────────────────────────
// 긴급 전체 화면 경보
// ──────────────────────────────────────────────
function showEmergencyWindow(notification) {
  if (emergencyWindow && !emergencyWindow.isDestroyed()) return; // 중복 방지

  const { width, height } = screen.getPrimaryDisplay().workAreaSize;

  const params = new URLSearchParams({
    detectedAt: notification.createdAt || '',
    family: notification.familyName || '',
    guidance: notification.guidance ? notification.guidance.slice(0, 120) : '',
  });

  emergencyWindow = new BrowserWindow({
    width,
    height,
    fullscreen: true,
    alwaysOnTop: true,
    frame: false,
    skipTaskbar: false,
    webPreferences: { contextIsolation: true },
  });

  emergencyWindow.loadFile(
    path.join(__dirname, 'assets', 'emergency.html'),
    { query: Object.fromEntries(params) }
  );

  emergencyWindow.on('closed', () => { emergencyWindow = null; });
}

function startDangerPolling() {
  if (dangerPollInterval) return;

  dangerPollInterval = setInterval(() => {
    const req = http.get(
      `http://localhost:${PORT}/notifications?level=DANGER&size=1&page=1&sort=createdAt,desc`,
      { headers: { Accept: 'application/json' } },
      (res) => {
        let body = '';
        res.on('data', (chunk) => { body += chunk; });
        res.on('end', () => {
          try {
            const json = JSON.parse(body);
            // ApiResponse 래퍼 자동 unwrap
            const payload = (json && 'data' in json) ? json.data : json;
            const items = payload?.items ?? [];
            if (items.length === 0) return;

            const latest = items[0];
            const latestId = latest.id ?? latest.notificationId;
            if (latestId === undefined || latestId === null) return;

            if (lastDangerNotificationId === -1) {
              // 첫 폴링 — 기준 ID 저장만, 경보 없음
              lastDangerNotificationId = latestId;
              return;
            }

            if (latestId !== lastDangerNotificationId) {
              lastDangerNotificationId = latestId;
              showEmergencyWindow(latest);
            }
          } catch (_) { /* JSON 파싱 실패 무시 */ }
        });
      }
    );
    req.on('error', () => { /* 폴링 오류 무시 */ });
    req.setTimeout(2000, () => req.destroy());
  }, 3000); // 3초 간격
}

// ──────────────────────────────────────────────
// 앱 종료 시 Spring Boot 프로세스 정리
// ──────────────────────────────────────────────
app.on('before-quit', () => {
  app.isQuiting = true;
  if (dangerPollInterval) { clearInterval(dangerPollInterval); dangerPollInterval = null; }
  if (springProcess) {
    console.log('[Electron] Spring Boot 종료 중...');
    springProcess.kill('SIGTERM');
  }
});

app.on('window-all-closed', () => {
  // macOS: Dock 아이콘 클릭으로 재실행 허용 (트레이는 유지)
  if (process.platform !== 'darwin') {
    // Windows/Linux는 트레이로만 유지 — 종료는 트레이 메뉴에서
  }
});

app.on('activate', () => {
  // macOS Dock 클릭
  if (mainWindow) {
    mainWindow.show();
  }
});
