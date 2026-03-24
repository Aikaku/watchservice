const { app, BrowserWindow, Tray, Menu, nativeImage, shell } = require('electron');
const { spawn } = require('child_process');
const path = require('path');
const http = require('http');
const fs = require('fs');

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
  } catch (err) {
    console.error('[Electron] 시작 실패:', err.message);
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.loadFile(path.join(__dirname, 'assets', 'error.html'));
    }
  }
});

// ──────────────────────────────────────────────
// 앱 종료 시 Spring Boot 프로세스 정리
// ──────────────────────────────────────────────
app.on('before-quit', () => {
  app.isQuiting = true;
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
