"""
Shared preprocessing logic. Used by BOTH train.py (offline training) and
main.py (live FastAPI inference) so the exact same transformation is applied
in both places — this is what stops "train/serve skew" bugs.
"""

import numpy as np
import pandas as pd

PROTOCOL_MAP = {"TCP": 0, "UDP": 1, "ICMP": 2}

# Final, ordered feature vector fed into the Random Forest.
FEATURE_COLUMNS = [
    "protocolEncoded",
    "sourcePort",
    "destinationPort",
    "flowDuration",
    "packetCount",
    "byteCount",
    "packetsPerSecond",
    "bytesPerSecond",
    "avgPacketSize",
    "dstConnCount60s",
    "uniqueDstPorts60s",
    "repeatConnSameDstPort60s",
]

# Human-readable label for each feature, used by explanation.py
FEATURE_LABELS = {
    "protocolEncoded": "Protocol type",
    "sourcePort": "Source port",
    "destinationPort": "Destination port",
    "flowDuration": "Flow duration",
    "packetCount": "Packet count",
    "byteCount": "Byte count",
    "packetsPerSecond": "Packet rate (packets/sec)",
    "bytesPerSecond": "Byte rate (bytes/sec)",
    "avgPacketSize": "Average packet size",
    "dstConnCount60s": "Connections to this destination in the last 60s",
    "uniqueDstPorts60s": "Distinct destination ports touched by this source in the last 60s",
    "repeatConnSameDstPort60s": "Repeat attempts to this destination+port in the last 60s",
}


def engineer_dataframe(df: pd.DataFrame) -> pd.DataFrame:
    """Adds derived columns to a raw flow dataframe. Non-destructive."""
    df = df.copy()

    if "avgPacketSize" not in df.columns:
        df["avgPacketSize"] = df["byteCount"] / df["packetCount"].replace(0, 1)

    df["protocolEncoded"] = df["protocol"].map(PROTOCOL_MAP).fillna(0).astype(int)

    for col in ["dstConnCount60s", "uniqueDstPorts60s", "repeatConnSameDstPort60s"]:
        if col not in df.columns:
            df[col] = 0

    # basic cleaning: clip impossible/invalid values instead of dropping rows
    df["flowDuration"] = df["flowDuration"].clip(lower=0.001)
    df["packetCount"] = df["packetCount"].clip(lower=1)
    df["byteCount"] = df["byteCount"].clip(lower=0)
    df["packetsPerSecond"] = df["packetsPerSecond"].clip(lower=0)
    df["bytesPerSecond"] = df["bytesPerSecond"].clip(lower=0)

    return df


def to_feature_matrix(df: pd.DataFrame) -> np.ndarray:
    df = engineer_dataframe(df)
    for col in FEATURE_COLUMNS:
        if col not in df.columns:
            df[col] = 0
    return df[FEATURE_COLUMNS].astype(float).values


def single_flow_to_vector(flow: dict) -> np.ndarray:
    """Convert a single incoming flow (dict, e.g. from the API request) to a
    1-row feature matrix using the identical logic as training."""
    df = pd.DataFrame([flow])
    return to_feature_matrix(df)
