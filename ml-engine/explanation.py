"""
Explainable AI module.

Reasons are generated from TWO real, transparent signals — never randomly:
  1. The Random Forest's global feature_importances_ (which features the
     model actually relies on).
  2. How far the incoming flow's values deviate from the "Normal" class's
     mean/std for that feature (a z-score), computed once at training time
     and saved in models/class_feature_stats.json.

A feature only becomes a displayed "reason" if it is both (a) among the
model's more important features and (b) statistically abnormal for this
specific flow. This keeps every explanation tied to the actual input.
"""

from preprocessing.feature_engineering import FEATURE_COLUMNS, FEATURE_LABELS

Z_THRESHOLD = 1.4
MAX_REASONS = 4

REASON_TEMPLATES = {
    "packetsPerSecond": {
        "high": "Packet rate is far above normal baseline traffic",
        "low": "Packet rate is unusually low for this traffic type",
    },
    "bytesPerSecond": {
        "high": "Data transfer rate is abnormally high for this flow",
        "low": "Data transfer rate is unusually low",
    },
    "packetCount": {
        "high": "Very high packet count in a single flow",
        "low": "Very small packet count, typical of probing traffic",
    },
    "byteCount": {
        "high": "Total flow size is significantly larger than normal",
        "low": "Total flow size is abnormally small",
    },
    "avgPacketSize": {
        "high": "Average packet size deviates from normal sessions",
        "low": "Unusually small packet size, consistent with flood or probe traffic",
    },
    "flowDuration": {
        "high": "Flow lasted much longer than typical sessions",
        "low": "Extremely short flow duration, inconsistent with a normal session",
    },
    "dstConnCount60s": {
        "high": "Destination host is receiving connections from an unusually large number of sources",
        "low": "Destination host is receiving fewer connections than usual",
    },
    "uniqueDstPorts60s": {
        "high": "Source touched many distinct destination ports in a short time window",
        "low": "Source touched fewer distinct ports than usual",
    },
    "repeatConnSameDstPort60s": {
        "high": "Repeated connection attempts to the same destination and port in a short window",
        "low": "Fewer repeat attempts than usual",
    },
    "destinationPort": {
        "high": "Destination port differs from common service ports",
        "low": "Destination port differs from common service ports",
    },
}


def generate_reasons(feature_values: dict, predicted_class: str,
                      feature_importances: dict, class_stats: dict) -> list:
    if predicted_class == "Normal" or predicted_class not in class_stats:
        return ["Traffic pattern is consistent with the learned baseline of normal behaviour"]

    normal_stats = class_stats.get("Normal")
    if normal_stats is None:
        return []

    scored = []
    for feat in FEATURE_COLUMNS:
        if feat not in REASON_TEMPLATES:
            continue
        value = feature_values.get(feat, 0)
        mean = normal_stats["mean"].get(feat, 0)
        std = normal_stats["std"].get(feat, 1) or 1
        z = (value - mean) / std
        importance = feature_importances.get(feat, 0)
        if abs(z) >= Z_THRESHOLD:
            direction = "high" if z > 0 else "low"
            reason_text = REASON_TEMPLATES[feat][direction]
            score = importance * abs(z)
            scored.append((score, reason_text))

    scored.sort(key=lambda x: -x[0])
    reasons = []
    seen = set()
    for _, text in scored:
        if text not in seen:
            reasons.append(text)
            seen.add(text)
        if len(reasons) >= MAX_REASONS:
            break

    if not reasons:
        reasons = ["Traffic pattern deviates from the learned normal baseline"]

    return reasons


RECOMMENDED_ACTIONS = {
    "DDoS": "High-volume traffic detected towards this destination. Investigate the source traffic and consider temporary rate-limiting or isolation according to security policy.",
    "PortScan": "Reconnaissance-style scanning activity detected. Investigate the source host and monitor for follow-up connection attempts according to security policy.",
    "BruteForce": "Repeated authentication attempts detected. Investigate the source host and consider temporary account lockout or isolation according to security policy.",
    "Normal": "No action required. Continue routine monitoring.",
}


def recommended_action(predicted_class: str) -> str:
    return RECOMMENDED_ACTIONS.get(
        predicted_class,
        "Investigate the source traffic and consider temporary isolation according to security policy.",
    )
