# SoleMate Outfit Recommender - Setup Guide

This guide provides step-by-step instructions to set up and run the SoleMate Outfit Recommender project on a new system.

## 1. Prerequisites
- **Python 3.11** (Strict requirement for compatibility with `numpy` and `opencv-python`). Make sure to add Python to your system PATH during installation.
- **Git** (Optional, if cloning the repository).
- **Adequate System Resources**: The application loads AI models (`YOLOv8` and `CLIP`) into memory, so at least 8GB of RAM is recommended.

## 2. Project Directory Structure
Ensure you have the project directory structure similar to this:
```
outfit_rec/
├── project_summary.md
└── backend/
    ├── api.py
    ├── color_extractor.py
    ├── extract_features.py
    ├── recommend.py
    ├── outfit_generator.py
    ├── run_shoe_recommender.py
    ├── run_outfit_generator.py
    ├── verify_model.py
    ├── yolov8n-fashionpedia.onnx  # Object detection model
    ├── yolov8n-seg.pt             # Segmentation model
    └── ...
```

## 3. Create a Virtual Environment (Recommended)
It is highly recommended to use a virtual environment to avoid dependency conflicts.

Open a terminal or PowerShell and navigate to the project root:
```powershell
cd path\to\outfit_rec
```

Create a virtual environment (explicitly using Python 3.11):
```powershell
py -3.11 -m venv venv
```

Activate the virtual environment:
- **Windows**:
  ```powershell
  venv\Scripts\activate
  ```
- **macOS/Linux**:
  ```bash
  source venv/bin/activate
  ```

## 4. Install Dependencies
Ensure your virtual environment is activated, then install the required Python libraries.

You will need the following core libraries:
```powershell
pip install fastapi uvicorn python-multipart
pip install opencv-python numpy Pillow
pip install torch torchvision torchaudio  # Use PyTorch's official site for GPU support if needed
pip install transformers
pip install webcolors trimesh
pip install scikit-learn ultralytics onnxruntime
```
*Note: `python-multipart` is required by FastAPI to handle file uploads.*

## 5. Verify the AI Models
The `backend/` directory should already contain the required YOLO model files (`yolov8n-fashionpedia.onnx` and `yolov8n-seg.pt`).

Run the verification script to ensure the model loads correctly:
```powershell
cd backend
python verify_model.py
```
*Note: Because we are inside the activated virtual environment, we use the `python` command rather than `py -3.11`. Using `py -3.11` directly might bypass the virtual environment and execute the global interpreter instead.*
*Note: The first time you run the application or tests, the Hugging Face `transformers` library will automatically download the CLIP model ("openai/clip-vit-base-patch32"), which is around ~600MB.*

## 6. Running the API Server
Start the FastAPI server:
```powershell
# Make sure you are in the backend/ directory
python -m uvicorn api:app --reload  # Use 'python', NOT 'py -3.11', to stay in the virtual environment!
```
- The server will start at: `http://127.0.0.1:8000`
- You can access the interactive API documentation (Swagger UI) at: `http://127.0.0.1:8000/docs`

## 7. Testing via CLI Tools
You can also test the core functionality using the provided CLI scripts without running the API server.

**Testing "Outfit to Shoe" Recommendation:**
```powershell
python run_shoe_recommender.py sample_outfit.jpg
```

**Testing "Shoe to Outfit" Generation:**
```powershell
python run_outfit_generator.py sample_shoe.jpg
# OR with a 3D model
python run_outfit_generator.py path/to/shoe.glb
```

**Testing Feedback Engine (Validation):**
*(Note: Requires the FastAPI server to be running)*
```powershell
python test_validate_image.py sample_outfit.jpg
```

## Troubleshooting
- **Missing YOLO model**: If `verify_model.py` fails, ensure the `.onnx` and `.pt` model files are present in the `backend/` folder.
- **Dependency Errors**: Ensure you have Python **3.11** installed. Newer versions (like 3.12/3.13) might have conflicts with `opencv-python` or `trimesh`.
- **Memory Issues**: The CLIP model can be memory-intensive. Close unused applications if you experience crashes during the first run.
