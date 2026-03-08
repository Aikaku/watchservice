/**
 * 파일 이름 : UseWatchedFolders.js
 * 기능 : 감시 폴더 관리를 위한 커스텀 훅. 폴더 목록 조회, 추가, 삭제 기능을 제공한다.
 * 작성 날짜 : 2025/12/17
 * 작성자 : 시스템
 */
import { useCallback, useEffect, useState } from 'react';
import {
  fetchWatchedFolders,
  createWatchedFolder,
  deleteWatchedFolder,
} from '../api/SettingApi';

/**
 * 함수 이름 : guessNameFromPath
 * 기능 : 경로 문자열에서 폴더 이름을 추출한다.
 * 매개변수 : path - 파일 경로 문자열
 * 반환값 : string - 추출된 폴더 이름
 * 작성 날짜 : 2025/12/17
 * 작성자 : 시스템
 */
function guessNameFromPath(path) {
  if (!path) return '폴더';
  const parts = String(path).split(/[/\\]+/).filter(Boolean);
  return parts.length ? parts[parts.length - 1] : '폴더';
}

/**
 * 함수 이름 : useWatchedFolders
 * 기능 : 감시 폴더 관리를 위한 커스텀 훅.
 * 매개변수 : 없음
 * 반환값 : Object - 폴더 목록, 로딩 상태, 에러, 새로고침 함수, 폴더 추가/삭제 함수를 포함한 객체
 * 작성 날짜 : 2025/12/17
 * 작성자 : 시스템
 */
export function useWatchedFolders() {
  const [folders, setFolders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const refresh = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await fetchWatchedFolders();
      const list = Array.isArray(data) ? data : (data?.items ?? []);

      setFolders(
        list.map((it) => ({
          id: it.id ?? it.folderId ?? it.path,
          name: it.name ?? it.folderName ?? guessNameFromPath(it.path),
          path: it.path,
        }))
      );
    } catch (e) {
      setError(e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  // path: 실제 경로, name: 표시 이름 (비어있으면 경로에서 자동 추출)
  const addFolder = useCallback(async (path, name) => {
    const displayName = (name && name.trim()) ? name.trim() : guessNameFromPath(path);
    await createWatchedFolder({ name: displayName, path });
    await refresh();
  }, [refresh]);

  const removeFolder = useCallback(async (id) => {
    await deleteWatchedFolder(id);
    await refresh();
  }, [refresh]);

  return { folders, loading, error, refresh, addFolder, removeFolder };
}
