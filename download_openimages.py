"""
Download Open Images V7 dataset for ONLY the 'Ceiling fan' class.

All datasets, caches, databases, and temporary files are configured to reside on
the E: drive to protect C: drive storage.
"""

import os
import sys
import tempfile

# ==============================================================================
# Configure Storage Paths on E: Drive (Protects C: Drive)
# ==============================================================================
BASE_E = r"E:\SolarEase-Datasets"
OPENIMAGES_DIR = os.path.join(BASE_E, "OpenImages")
CEILINGFAN_DIR = os.path.join(OPENIMAGES_DIR, "CeilingFan")
FIFTYONE_DIR = os.path.join(BASE_E, "FiftyOne")
DB_DIR = os.path.join(FIFTYONE_DIR, "db")
ZOO_DIR = CEILINGFAN_DIR  # Zoo datasets saved under E:\SolarEase-Datasets\OpenImages\CeilingFan
MODELS_DIR = os.path.join(FIFTYONE_DIR, "models")
PLUGINS_DIR = os.path.join(FIFTYONE_DIR, "plugins")
TMP_DIR = os.path.join(BASE_E, "tmp")

# Ensure all target directories exist on E:
for d in [CEILINGFAN_DIR, DB_DIR, ZOO_DIR, MODELS_DIR, PLUGINS_DIR, TMP_DIR]:
    os.makedirs(d, exist_ok=True)

# Set environment variables BEFORE importing FiftyOne
os.environ["FIFTYONE_DEFAULT_DATASET_DIR"] = OPENIMAGES_DIR
os.environ["FIFTYONE_DATASET_ZOO_DIR"] = ZOO_DIR
os.environ["FIFTYONE_DATABASE_DIR"] = DB_DIR
os.environ["FIFTYONE_MODEL_ZOO_DIR"] = MODELS_DIR
os.environ["FIFTYONE_PLUGINS_DIR"] = PLUGINS_DIR
os.environ["TEMP"] = TMP_DIR
os.environ["TMP"] = TMP_DIR
os.environ["TMPDIR"] = TMP_DIR
tempfile.tempdir = TMP_DIR

# Clean PostgreSQL SSL environment variable conflict if present
if "CURL_CA_BUNDLE" in os.environ and "PostgreSQL" in os.environ["CURL_CA_BUNDLE"]:
    del os.environ["CURL_CA_BUNDLE"]
if "SSL_CERT_FILE" in os.environ and "PostgreSQL" in os.environ["SSL_CERT_FILE"]:
    del os.environ["SSL_CERT_FILE"]

# Ensure Python 3.11 is prioritized
py311_dir = r"C:\Users\SANJAI\AppData\Local\Programs\Python\Python311"
py311_scripts = os.path.join(py311_dir, "Scripts")
if os.path.exists(py311_dir) and py311_dir not in os.environ.get("PATH", ""):
    os.environ["PATH"] = f"{py311_dir};{py311_scripts};" + os.environ.get("PATH", "")

import fiftyone as fo
import fiftyone.zoo as foz

# Force FiftyOne config properties to E: paths
fo.config.default_dataset_dir = OPENIMAGES_DIR
fo.config.dataset_zoo_dir = ZOO_DIR
fo.config.database_dir = DB_DIR
fo.config.model_zoo_dir = MODELS_DIR
fo.config.plugins_dir = PLUGINS_DIR

# Allow dataset.dataset_dir access
if not hasattr(fo.Dataset, "dataset_dir"):
    fo.Dataset.dataset_dir = property(
        lambda self: foz.find_zoo_dataset("open-images-v7")
    )

print("==================================================")
print("FiftyOne Storage Configuration on E: Drive:")
print(f"  Database directory    : {fo.config.database_dir}")
print(f"  Dataset Zoo directory : {fo.config.dataset_zoo_dir}")
print(f"  Default dataset dir   : {fo.config.default_dataset_dir}")
print(f"  Temporary directory   : {tempfile.gettempdir()}")
print("==================================================")

# Target configuration
TARGET_CLASS = "Ceiling fan"
DATASET_NAME = "ceiling-fan-open-images-v7"

# Clean up any existing FiftyOne DB registration with the same name if necessary
if DATASET_NAME in fo.list_datasets():
    print(f"Removing previous database registration for '{DATASET_NAME}'...")
    fo.delete_dataset(DATASET_NAME)

print(f"Downloading Open Images V7 for class: '{TARGET_CLASS}'...")
print(f"Split: train | Label type: detections")

dataset = foz.load_zoo_dataset(
    "open-images-v7",
    split="train",
    label_types=["detections"],
    classes=[TARGET_CLASS],
    dataset_name=DATASET_NAME,
)

# Persist dataset in FiftyOne database
dataset.persistent = True

print("==================================================")
print("Downloaded images:", len(dataset))
print("Dataset location:", dataset.dataset_dir)

# Count annotations for TARGET_CLASS
total_annotations = 0
for sample in dataset:
    if sample.ground_truth and sample.ground_truth.detections:
        for det in sample.ground_truth.detections:
            if det.label == TARGET_CLASS:
                total_annotations += 1

print(f"Total annotations: {total_annotations}")
print("==================================================")
