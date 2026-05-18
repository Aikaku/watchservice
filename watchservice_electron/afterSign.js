const { execSync } = require('child_process');
const path = require('path');

module.exports = async function (context) {
    if (process.platform !== 'darwin') return;

    const { appOutDir, packager } = context;
    const appName = packager.appInfo.productFilename;
    const appPath = path.join(appOutDir, `${appName}.app`);

    console.log(`[afterSign] adhoc 서명 적용: ${appPath}`);
    execSync(`codesign --deep --force --sign - "${appPath}"`);
    console.log('[afterSign] 완료');
};
