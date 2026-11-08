"""
CyberSentinel AI - ML Inference Service (FastAPI)
==================================================
Loads the trained Random Forest model once at startup and exposes:

  GET  /health          - service + model status
  GET  /model-info       - evaluation metrics, classes, feature importances
  POST /predict          - single network flow -> prediction
  POST /predict/batch     - list of network flows -> list of predictions

This service is called ONLY by the Spring Boot backend. Nothing here talks
to MongoDB or the frontend directly.

Run:  uvicorn main:app --host 0.0.0.0 --port 8000 --reload
"""

import os
import json
import time
from typing import List, Optional

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

import predict as predict_module

app = FastAPI(title="CyberSentinel AI - ML Engine", version="1.0.0")

# Only the Spring Boot backend should call this service. In production this
# would be restricted to the backend's internal address, not "*".
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

START_TIME = time.time()
MODEL_DIR = os.path.join(os.path.dirname(__file__), "models")


class FlowInput(BaseModel):
    sourceIP: Optional[str] = "0.0.0.0"
    destinationIP: Optional[str] = "0.0.0.0"
    protocol: str = Field(default="TCP")
    sourcePort: int = 0
    destinationPort: int = 0
    flowDuration: float
    packetCount: int
    byteCount: int
    packetsPerSecond: Optional[float] = None
    bytesPerSecond: Optional[float] = None
    # Contextual/aggregate features. In real traffic these are computed by
    # the Spring Boot backend from recent flows stored in MongoDB before
    # calling this service. Default to 0 so this endpoint can also be
    # tested standalone via /docs.
    dstConnCount60s: int = 0
    uniqueDstPorts60s: int = 0
    repeatConnSameDstPort60s: int = 0

    def to_dict(self):
        d = self.dict()
        if d.get("packetsPerSecond") is None:
            d["packetsPerSecond"] = d["packetCount"] / max(d["flowDuration"], 0.001)
        if d.get("bytesPerSecond") is None:
            d["bytesPerSecond"] = d["byteCount"] / max(d["flowDuration"], 0.001)
        return d


class BatchInput(BaseModel):
    flows: List[FlowInput]


@app.on_event("startup")
def startup():
    try:
        predict_module.load_artifacts()
        print("Model artifacts loaded successfully.")
    except FileNotFoundError as e:
        # Service still starts so /health can report the problem clearly
        # instead of the process crashing silently.
        print(f"WARNING: {e}")


@app.get("/health")
def health():
    return {
        "status": "online" if predict_module.is_ready() else "model_not_loaded",
        "modelLoaded": predict_module.is_ready(),
        "uptimeSeconds": round(time.time() - START_TIME, 1),
    }


@app.get("/model-info")
def model_info():
    metrics_path = os.path.join(MODEL_DIR, "metrics.json")
    if not os.path.exists(metrics_path):
        raise HTTPException(status_code=503, detail="Model metrics not available. Train the model first.")
    with open(metrics_path) as f:
        return json.load(f)


@app.post("/predict")
def predict(flow: FlowInput):
    if not predict_module.is_ready():
        raise HTTPException(status_code=503, detail="ML model is not loaded. Run train.py and restart the service.")
    try:
        result = predict_module.predict_flow(flow.to_dict())
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Prediction failed: {e}")


@app.post("/predict/batch")
def predict_batch(payload: BatchInput):
    if not predict_module.is_ready():
        raise HTTPException(status_code=503, detail="ML model is not loaded. Run train.py and restart the service.")
    results = []
    for flow in payload.flows:
        try:
            results.append(predict_module.predict_flow(flow.to_dict()))
        except Exception as e:
            results.append({"error": str(e)})
    return {"results": results, "count": len(results)}
