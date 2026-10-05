from __future__ import annotations

from pathlib import Path
from typing import Any
import csv
import joblib
import sklearn

import numpy as np
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, classification_report
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

ROOT = Path(__file__).resolve().parents[1]
DATASET = ROOT / "training_data.csv"
FEATURES = [
    "revenueGrowth", "profitGrowth", "roe", "debtToEquity", "peRatio",
    "fcfMargin", "operatingMargin", "epsGrowth",
]
MODEL_VERSION = "synthetic-baseline-v1"
MODEL_PATH = ROOT / "artifacts" / "model.joblib"
MODEL: Pipeline | None = None
METRICS: dict[str, Any] | None = None


def train_model() -> dict[str, Any]:
    global MODEL, METRICS
    with DATASET.open(newline="", encoding="utf-8") as stream:
        next(stream)  # explicitly documented synthetic data note
        rows = list(csv.DictReader(stream))
    x = np.asarray([[float(row[name]) for name in FEATURES] for row in rows], dtype=float)
    y = np.asarray([row["label"] for row in rows])
    x_train, x_test, y_train, y_test = train_test_split(
        x, y, test_size=0.25, random_state=42, stratify=y
    )
    candidate = Pipeline([
        ("imputer", SimpleImputer(strategy="median", keep_empty_features=True)),
        ("scaler", StandardScaler()),
        ("classifier", LogisticRegression(max_iter=1000, class_weight="balanced", random_state=42)),
    ])
    candidate.fit(x_train, y_train)
    predicted = candidate.predict(x_test)
    MODEL = candidate
    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump({"model": candidate, "modelVersion": MODEL_VERSION, "sklearnVersion": sklearn.__version__}, MODEL_PATH)
    METRICS = {
        "accuracy": float(accuracy_score(y_test, predicted)),
        "classificationReport": classification_report(y_test, predicted, output_dict=True, zero_division=0),
        "trainRows": int(len(x_train)),
        "testRows": int(len(x_test)),
        "dataset": "controlled synthetic demonstration data; labels are rule-defined, not observed investment outcomes",
        "modelVersion": MODEL_VERSION,
        "features": FEATURES,
    }
    return METRICS


def get_model() -> Pipeline:
    global MODEL
    if MODEL is None:
        if MODEL_PATH.exists():
            saved = joblib.load(MODEL_PATH)
            if (
                saved.get("modelVersion") == MODEL_VERSION
                and saved.get("sklearnVersion") == sklearn.__version__
            ):
                MODEL = saved["model"]
        if MODEL is None:
            train_model()
    assert MODEL is not None
    return MODEL


def get_metrics() -> dict[str, Any]:
    if METRICS is None:
        train_model()
    assert METRICS is not None
    return METRICS


def predict(features: dict[str, float | None]) -> dict[str, Any]:
    model = get_model()
    row = np.asarray([[np.nan if features.get(name) is None else float(features[name]) for name in FEATURES]], dtype=float)
    probabilities = model.predict_proba(row)[0]
    classes = model.named_steps["classifier"].classes_
    best = int(probabilities.argmax())
    return {
        "signal": str(classes[best]),
        "probability": float(probabilities[best]),
        "probabilities": {str(label): float(probability) for label, probability in zip(classes, probabilities)},
        "modelVersion": MODEL_VERSION,
        "features": features,
        "datasetType": "synthetic_demo",
    }
