const { execSync } = require('child_process');
const path = require('path');

function isMachO(filePath) {
  try {
    return execSync(`file "${filePath}"`, { encoding: 'utf8' }).includes('Mach-O');
  } catch { return false; }
}

module.exports = async function (context) {
  if (process.platform !== 'darwin') return;

  const { appOutDir, packager } = context;
  const appName = packager.appInfo.productFilename;
  const appPath = path.join(appOutDir, `${appName}.app`);

  console.log(`[afterSign] 서명 시작: ${appPath}`);

  // JRE 내부 Mach-O 바이너리를 --deep 전에 개별 서명
  const jrePath = path.join(appPath, 'Contents', 'Resources', 'jre');
  try {
    const files = execSync(
      `find "${jrePath}" -type f \\( -name "*.dylib" -o -name "*.so" -o -perm +0111 \\) 2>/dev/null`,
      { encoding: 'utf8' }
    ).trim().split('\n').filter(Boolean);

    for (const f of files) {
      if (isMachO(f)) {
        try {
          execSync(`codesign --force --sign - "${f}"`, { stdio: 'pipe' });
        } catch (e) {
          console.warn(`[afterSign] 개별 서명 실패 (무시): ${path.basename(f)}`);
        }
      }
    }
    console.log(`[afterSign] JRE 바이너리 서명 완료 (${files.length}개 대상)`);
  } catch (e) {
    console.warn('[afterSign] JRE 경로 없음, 스킵:', e.message);
  }

  // 앱 번들 전체 서명
  execSync(`codesign --deep --force --sign - "${appPath}"`);
  console.log('[afterSign] 완료');
};
