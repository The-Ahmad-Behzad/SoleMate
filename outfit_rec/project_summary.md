# SoleMate Outfit Recommender - Project Summary

## Project Goal
A comprehensive backend system that provides two-way fashion recommendations:
1.  **Outfit -> Shoe**: Analyzes a user's outfit to recommend matching shoes.
2.  **Shoe -> Outfit**: Analyzes a shoe (Image or 3D Model) to recommend a suitable outfit.

## Technology Stack
- **Language**: Python 3.11 (Required due to library support)
- **Framework**: FastAPI
- **AI Models**:
    - **YOLOv8 (Fashionpedia)**: For detecting and locating specific fashion items (shirt, pants, accessories) via bounding boxes to isolate them for color extraction.
    - **Transformers (CLIP)**: For classifying outfit and shoe styles using Zero-shot checks (e.g. "casual" vs "formal").
    - **K-Means Clustering**: For extracting dominant colors from the detected regions.
- **Libraries**: `opencv-python`, `numpy`, `transformers`, `torch`, `colorsys`, `webcolors`, `trimesh` (for 3D files), `Pillow`.

## System Architecture (Files in `backend/`)

### 1. [api.py](file:///d:/outfit_rec/backend/api.py) (The Interface)
- **Role**: FastAPI server handling requests.
- **API Documentation (For Swagger/OpenAPI Implementation)**:

    *Note: FastAPI automatically generates a fully interactive Swagger/OpenAPI UI. Once the server is running, you can access it at `http://127.0.0.1:8000/docs`. The endpoint schemas below are provided for reference.*

    *All POST endpoints expect a `multipart/form-data` request with a `file` field.*

    - `GET /`:
        - **Description**: Health check endpoint.
        - **Response Schema** (JSON):
          ```json
          {"message": "string"}
          ```
    - `POST /recommend`:
        - **Description**: Accepts an outfit image and returns shoe suggestions.
        - **Request Content-Type**: `multipart/form-data`
        - **Body parameter**: `file` (binary/image)
        - **Response Schema** (JSON):
          ```json
          {
              "detected_style": "string",
              "detected_colors": [
                  {"source": "string", "name": "string", "hex": "string", "rgb": [255,255,255]}
              ],
              "recommendations": [
                  {"type": "string", "name": "string", "hex": "string", "reason": "string"}
              ]
          }
          ```
    - `POST /recommend_outfit`:
        - **Description**: Accepts a shoe image or `.glb` model and returns outfit text description.
        - **Request Content-Type**: `multipart/form-data`
        - **Body parameter**: `file` (binary/image or .glb model)
        - **Response Schema** (JSON):
          ```json
          {
              "shoe_style": "string",
              "shoe_color": "string",
              "shoe_hex": "string",
              "recommendation_text": "string"
          }
          ```
    - `POST /validate_outfit_image`:
        - **Description**: Validates an outfit with shoes and provides styling feedback.
        - **Request Content-Type**: `multipart/form-data`
        - **Body parameter**: `file` (binary/image)
        - **Response Schema** (JSON):
          ```json
          {
              "outfit_style": "string",
              "shoe_style": "string",
              "shoe_color": "string",
              "feedback": "string"
          }
          ```

### 2. [color_extractor.py](file:///d:/outfit_rec/backend/color_extractor.py) (The Eye)
- **Logic**:
    - Runs YOLO segmentation to isolate "Person" or specific garments.
    - Filters background and skin tones.
    - Uses K-Means to find dominant RGB colors.
    - **Features**:
        - Dynamic `webcolors` mapping.
        - Debug mode to visualize what the AI detects.

### 3. [extract_features.py](file:///d:/outfit_rec/backend/extract_features.py) (The Brain)
- **Logic**:
    - Uses OpenAI's CLIP model to classify images into styles: ["formal", "casual", "sporty", "boots"].
    - Used by both workflows to process outfit or shoe images.

### 4. [recommend.py](file:///d:/outfit_rec/backend/recommend.py) (The Stylist - Outfit to Shoe)
- **Logic**:
    - **Inputs**: Outfit Colors + Style + Accessories (Belt/Watch/Tie).
    - **Accessory Matching**:
        - *Belt/Watch*: Prioritizes shoes that match leather accessories (The "Golden Rule").
        - *Tie*: Presence of a tie automatically enforces "Formal" style.
    - **Color Rules**:
        - *Monochromatic/Dark*: Suggests High Contrast (White/Light).
        - *Light/Pale*: Suggests Grounding Colors (Black/Navy).
        - *Colorful*: Suggests Neutrals to avoid clashing.
    - **Style Rules**: Maps detected style to shoe types (e.g., Formal -> Oxfords).

### 5. [outfit_generator.py](file:///d:/outfit_rec/backend/outfit_generator.py) (The Reverse Stylist - Shoe to Outfit)
- **Logic**:
    - **Input**: Shoe Image (`.jpg`, `.png`) OR 3D Model (`.glb`, `.gltf`).
    - **3D Processing**: Extracts base texture from GLB models using `trimesh`.
    - **Process**:
        1.  Detects Shoe Style (e.g., "sporty running shoes").
        2.  Extracts Shoe Color (Center crop analysis).
        3.  Generates a text-based outfit recommendation based on fashion templates.

### 6. Utility Scripts
- **[run_shoe_recommender.py](file:///d:/outfit_rec/backend/run_shoe_recommender.py)**: CLI tool to test "Outfit -> Shoe" flow locally.
- **[run_outfit_generator.py](file:///d:/outfit_rec/backend/run_outfit_generator.py)**: CLI tool to test "Shoe -> Outfit" flow.
- **[verify_model.py](file:///d:/outfit_rec/backend/verify_model.py)**: Checks if YOLO model loads correctly.
- **[explore_glb.py](file:///d:/outfit_rec/backend/explore_glb.py)**: Debug tool to inspect GLB file structures and textures.
- **[test_accessories.py](file:///d:/outfit_rec/backend/test_accessories.py)**: Unit tests for accessory matching logic.

## Run Instructions

**Prerequisites**: Python 3.11 installed.

1.  **Navigate to Backend**:
    ```powershell
    cd d:/outfit_rec/backend
    ```

2.  **Run Server**:
    ```powershell
    py -3.11 -m uvicorn api:app --reload
    ```
    *Server will start at `http://127.0.0.1:8000`*

3.  **CLI Testing**:
    - **Recommend Shoes**:
      ```powershell
      py -3.11 run_shoe_recommender.py [path_to_outfit.jpg]
      ```
    - **Generate Outfit from Shoe**:
      ```powershell
      py -3.11 run_outfit_generator.py [path_to_shoe.jpg_or_model.glb]
      ```
    - **Run Feedback Engine (Validation)**:
      *(Note: Requires the FastAPI server to be running)*
      ```powershell
      py -3.11 test_validate_image.py [path_to_image.jpg]
      ```

## Known Constraints
- **Python Version**: Must use Python 3.11 due to `numpy`/`opencv` compatibility.
- **3D Models**: GLB files must have a texture map (BaseColorTexture) to be color-analyzed. Vertex colors are not yet supported.
- **Performance**: First request loads heavy AI models (CLIP/YOLO), subsequent requests are faster.
