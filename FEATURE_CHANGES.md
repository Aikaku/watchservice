# 피처 계산 방식 변경사항 (Feature Calculation Changes)

> 작성일: 2026-03-06
> 대상: XGBoost 랜섬웨어 탐지 모델에 전송되는 9개 피처
> AI 서버 API 호환성: AiPayload 필드명 변경 없음

---

## 개요

3초 윈도우 단위로 집계되는 9개 피처의 계산 정확도를 높이기 위해 5가지 개선을 적용했다.
모든 개선은 "더 위험한 활동 → 더 높은 피처값" 방향을 유지하므로 기존 모델 재학습 없이 적용 가능하다.

---

## 개선 1: EntropyAnalyzer — 3구간 샘플링

**영향 피처**: `entropy_diff_mean`, `file_encrypt_like_count`

**변경 파일**:
- `collector/business/EntropyAnalyzer.java` — `computeMultiSectionEntropy()` 추가
- `collector/FileCollectorService.java` — 호출부 교체 (line 77)

**문제**: 파일 앞부분 4096 bytes만 순차 읽어 엔트로피를 계산했다.
중간·끝부터 암호화하는 랜섬웨어(일부 LockBit 변종)나 PDF/ZIP처럼 헤더가 복잡한 파일에서 엔트로피가 실제보다 낮게 계산되었다.

**해결**:
- 파일 크기 > `totalSampleBytes`(4096)이면 3구간 샘플링으로 전환
  - 앞 40% / 중간 40% / 끝 20%
  - `SeekableByteChannel`을 사용해 각 구간 offset으로 직접 이동 후 읽기
  - 3구간의 바이트 빈도(freq[256])를 합산하여 단일 Shannon 엔트로피 계산
- 파일 크기 <= `totalSampleBytes`이면 기존 순차 읽기 유지 (소형 파일 회귀 없음)

**before**:
```
computeSampleEntropy(path, 4096)  // 앞 4096 bytes만 읽음
```

**after**:
```
computeMultiSectionEntropy(path, 4096)  // 앞40% / 중간40% / 끝20% 샘플링
```

**리스크**: LOW — 소형 파일은 기존 경로 유지

---

## 개선 2: rename 탐지 — ext-추가 패턴 인식

**영향 피처**: `file_rename_count`, `file_delete_count`

**변경 파일**: `analytics/EventWindowAggregator.java` — `detectRenameLikeCountByScore()`

**문제**: WannaCry/LockBit의 전형적 패턴인 `file.doc → file.doc.wncry`(원본 파일명에 확장자 추가)가
기존 score 시스템에서 ext 불일치 + 크기 변화로 score=0이 되어 rename으로 탐지되지 않았다.

**해결**: score 계산에 2가지 조건 추가 (기존 임계 score>=3 유지)

| 조건 | 점수 | 설명 |
|------|------|------|
| `crtPath.startsWith(delPath + ".")` 또는 `startsWith(delPath + "_")` | +3 | ext 추가 패턴 |
| `getFileStem(del).equalsIgnoreCase(getFileStem(crt))` | +2 | 파일 stem 동일 |

- `getFileStem()` private 메서드 추가: basename에서 첫 번째 점 이전 부분 반환
- 패턴 A(+3) 단독으로 임계값 달성 가능

**before**:
```
file.doc → file.doc.wncry  → score=0 (미탐)
```

**after**:
```
file.doc → file.doc.wncry  → score=3 (rename 탐지)
```

**리스크**: MEDIUM — false positive 가능성 있으나 parent dir + ownerKey + 시간 gap 필터가 추가 방어

---

## 개선 3: encrypt_like — before 스냅샷 없을 때 절대 엔트로피 탐지

**영향 피처**: `file_encrypt_like_count`

**변경 파일**: `analytics/EventWindowAggregator.java`, `resources/application.yml`

**문제**: `hasEntropyPair=false`(before 스냅샷 없음) 상태에서는 encrypt_like를 탐지할 수 없었다.
랜섬웨어가 암호화된 내용으로 새 파일을 직접 CREATE하는 경우 탐지 실패.

**해결**: 기존 조건 A에 신규 조건 B를 OR로 추가

```
조건 A (기존): bigEnough && hasEntropyPair && entropyDiff >= 0.30 && (sizeChanged || extChanged)
조건 B (신규): bigEnough && !hasEntropyPair && entropyAfter >= 7.2 && isSuspiciousExt(extAfter)
```

**신규 설정**:
```yaml
watchservice.analytics.encrypt.absolute-entropy-threshold: 7.2
```

**리스크**: MEDIUM — `bigEnough`(4KB 이상) + `isSuspiciousExt` 이중 필터로 false positive 억제

---

## 개선 4: entropy_diff_mean — 유의미한 변화만 평균 산입

**영향 피처**: `entropy_diff_mean`

**변경 파일**: `analytics/EventWindowAggregator.java`, `resources/application.yml`

**문제**: 파일 내용이 거의 변하지 않은 이벤트(entropyDiff ≈ 0)도 평균에 포함되어 실제 암호화 신호가 희석되었다.

**해결**: `Math.abs(entropyDiff) > 0.05`인 경우만 평균에 산입

**before**:
```java
if (hasEntropyPair) {
    entropyDiffSum += entropyDiff;
    entropyDiffCount++;
}
```

**after**:
```java
if (hasEntropyPair && Math.abs(entropyDiff) > entropySignificantDiffThreshold) {
    entropyDiffSum += entropyDiff;
    entropyDiffCount++;
}
```

**신규 설정**:
```yaml
watchservice.analytics.entropy.significant-diff-threshold: 0.05
```

**리스크**: HIGH — `entropy_diff_mean`의 절대값이 전반적으로 상승한다.
기존 학습된 XGBoost 모델의 결정 경계에 영향을 줄 수 있으므로 배포 후 모니터링 필요.
threshold=0.05는 보수적으로 설정하여 영향을 최소화.

---

## 개선 5: random_extension_flag — 균일 확장자 집단 패턴 추가

**영향 피처**: `random_extension_flag`

**변경 파일**: `analytics/EventWindowAggregator.java`, `resources/application.yml`

**문제**: suspicious 확장자 파일 개수(`suspiciousExtCount >= 2`)만으로 flag를 판단했다.
동일한 suspicious 확장자가 여러 파일에 반복 적용되는 집단 패턴(LockBit 균일 확장자 전략)을 별도로 인식하지 못했다.

**해결**: 기존 조건에 균일 확장자 집단 탐지 조건 추가

```java
// CREATE/MODIFY에서 suspicious extAfter 빈도 누적
Map<String, Integer> extAfterFreq = new HashMap<>();
// ...
boolean uniformExtDetected = extAfterFreq.values().stream()
        .anyMatch(cnt -> cnt >= uniformExtMinCount);  // 동일 확장자 3개 이상
randomExtFlag = (suspiciousExtCount >= randomExtMinCount || uniformExtDetected) ? 1 : 0;
```

**신규 설정**:
```yaml
watchservice.analytics.random-ext.uniform-ext-min-count: 3
```

**리스크**: LOW — 이진 피처(0/1), 방향성 동일, 기존 조건도 유지

---

## 변경된 설정값 요약

| 설정 키 | 기본값 | 설명 |
|---------|--------|------|
| `watchservice.analytics.encrypt.absolute-entropy-threshold` | 7.2 | before 스냅샷 없이 CREATE된 파일의 고엔트로피 탐지 임계값 |
| `watchservice.analytics.entropy.significant-diff-threshold` | 0.05 | entropy_diff_mean 산입 최소 변화량 |
| `watchservice.analytics.random-ext.uniform-ext-min-count` | 3 | 균일 확장자 집단 탐지 최소 파일 수 |

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|---------|
| `collector/business/EntropyAnalyzer.java` | `computeMultiSectionEntropy()`, `readSection()` 추가 |
| `collector/FileCollectorService.java` | 엔트로피 계산 호출부 교체 |
| `analytics/EventWindowAggregator.java` | 개선 2~5 구현, @Value 3개, private 메서드 추가 |
| `resources/application.yml` | 신규 설정 키 3개 추가 |
