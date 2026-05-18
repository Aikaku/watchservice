const { execSync } = require('child_process');
const path = require('path');
const fs = require('fs');

// electron-builder DMG 생성 과정에서 서명이 깨지는 문제를 우회:
// 서명이 검증된 mac-arm64 앱으로 DMG를 직접 재생성한다.
module.exports = async function (buildResult) {
  if (process.platform !== 'darwin') return buildResult.artifactPaths;

  const dmg = buildResult.artifactPaths.find(p => p.endsWith('.dmg'));
  if (!dmg) return buildResult.artifactPaths;

  const distDir = path.dirname(dmg);
  const appPath = path.join(distDir, 'mac-arm64', 'WatchService Agent.app');

  if (!fs.existsSync(appPath)) {
    console.warn('[afterAllArtifactBuild] mac-arm64 앱 없음, 스킵');
    return buildResult.artifactPaths;
  }

  console.log('[afterAllArtifactBuild] 서명된 앱으로 DMG 재생성 중...');

  const tempDir = path.join(distDir, '.dmg-staging');
  if (fs.existsSync(tempDir)) execSync(`rm -rf "${tempDir}"`);
  fs.mkdirSync(tempDir);

  execSync(`cp -Rp "${appPath}" "${tempDir}/"`);
  execSync(`ln -sf /Applications "${tempDir}/Applications"`);

  fs.unlinkSync(dmg);

  execSync(
    `hdiutil create -volname "WatchService Agent 1.0.0" -srcfolder "${tempDir}" -ov -format UDZO -o "${dmg}"`,
    { stdio: 'inherit' }
  );

  execSync(`rm -rf "${tempDir}"`);

  console.log('[afterAllArtifactBuild] DMG 재생성 완료:', path.basename(dmg));
  return buildResult.artifactPaths;
};
