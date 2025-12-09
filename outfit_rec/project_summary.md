# SoleMate Outfit Recommender - Project Summary

## Project Goal
A backend system that accepts a user's outfit image, analyzes it using AI, and recommends matching shoes based on color harmony and style rules.

## Technology Stack
- **Language**: Python 3.11 (Required due to library support)
- **Framework**: FastAPI
- **AI Models**:
    - **YOLOv8-seg**: For segmenting the person/outfit from the background.
    - **Transformers (CLIP)**: For classifying outfit style (Casual vs Formal) using Zero-shot checks.
    - **K-Means Clustering**: For extracting dominant colors.
- **Libraries**: `opencv-python`, `numpy`, `transformers`, `torch`, `colorsys`, `webcolors`.

## System Architecture (Files in `backend/`)

### 1. [api.py](file:///d:/solemate_outfit/backend/api.py) (The Entry Point)
- **Role**: FastAPI server that handling image uploads.
- **Endpoint**: `POST /recommend`
- **Workflow**:
    1.  Receives image.
    2.  Calls [color_extractor.py](file:///d:/solemate_outfit/backend/color_extractor.py) to find colors.
    3.  Calls [extract_features.py](file:///d:/solemate_outfit/backend/extract_features.py) to identify style.
    4.  Calls [recommend.py](file:///d:/solemate_outfit/backend/recommend.py) to get shoe suggestions.
    5.  Returns JSON response.

### 2. [color_extractor.py](file:///d:/solemate_outfit/backend/color_extractor.py) (The Eye)
- **Logic**:
    - Runs YOLO segmentation to find the "Person" (Class 0).
    - Creates a binary mask to ignore the background.
    - Crops to the upper body (60% height) to focus on the shirt/main outfit.
    - Uses K-Means to find dominant RGB colors in the masked area.
    - **Fixes Applied**:
        - Numpy-to-Int conversion for JSON serialization.
        - Dynamic `webcolors` mapping (no hardcoded CSS lists).
        - Debug mode ([visualize_mask.py](file:///d:/solemate_outfit/backend/visualize_mask.py)) to see what the AI sees.

### 3. [extract_features.py](file:///d:/solemate_outfit/backend/extract_features.py) (The Brain)
- **Logic**:
    - Uses OpenAI's CLIP model (via `transformers`) to embed the image.
    - Compares image embedding to text prompts: ["casual outfit", "formal outfit", "sporty outfit"].
    - Returns the style with the highest probability.
- **Refactor**: Switched from `fashion-clip` library to standard `transformers` to avoid C++ build errors.

### 4. [recommend.py](file:///d:/solemate_outfit/backend/recommend.py) (The Stylist)
- **Logic**:
    - **Input**: Dominant Colors + Style.
    - **Color Theory**: Converts RGB to HSL (Hue, Saturation, Lightness).
    - **Rules (Dynamic)**:
        - *Dark Outfit* -> Suggest White/Light Contrast.
        - *Light Outfit* -> Suggest Black/Navy Contrast.
        - *Colorful Outfit* -> Suggest Neutrals or Complementary Pop.
    - **Style Rules**:
        - *Formal* -> Oxfords/Derbies.
        - *Casual* -> Sneakers/Slip-ons.

## Run Instructions

**Prerequisites**: Python 3.11 installed.

1.  **Navigate to Backend**:
    ```powershell
    cd d:/solemate_outfit/backend
    ```

2.  **Run Server**:
    ```powershell
    py -3.11 -m uvicorn api:app --reload
    ```
    *Server will start at `http://127.0.0.1:8000`*

3.  **Test API**:
    - Run the test script:
      ```powershell
      py -3.11 test_api.py
      ```
    - Or use the debug script to check the mask:
      ```powershell
      py -3.11 visualize_mask.py
      ```

## Known Constraints
- **Python Version**: Must use Python 3.11 (not 3.14) due to `numpy`/`opencv` support.
- **Performance**: First request might be slow due to model loading (CLIP/YOLO).
