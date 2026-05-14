# Windows 빌드 메모

이 파일을 읽고 있다면 Windows PC에서 `.msi` 인스톨러를 빌드하려는 것이다.

---

## 사전 준비 (없으면 먼저 설치)

| 도구 | 설치 링크 | 확인 명령 |
|------|-----------|-----------|
| Node.js 18+ | https://nodejs.org | `node -v` |
| **JDK 17** (JRE 아님) | https://adoptium.net/temurin/releases/?version=17 → OS: Windows, Arch: x64, Package: **JDK** | `javac -version` |
| Git | https://git-scm.com | `git --version` |
| gh CLI | https://cli.github.com | `gh --version` |

---

## 빌드

**명령 프롬프트(cmd)** 에서 실행 (Git Bash 아님):

```cmd
build.bat --electron
```

완료되면 `watchservice_electron\dist\` 에 `.msi` 파일 생성됨.

---

## GitHub Release 업로드

```cmd
gh auth login
gh release upload v1.0.0 "watchservice_electron/dist/WatchService Agent 1.0.0.msi"
```

> 파일명은 `dist\` 폴더에 실제 생성된 `.msi` 파일명을 그대로 쓴다.

---

## 다운로드 페이지 버튼 활성화

`download_site/index.html` 수정:

```html
<!-- 변경 전 -->
<a id="btn-win" class="btn-download disabled" href="#">
  ⬇ Windows(.msi) — 준비 중
</a>

<!-- 변경 후 -->
<a id="btn-win" class="btn-download"
   href="https://github.com/Aikaku/watchservice/releases/latest/download/WatchService%20Agent%201.0.0.msi">
  ⬇ Windows(.msi) 다운로드
</a>
```

```cmd
git add download_site/index.html
git commit -m "Feat : Windows 다운로드 버튼 활성화"
git push
```

---

## 실패 시 확인

| 오류 메시지 | 원인 | 해결 |
|-------------|------|------|
| `jlink를 찾을 수 없습니다` | JRE 설치됨 (JDK 아님) | JDK 17로 재설치 |
| `JAVA_HOME이 설정되지 않음` | 환경변수 미설정 | 시스템 환경변수에 `JAVA_HOME` = JDK 경로 추가 후 cmd 재시작 |
| `npm install 실패` | Node.js 미설치 | Node.js 18+ 설치 |
