"""
Loads the trained model + label encoder + class stats once, and exposes
predict_flow(flow_dict) -> full result dict. Used by main.py (FastAPI) and
can also be run directly for CLI testing:  python predict.py
"""

import os
import json
import joblib
import numpy as np

from preprocessing.feature_engineering import (
    FEATURE_COLUMNS, single_flow_to_vector,
)
from explanation import generate_reasons, recommended_action
from risk_scoring import compute_risk_and_severity

MODEL_DIR = os.path.join(os.path.dirname(__file__), "models")

_model = None
_label_encoder = None
_class_stats = None
_feature_importances = None


def load_artifacts():
    global _model, _label_encoder, _class_stats, _feature_importances
    if _model is not None:
        return
    model_path = os.path.join(MODEL_DIR, "random_forest_model.joblib")
    encoder_path = os.path.join(MODEL_DIR, "label_encoder.joblib")
    stats_path = os.path.join(MODEL_DIR, "class_feature_stats.json")

    if not os.path.exists(model_path):
        raise FileNotFoundError(
            f"Model not found at {model_path}. Run `python train.py` first."
        )

    _model = joblib.load(model_path)
    _label_encoder = joblib.load(encoder_path)
    with open(stats_path) as f:
        _class_stats = json.load(f)
    _feature_importances = dict(zip(FEATURE_COLUMNS, _model.feature_importances_.tolist()))


def is_ready() -> bool:
    return _model is not None


def predict_flow(flow: dict) -> dict:
    """flow: dict with raw flow fields (protocol, sourcePort, destinationPort,
    flowDuration, packetCount, byteCount, packetsPerSecond, bytesPerSecond,
    dstConnCount60s, uniqueDstPorts60s, repeatConnSameDstPort60s, ...)."""
    load_artifacts()

    X = single_flow_to_vector(flow)
    proba = _model.predict_proba(X)[0]
    pred_idx = int(np.argmax(proba))
    predicted_class = _label_encoder.inverse_transform([pred_idx])[0]
    confidence = float(proba[pred_idx])

    feature_values = dict(zip(FEATURE_COLUMNS, X[0].tolist()))

    reasons = generate_reasons(feature_values, predicted_class, _feature_importances, _class_stats)
    risk_score, severity = compute_risk_and_severity(
        predicted_class, confidence, feature_values, _feature_importances, _class_stats
    )
    action = recommended_action(predicted_class)

    class_probabilities = {
        cls: float(proba[i]) for i, cls in enumerate(_label_encoder.classes_)
    }

    return {
        "prediction": predicted_class,
        "confidence": round(confidence, 4),
        "riskScore": risk_score,
        "severity": severity,
        "reasons": reasons,
        "recommendedAction": action,
        "classProbabilities": {k: round(v, 4) for k, v in class_probabilities.items()},
    }


if __name__ == "__main__":
    load_artifacts()
    sample = {
        "protocol": "TCP", "sourcePort": 55210, "destinationPort": 80,
        "flowDuration": 0.4, "packetCount": 2500, "byteCount": 190000,
        "packetsPerSecond": 6250, "bytesPerSecond": 475000,
        "dstConnCount60s": 60, "uniqueDstPorts60s": 1, "repeatConnSameDstPort60s": 0,
    }
    print(json.dumps(predict_flow(sample), indent=2))
