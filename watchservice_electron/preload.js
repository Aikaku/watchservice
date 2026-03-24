// preload.js — contextIsolation 활성화 상태에서 renderer에 안전하게 노출할 API 정의
// 현재는 별도 노출 API 없음. 필요 시 contextBridge.exposeInMainWorld() 사용
const { contextBridge } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  platform: process.platform,
});
