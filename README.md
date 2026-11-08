# CyberSentinel AI

**SIH26145 — AI-Based Detection of Cyber Threats in Unidirectional IP Traffic**
Theme: Blockchain & Cybersecurity · College-level prototype for Smart India Hackathon selection (5 September)

> **Scope note:** This is intentionally a *small, reliable* prototype, not the full hackathon-final vision. It detects 4 traffic classes end-to-end through a real ML pipeline. Blockchain, deep learning, GenAI copilots, autonomous blocking, mobile apps, and cloud deployment are deliberately **not** implemented — see [Future Scope](#future-scope).

---

## 1. Project Overview

CyberSentinel AI analyzes unidirectional network-flow records (uploaded as CSV, or generated live by a controlled Attack Simulator) and classifies each flow into one of four classes using a trained Random Forest model:

1. **Normal** traffic
2. **DDoS / DoS**
3. **Port Scan**
4. **Brute Force**

Every prediction carries a **confidence score** (how sure the model is), a separately-computed **risk score** (how dangerous the behaviour is), a **severity** label, and a set of **explainable reasons** derived from the model's actual feature importances — never hardcoded or randomly generated text.

## 2. Problem Statement

SIH26145 asks for AI-based detection of cyber threats in unidirectional IP traffic. This prototype demonstrates the core detection loop — flow ingestion → preprocessing → ML classification → risk scoring → explanation → storage → dashboard — reliably and transparently, so it can be understood by a judge in under two minutes.

## 3. Features (this prototype)

- CSV upload of network-flow datasets, validated and run through the real pipeline
- Controlled Attack Simulator that generates representative (not hardcoded) flow bursts for each attack type, through the *same* ML pipeline as an upload
- Live SOC-style dashboard: stats, traffic activity chart, threat distribution chart, live threat feed
- Threats table with filters, and a Threat Investigation view with full explainability + recommended action
- System Status page showing health of ML service, backend, and MongoDB
- Documented, transparent risk-scoring formula (confidence ≠ risk, kept separate)

## 4. Architecture

```
Network Flow
   ↓
CSV Upload  OR  Controlled Attack Simulation
   ↓
Data Validation (Spring Boot)
   ↓
Feature Preprocessing (Python)
   ↓
Machine Learning Model (Random Forest, scikit-learn)
   ↓
Prediction + Confidence
   ↓
Risk Score + Severity
   ↓
Explainable AI (feature importance + statistical deviation)
   ↓
Spring Boot Backend (business logic, validation, orchestration)
   ↓
MongoDB (network_flows, threats, alerts)
   ↓
React SOC Dashboard
```

```
React (JS/Vite)  ──HTTP──▶  Spring Boot (Java)  ──WebClient──▶  FastAPI (Python) ──▶ scikit-learn model
                                    │
                                    ▼
                                MongoDB
```

The **frontend never talks to MongoDB or the ML service directly** — only to the Spring Boot API. The **ML service never talks to MongoDB or the frontend** — only to Spring Boot.

## 5. Technology Stack

| Layer | Technology |
|---|---|
| Frontend | JavaScript, React, Vite, JSX + CSS, Recharts |
| Backend | Java, Spring Boot, Spring Data MongoDB, Spring WebFlux (WebClient only) |
| Database | MongoDB |
| ML / AI | Python, scikit-learn, pandas, numpy, joblib |
| ML API | Python, FastAPI, Uvicorn |

No PHP, no Node.js backend, no Flask, no MySQL, per the stated constraints.

## 6. Folder Structure

```
CyberSentinel/
├── frontend/        React + Vite SOC dashboard
├── backend/         Spring Boot REST API (Java)
├── ml-engine/        Python ML training + FastAPI inference service
├── database/
│   └── sample-data/  Example MongoDB documents + a demo CSV
├── docs/             Additional documentation
└── README.md         This file
```

---

## 7. Prerequisites

- **Node.js** 18+ and npm
- **Java** 17+ and Maven 3.9+
- **Python** 3.10+ and pip
- **MongoDB** 6+ running locally (or a connection string to a hosted instance)

---

## 8. Setup & Run — in order

The three backend-side services must start in this order: **MongoDB → ML service → Spring Boot → React**.

### 8.1 MongoDB

Start a local MongoDB instance (default settings are fine):

```bash
mongod --dbpath /path/to/your/data/dir
```

MongoDB will listen on `mongodb://localhost:27017`. The `cybersentinel` database and its three collections (`network_flows`, `threats`, `alerts`) are created automatically the first time data is written — no manual schema setup needed. Example documents matching the exact shape used are in `database/sample-data/`.

### 8.2 ML Engine (Python / FastAPI)

```bash
cd ml-engine
python3 -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt

# 1. Generate the training dataset (see docs/DATASET.md for methodology)
python data/generate_dataset.py

# 2. Train the model (prints real accuracy/precision/recall/F1 + confusion matrix)
python train.py

# 3. Start the inference API
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

Verify it's alive: `curl http://localhost:8000/health` → `{"status":"online","modelLoaded":true,...}`

Interactive API docs: `http://localhost:8000/docs`

### 8.3 Backend (Spring Boot)

```bash
cd backend
cp .env.example .env      # then edit values if needed
```

Export the variables from `.env` (or set them in your shell / IDE run config) before running:

```bash
export MONGODB_URI=mongodb://localhost:27017/cybersentinel
export ML_SERVICE_URL=http://localhost:8000
export CORS_ALLOWED_ORIGINS=http://localhost:5173

mvn spring-boot:run
```

The API starts on `http://localhost:8080`. Health check: `curl http://localhost:8080/api/system/status`

> **Note on this repository's build state:** the Java source in `backend/` was written and reviewed carefully, but could **not** be compiled inside the sandbox this project was generated in (no access to Maven Central from that environment). Run `mvn clean compile` first thing after downloading to confirm a clean build on your machine, and open an issue/fix any straggler compile error before the demo — treat this as untested, not battle-tested.

### 8.4 Frontend (React / Vite)

```bash
cd frontend
cp .env.example .env      # defaults already point at localhost:8080/api
npm install
npm run dev
```

Open `http://localhost:5173`.

`npm run build` was verified in the build environment for this project (produces a clean production bundle with no errors).

---

## 9. How the Prediction Pipeline Works

1. A flow arrives either as a row in an uploaded CSV or as a record generated by the Attack Simulator.
2. Spring Boot validates it (`CsvFlowParser` / `FlowSimulator`), saves the raw flow to `network_flows`, then computes three **contextual/aggregate features** by querying the last 60 seconds of related flows already in MongoDB (`FlowContextService`):
   - `dstConnCount60s` — connections to the same destination IP in the last 60s
   - `uniqueDstPorts60s` — distinct destination ports the same source IP touched in the last 60s
   - `repeatConnSameDstPort60s` — repeat attempts by the same source to the same destination+port in the last 60s
3. Spring Boot sends the full feature set to FastAPI's `POST /predict`.
4. FastAPI loads the trained Random Forest (`joblib`), computes `predict_proba`, and returns prediction + confidence.
5. FastAPI computes **risk score** and **severity** using the documented formula in `ml-engine/risk_scoring.py`, and **reasons** using `ml-engine/explanation.py`.
6. Spring Boot saves the result as a `Threat` document, and an `Alert` if it isn't Normal.
7. The dashboard polls Spring Boot's REST API and renders everything.

The Attack Simulator does **not** shortcut this — it calls `FlowSimulator` to generate a burst of realistic, *unlabeled* raw flow records, then pushes every one of them through the exact same `TrafficService.processFlow()` pipeline as a CSV upload. The ML model — not a button — decides what gets flagged.

## 10. Dataset & Model

See [`docs/DATASET.md`](docs/DATASET.md) for full methodology. In short: public flow-level datasets that match this exact feature set (CICIDS2017-style: source/destination IP, ports, protocol, packets/bytes per second) are multi-gigabyte downloads not reachable from a sandboxed build environment. A documented, session-based simulator (`ml-engine/data/generate_dataset.py`) was used instead, calibrated to the same statistical signatures published intrusion-detection research describes for these attack families, with deliberate class overlap/noise so the reported metrics are honest rather than artificially perfect.

**Actual measured results** (held-out 20% test split, 200-tree Random Forest, `class_weight="balanced"`):

| Metric | Value |
|---|---|
| Accuracy | 99.65% |
| Precision (weighted) | 99.65% |
| Recall (weighted) | 99.65% |
| F1-score (weighted) | 99.65% |

Confusion matrix and per-feature importances are saved to `ml-engine/models/metrics.json` after training and are also served at `GET /model-info` on the ML API. Full details, caveats, and how to swap in a real dataset are in `docs/DATASET.md`.

## 11. Risk Scoring Formula

Confidence and risk are deliberately kept separate. Full documented formula lives in `ml-engine/risk_scoring.py`:

- **Confidence** — the Random Forest's own `predict_proba` for the predicted class.
- **Risk** — `base_weight[threat_type] + confidence × 15 + intensity × 10`, where `base_weight` is `DDoS=70, BruteForce=60, PortScan=50`, and `intensity` is the average deviation (z-score, capped) of the flow's top-5 most important features from the learned Normal baseline.
- **Severity** — thresholded on risk: `≥85 CRITICAL`, `70–84 HIGH`, `40–69 MEDIUM`, `<40 LOW`, and `SAFE` for anything classified Normal.

## 12. Explainability

`ml-engine/explanation.py` only surfaces a reason if a feature is **both** (a) among the model's more important features globally, and (b) statistically abnormal for that specific flow (z-score vs. the Normal class baseline, computed once at training time and saved to `models/class_feature_stats.json`). Nothing is randomly generated.

## 13. Environment Variables

| File | Purpose |
|---|---|
| `backend/.env.example` | `MONGODB_URI`, `ML_SERVICE_URL`, `ML_SERVICE_TIMEOUT_SECONDS`, `CORS_ALLOWED_ORIGINS` |
| `frontend/.env.example` | `VITE_API_BASE_URL` |

Never commit a real `.env` file. `ml-engine` reads no secrets — it has no external dependencies.

## 14. Error Handling

- Backend: a `GlobalExceptionHandler` (`@RestControllerAdvice`) turns invalid CSVs, missing columns, oversized uploads, and ML-service timeouts into clear JSON errors instead of stack traces or silent failures.
- Frontend: every page has explicit loading, error (with retry), and empty states (`LoadingState.jsx`, `ErrorState.jsx`, `EmptyState.jsx`) — nothing fails silently.
- ML service: if the model isn't trained/loaded yet, `/health` reports it and `/predict` returns `503` with a clear message rather than crashing.

## 15. Known Limitations

- Training data is a documented synthetic/simulated dataset (see §10), not a live capture — expect lower accuracy on real, messier traffic.
- Dashboard traffic-activity bucketing currently scans all flows in memory (`DashboardService.bucketize`) — fine at prototype scale, would need an aggregation pipeline or a capped/indexed query at real scale.
- No authentication/RBAC — out of scope for this prototype (see Future Scope).
- The Spring Boot module was not compiled inside the environment that generated it (see §8.3) — verify the build locally before the demo.

## 16. Future Scope

*(explicitly out of scope for 5 September — do not attempt to add before the demo)*

- Advanced/zero-day anomaly detection, temporal models, threat forecasting
- GenAI SOC Copilot, automated incident report generation
- Human-approved automated response, firewall integration
- RBAC, audit logs, cloud deployment, mobile application
- Real-time packet/flow ingestion at enterprise scale
- Additional attack categories
- Blockchain-based audit-log integrity, if justified by the SIH theme

---

## 17. Coding Language Summary

- **Frontend:** JavaScript + JSX, React, Vite, CSS, Recharts (JavaScript)
- **Backend:** Java, Spring Boot
- **Database:** MongoDB, MongoDB Query Language (via Spring Data)
- **AI/ML:** Python — scikit-learn, pandas, numpy, joblib
- **ML API:** Python, FastAPI
