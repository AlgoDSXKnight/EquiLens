from __future__ import annotations

from math import isfinite

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, model_validator

from .model import FEATURES, get_metrics, predict, train_model

app = FastAPI(title="EquiLens Research Signal API", version="1.0.0")


class FinancialFeatures(BaseModel):
    revenueGrowth: float | None = None
    profitGrowth: float | None = None
    roe: float | None = None
    debtToEquity: float | None = None
    peRatio: float | None = None
    fcfMargin: float | None = None
    operatingMargin: float | None = None
    epsGrowth: float | None = None

    @model_validator(mode="after")
    def require_finite_values(self) -> "FinancialFeatures":
        if any(value is not None and not isfinite(value) for value in self.model_dump().values()):
            raise ValueError("Financial features must be finite numbers")
        return self


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "equilens-ml", "datasetType": "synthetic_demo"}


@app.post("/predict")
def make_prediction(features: FinancialFeatures) -> dict:
    values = features.model_dump()
    if not any(value is not None for value in values.values()):
        raise HTTPException(status_code=422, detail="At least one financial feature is required")
    return predict(values)


@app.post("/train")
def train() -> dict:
    return train_model()


@app.get("/model/metrics")
def metrics() -> dict:
    return get_metrics()
