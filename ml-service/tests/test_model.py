import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from fastapi import HTTPException
from pydantic import ValidationError
from app.main import FinancialFeatures, make_prediction
from app import model as model_module
from app.model import FEATURES, MODEL_PATH, predict, train_model


class ResearchModelTests(unittest.TestCase):
    def test_training_uses_holdout_and_reports_dataset_limits(self):
        metrics = train_model()
        self.assertGreater(metrics["trainRows"], metrics["testRows"])
        self.assertIn("synthetic", metrics["dataset"])
        self.assertTrue(MODEL_PATH.exists())

    def test_matching_saved_model_is_loaded(self):
        original_path = model_module.MODEL_PATH
        original_model = model_module.MODEL
        original_metrics = model_module.METRICS
        try:
            with TemporaryDirectory() as directory:
                model_module.MODEL_PATH = Path(directory) / "model.joblib"
                model_module.MODEL = None
                model_module.METRICS = None
                train_model()
                model_module.MODEL = None

                loaded = model_module.get_model()

                self.assertEqual(loaded.named_steps["classifier"].classes_.size, 3)
        finally:
            model_module.MODEL_PATH = original_path
            model_module.MODEL = original_model
            model_module.METRICS = original_metrics


    def test_incompatible_saved_model_is_retrained(self):
        original_path = model_module.MODEL_PATH
        original_model = model_module.MODEL
        original_metrics = model_module.METRICS
        try:
            with TemporaryDirectory() as directory:
                model_module.MODEL_PATH = Path(directory) / "model.joblib"
                model_module.MODEL = None
                model_module.METRICS = None
                model_module.MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
                import joblib
                joblib.dump({
                    "modelVersion": model_module.MODEL_VERSION,
                    "sklearnVersion": "0.0-incompatible",
                    "model": None,
                }, model_module.MODEL_PATH)

                loaded = model_module.get_model()

                self.assertIsNotNone(loaded)
                saved = joblib.load(model_module.MODEL_PATH)
                self.assertEqual(saved["sklearnVersion"], model_module.sklearn.__version__)
        finally:
            model_module.MODEL_PATH = original_path
            model_module.MODEL = original_model
            model_module.METRICS = original_metrics

    def test_prediction_returns_probabilities_and_features(self):
        features = {name: 12.0 for name in FEATURES}
        result = predict(features)
        self.assertIn(result["signal"], {"POSITIVE", "NEUTRAL", "NEGATIVE"})
        self.assertAlmostEqual(sum(result["probabilities"].values()), 1.0, places=6)
        self.assertEqual(result["features"], features)

    def test_empty_prediction_input_is_rejected(self):
        with self.assertRaises(HTTPException) as error:
            make_prediction(FinancialFeatures())
        self.assertEqual(error.exception.status_code, 422)

    def test_prediction_accepts_partial_features(self):
        result = predict({name: None for name in FEATURES} | {"roe": 20.0})
        self.assertIn(result["signal"], {"POSITIVE", "NEUTRAL", "NEGATIVE"})

    def test_non_finite_feature_values_are_rejected(self):
        for value in (float("nan"), float("inf"), float("-inf")):
            with self.subTest(value=value):
                with self.assertRaises(ValidationError):
                    FinancialFeatures(roe=value)


if __name__ == "__main__":
    unittest.main()
