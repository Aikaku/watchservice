# ============================================================
# 스크립트 이름 : eval_model.py
# 기능 : 저장된 XGBoost 모델(ai_server/artifacts/model_xgb.json)을
#        학습 스크립트(ai_server/untitled21.py)와 동일한 테스트셋
#        (test_size=0.2, stratify, random_state=42)으로 재평가하여
#        정확도·ROC-AUC·PR-AUC·혼동행렬·오탐률·피처 중요도를 출력한다.
#        ai_server/ 폴더 파일은 읽기만 한다.
# 사용법 : python3 scripts/eval_model.py [데이터CSV] [모델JSON]
#   의존성 : pip install pandas scikit-learn xgboost
# 작성 날짜 : 2026/11/17
# 작성자 : 이상혁
# ============================================================
import sys
import time

import pandas as pd
from sklearn.metrics import (
    accuracy_score,
    average_precision_score,
    classification_report,
    confusion_matrix,
    roc_auc_score,
)
from sklearn.model_selection import train_test_split
from xgboost import XGBClassifier

DATA_PATH = sys.argv[1] if len(sys.argv) > 1 else "ai_server/synthetic_ransomware_dataset_30000.csv"
MODEL_PATH = sys.argv[2] if len(sys.argv) > 2 else "ai_server/artifacts/model_xgb.json"

FEATURES = [
    "file_read_count",
    "file_write_count",
    "file_delete_count",
    "file_rename_count",
    "file_encrypt_like_count",
    "changed_files_count",
    "random_extension_flag",
    "entropy_diff_mean",
    "file_size_diff_mean",
]

# 1) 데이터 로드 및 학습 때와 동일한 분할
df = pd.read_csv(DATA_PATH)
X = df[FEATURES]
y = (df["label"] == "ransomware").astype(int)
print("전체 데이터:", len(df), "| 라벨 분포(0=benign, 1=ransomware):", y.value_counts().to_dict())

_, X_test, _, y_test = train_test_split(X, y, test_size=0.2, stratify=y, random_state=42)

# 2) 저장된 모델 로드 및 예측
model = XGBClassifier()
model.load_model(MODEL_PATH)

start = time.perf_counter()
proba = model.predict_proba(X_test)[:, 1]
infer_us = (time.perf_counter() - start) / len(X_test) * 1e6
pred = (proba >= 0.5).astype(int)

# 3) 지표 출력
print(f"\n테스트셋: {len(X_test)}건")
print(f"Accuracy : {accuracy_score(y_test, pred):.4f}")
print(f"ROC-AUC  : {roc_auc_score(y_test, proba):.4f}")
print(f"PR-AUC   : {average_precision_score(y_test, proba):.4f}")
print("\n=== Classification Report ===")
print(classification_report(y_test, pred, target_names=["benign", "ransomware"], digits=4))
print("=== Confusion Matrix [[TN, FP], [FN, TP]] ===")
print(confusion_matrix(y_test, pred).tolist())

# 4) 시스템 판정 임계값별 오탐률/탐지율 (WARNING ≥ 0.5, DANGER ≥ 0.7)
print("\n=== 임계값별 오탐률(FPR) / 탐지율(TPR) ===")
for th in (0.5, 0.7):
    tn, fp, fn, tp = confusion_matrix(y_test, (proba >= th).astype(int)).ravel()
    print(f"threshold {th}: FPR={fp / (fp + tn):.4f}  TPR={tp / (tp + fn):.4f}")

print(f"\n1건당 평균 추론 시간: {infer_us:.2f} µs (모델 연산만, 네트워크 제외)")

# 5) 피처 중요도 (gain 기준, 합계 1로 정규화)
print("\n=== Feature Importance (gain) ===")
gain = model.get_booster().get_score(importance_type="gain")
names = {f"f{i}": n for i, n in enumerate(FEATURES)}
total = sum(gain.values())
for key, value in sorted(gain.items(), key=lambda kv: -kv[1]):
    print(f"{names.get(key, key):25s} : {value / total:.4f}")
