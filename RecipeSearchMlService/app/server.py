from __future__ import annotations

import os
from pathlib import Path

from flask import Flask, jsonify, request

from .model import load_model_artifacts, rank_text_candidates, rerank_candidates

app = Flask(__name__)
MODEL_DIR = Path(os.getenv("MODEL_DIR", "/app/model/current"))
MODEL = None
MODEL_ERROR = None


def try_load_model() -> None:
    global MODEL, MODEL_ERROR
    try:
        MODEL = load_model_artifacts(MODEL_DIR)
        MODEL_ERROR = None
    except Exception as exc:  # pragma: no cover - startup-only path
        MODEL = None
        MODEL_ERROR = str(exc)


@app.get("/actuator/health")
def health():
    return jsonify(
        {
            "status": "UP" if MODEL is not None else "DEGRADED",
            "ready": MODEL is not None,
            "modelDir": str(MODEL_DIR),
            "error": MODEL_ERROR,
        }
    )


@app.post("/v1/rerank")
def rerank():
    if MODEL is None:
        return jsonify({"error": "model_not_loaded", "details": MODEL_ERROR}), 503

    payload = request.get_json(silent=True) or {}
    query = payload.get("query") or ""
    candidates = payload.get("candidates") or []
    ranked = rerank_candidates(MODEL, query, candidates)
    return jsonify({"scores": ranked})


@app.post("/v1/suggest")
def suggest():
    if MODEL is None:
        return jsonify({"error": "model_not_loaded", "details": MODEL_ERROR}), 503

    payload = request.get_json(silent=True) or {}
    query = payload.get("query") or ""
    candidates = payload.get("candidates") or []
    top_k = payload.get("limit") or payload.get("topK") or len(candidates)
    ranked = rank_text_candidates(MODEL, query, candidates, id_keys=("id",))
    try:
        top_k = int(top_k)
    except Exception:
        top_k = len(ranked)
    if top_k > 0:
        ranked = ranked[:top_k]
    return jsonify({"items": ranked})


if __name__ == "__main__":
    try_load_model()
    app.run(host="0.0.0.0", port=int(os.getenv("PORT", "8107")))
