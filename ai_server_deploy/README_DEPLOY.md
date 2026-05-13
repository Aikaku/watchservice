# AI 서버 클라우드 배포 가이드 (Railway / Render)

## 배포 구조

```
AI 서버 (Railway/Render)
  ← POST /api/analyze  (Spring Boot → AI 서버)
  ← POST /predict      (Spring Boot → AI 서버)
```

Spring Boot는 환경변수 `AI_ANALYZE_URL`, `AI_FAMILY_URL`로 AI 서버 주소를 주입받는다.

---

## 1단계: 별도 Git 저장소 생성

Railway/Render는 Git 저장소 단위로 배포하므로, AI 서버 코드를 별도 레포로 분리해야 한다.

```bash
mkdir ai-server && cd ai-server
git init

# 아래 파일들을 이 폴더로 복사
# 원본: 통합프로젝트/ai_server/api_server.py
# 원본: 통합프로젝트/ai_server/artifacts/  (model_xgb.json, features.json, classes.json)
cp ../통합/ai_server/api_server.py .
cp -r ../통합/ai_server/artifacts ./artifacts
cp -r ../통합/ai_server_deploy/configs ./configs

# 이 폴더의 파일들도 복사
cp ../통합/ai_server_deploy/requirements.txt .
cp ../통합/ai_server_deploy/Procfile .
cp ../통합/ai_server_deploy/railway.toml .
```

최종 레포 구조:
```
ai-server/
├── api_server.py
├── requirements.txt
├── Procfile
├── railway.toml
├── configs/
│   └── config.yaml
└── artifacts/
    ├── model_xgb.json
    ├── features.json
    └── classes.json
```

---

## 2단계: Railway 배포

1. [railway.app](https://railway.app) 접속 → New Project → Deploy from GitHub repo
2. 위에서 만든 `ai-server` 레포 선택
3. Variables 탭에서 환경변수 설정:
   ```
   CORS_ALLOWED_ORIGINS=https://your-spring-boot-domain.com
   ```
4. 배포 완료 후 생성된 URL 확인 (예: `https://ai-server-xxx.railway.app`)

---

## 3단계: Spring Boot 환경변수 설정

Electron 앱 배포 시 또는 서버 실행 시 아래 환경변수 주입:

```bash
AI_ANALYZE_URL=https://ai-server-xxx.railway.app/api/analyze
AI_FAMILY_URL=https://ai-server-xxx.railway.app/predict
```

Windows 환경변수 설정:
```
setx AI_ANALYZE_URL "https://ai-server-xxx.railway.app/api/analyze"
setx AI_FAMILY_URL "https://ai-server-xxx.railway.app/predict"
```

macOS/Linux:
```bash
export AI_ANALYZE_URL="https://ai-server-xxx.railway.app/api/analyze"
export AI_FAMILY_URL="https://ai-server-xxx.railway.app/predict"
```

---

## 4단계: 동작 확인

```bash
# 헬스체크
curl https://ai-server-xxx.railway.app/health

# 분석 테스트
curl -X POST https://ai-server-xxx.railway.app/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "fileReadCount": 10,
    "fileWriteCount": 5,
    "fileDeleteCount": 0,
    "fileRenameCount": 0,
    "fileEncryptLikeCount": 0,
    "changedFilesCount": 3,
    "randomExtensionFlag": 0,
    "entropyDiffMean": 0.02,
    "fileSizeDiffMean": 512.0
  }'
```

---

## 주의사항

- `artifacts/` 폴더의 모델 파일은 Git LFS 또는 직접 커밋으로 레포에 포함해야 한다.
  모델 파일이 크다면 Railway의 Volume 또는 외부 스토리지(S3 등) 사용 고려.
- Railway 무료 플랜은 월 5달러 크레딧 제공 (Sleep 모드 있음). 항상 켜두려면 유료 플랜 필요.
- Render 무료 플랜은 15분 비활동 시 Sleep. 첫 요청에 ~30초 지연 발생.
