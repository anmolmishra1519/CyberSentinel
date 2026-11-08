"""
Risk Scoring
============
Confidence and Risk are deliberately kept separate:

  CONFIDENCE = how certain the ML model is about the predicted class
               (straight from RandomForestClassifier.predict_proba).

  RISK       = how dangerous the detected behaviour is, combining:
                 1) a base danger weight for the predicted threat TYPE
                 2) the model's confidence in that prediction
                 3) a traffic-intensity/abnormality factor (how far the
                    flow's key features deviate from the learned Normal
                    baseline — the same z-scores used for explainability)

FORMULA
-------
  Normal:
      risk = round( (1 - confidence) * 15 )         # 0-15, "how sure are we it's really safe"

  DDoS / PortScan / BruteForce:
      risk = base_weight[type]
             + confidence * 15
             + intensity  * 10
      risk = clip(round(risk), 0, 100)

  base_weight:  DDoS = 70, BruteForce = 60, PortScan = 50
    (DDoS/BruteForce represent active attacks against availability/
     credentials; PortScan is reconnaissance, so it starts lower.)

  intensity: mean |z-score| (against the Normal-class mean/std, capped at
             4 standard deviations) across the top-5 most important
             features for this prediction, then min-max normalised to 0-1.

SEVERITY is then a simple, documented threshold on top of risk:
  Normal            -> SAFE
  risk >= 85         -> CRITICAL
  70 <= risk < 85     -> HIGH
  40 <= risk < 70     -> MEDIUM
  risk < 40           -> LOW
"""

from preprocessing.feature_engineering import FEATURE_COLUMNS

BASE_WEIGHT = {
    "DDoS": 70,
    "BruteForce": 60,
    "PortScan": 50,
}

Z_CAP = 4.0


def _intensity(feature_values: dict, feature_importances: dict, class_stats: dict) -> float:
    normal_stats = class_stats.get("Normal")
    if not normal_stats:
        return 0.0

    top_features = sorted(FEATURE_COLUMNS, key=lambda f: -feature_importances.get(f, 0))[:5]

    zs = []
    for feat in top_features:
        value = feature_values.get(feat, 0)
        mean = normal_stats["mean"].get(feat, 0)
        std = normal_stats["std"].get(feat, 1) or 1
        z = abs((value - mean) / std)
        zs.append(min(z, Z_CAP))

    if not zs:
        return 0.0

    avg_z = sum(zs) / len(zs)
    return min(avg_z / Z_CAP, 1.0)


def compute_risk_and_severity(predicted_class: str, confidence: float,
                               feature_values: dict, feature_importances: dict,
                               class_stats: dict):
    if predicted_class == "Normal":
        risk = round((1 - confidence) * 15)
        risk = max(0, min(risk, 15))
        return risk, "SAFE"

    base = BASE_WEIGHT.get(predicted_class, 55)
    intensity = _intensity(feature_values, feature_importances, class_stats)
    risk = base + confidence * 15 + intensity * 10
    risk = int(max(0, min(round(risk), 100)))

    if risk >= 85:
        severity = "CRITICAL"
    elif risk >= 70:
        severity = "HIGH"
    elif risk >= 40:
        severity = "MEDIUM"
    else:
        severity = "LOW"

    return risk, severity
