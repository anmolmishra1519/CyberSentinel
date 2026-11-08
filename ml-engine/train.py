"""
CyberSentinel AI - Model Training
==================================
Loads network_flows_dataset.csv, cleans it, engineers features, trains a
Random Forest classifier, evaluates it honestly (no cherry-picked metrics),
and saves the trained model + label encoder with joblib for FastAPI to load.

Run: python train.py
"""

import sys
import os
import json
import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import (
    accuracy_score, precision_score, recall_score, f1_score,
    confusion_matrix, classification_report,
)

sys.path.append(os.path.dirname(__file__))
from preprocessing.feature_engineering import engineer_dataframe, FEATURE_COLUMNS

DATA_PATH = os.path.join(os.path.dirname(__file__), "data", "network_flows_dataset.csv")
MODEL_DIR = os.path.join(os.path.dirname(__file__), "models")
os.makedirs(MODEL_DIR, exist_ok=True)


def main():
    print(f"Loading dataset from {DATA_PATH} ...")
    df = pd.read_csv(DATA_PATH)
    print(f"Loaded {len(df)} raw rows.")

    # ---- Data validation / cleaning ----
    before = len(df)
    df = df.dropna(subset=["sourceIP", "destinationIP", "protocol", "label"])
    df = df[df["packetCount"] > 0]
    df = df[df["flowDuration"] > 0]
    print(f"Dropped {before - len(df)} invalid/incomplete rows during cleaning.")

    df = engineer_dataframe(df)

    label_encoder = LabelEncoder()
    y = label_encoder.fit_transform(df["label"])
    X = df[FEATURE_COLUMNS].astype(float).values

    print("Classes:", list(label_encoder.classes_))

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )
    print(f"Train size: {len(X_train)}  Test size: {len(X_test)}")

    model = RandomForestClassifier(
        n_estimators=200,
        max_depth=14,
        min_samples_leaf=3,
        class_weight="balanced",
        random_state=42,
        n_jobs=-1,
    )
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)

    acc = accuracy_score(y_test, y_pred)
    prec = precision_score(y_test, y_pred, average="weighted", zero_division=0)
    rec = recall_score(y_test, y_pred, average="weighted", zero_division=0)
    f1 = f1_score(y_test, y_pred, average="weighted", zero_division=0)
    cm = confusion_matrix(y_test, y_pred).tolist()
    report = classification_report(
        y_test, y_pred, target_names=label_encoder.classes_, zero_division=0
    )

    print("\n=== EVALUATION (held-out test set, 20%) ===")
    print(f"Accuracy : {acc:.4f}")
    print(f"Precision: {prec:.4f}")
    print(f"Recall   : {rec:.4f}")
    print(f"F1-score : {f1:.4f}")
    print("\nConfusion matrix (rows=actual, cols=predicted):")
    print(f"Classes: {list(label_encoder.classes_)}")
    for row in cm:
        print(row)
    print("\nClassification report:\n", report)

    importances = dict(zip(FEATURE_COLUMNS, model.feature_importances_.tolist()))
    print("\nFeature importances:")
    for k, v in sorted(importances.items(), key=lambda x: -x[1]):
        print(f"  {k:35s} {v:.4f}")

    # ---- Per-class feature statistics, used later for explainability ----
    class_stats = {}
    for cls in label_encoder.classes_:
        mask = df["label"] == cls
        sub = df.loc[mask, FEATURE_COLUMNS]
        class_stats[cls] = {
            "mean": sub.mean().to_dict(),
            "std": (sub.std() + 1e-6).to_dict(),
        }
    with open(os.path.join(MODEL_DIR, "class_feature_stats.json"), "w") as f:
        json.dump(class_stats, f, indent=2)

    # ---- Save artifacts ----
    joblib.dump(model, os.path.join(MODEL_DIR, "random_forest_model.joblib"))
    joblib.dump(label_encoder, os.path.join(MODEL_DIR, "label_encoder.joblib"))

    metrics = {
        "accuracy": acc,
        "precision": prec,
        "recall": rec,
        "f1_score": f1,
        "confusion_matrix": cm,
        "classes": list(label_encoder.classes_),
        "feature_importances": importances,
        "train_size": len(X_train),
        "test_size": len(X_test),
        "n_estimators": 200,
    }
    with open(os.path.join(MODEL_DIR, "metrics.json"), "w") as f:
        json.dump(metrics, f, indent=2)

    print(f"\nSaved model, label encoder and metrics.json to {MODEL_DIR}/")


if __name__ == "__main__":
    main()
