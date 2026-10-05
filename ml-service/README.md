# EquiLens ML Service

FastAPI exposes a small logistic-regression classification baseline. It predicts `POSITIVE`, `NEUTRAL`, or `NEGATIVE` from growth, profitability, leverage and valuation features. Missing features are median-imputed; inputs and probability outputs are returned so a client can inspect the pipeline.

`training_data.csv` is a controlled **synthetic** 24-row demonstration dataset. Labels are assigned by classroom rules (strong growth/returns with moderate leverage and P/E tends positive; weak growth/returns with high leverage/P/E tends negative; mixed values are neutral). These are not measured market outcomes. The train/test metrics describe agreement with those synthetic labels and must not be presented as evidence of investment performance.

The model uses a stratified fixed-seed 75/25 train/test split, `SimpleImputer`, `StandardScaler`, and balanced `LogisticRegression`. `POST /train` fits and saves `artifacts/model.joblib`; startup loads a matching model version when present and otherwise trains it. The generated artifact is ignored by Git and can be recreated from the dataset.

Run `pip install -r requirements.txt` and `uvicorn app.main:app --reload --port 8000`. Run checks with `python -m unittest discover -s tests -v`.
