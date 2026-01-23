from fastapi import FastAPI, UploadFile, File, HTTPException
import shutil
import os
import uuid
from typing import List

from color_extractor import extract_outfit_colors
from extract_features import classify_outfit_style
from recommend import recommend_shoes
from outfit_generator import generate_outfit_from_shoe

app = FastAPI(title="SoleMate Outfit Recommender API")

UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

@app.post("/recommend")
async def get_recommendations(file: UploadFile = File(...)):
    # 1. Save uploaded file
    file_extension = file.filename.split(".")[-1]
    filename = f"{uuid.uuid4()}.{file_extension}"
    file_path = os.path.join(UPLOAD_DIR, filename)
    
    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
        
    try:
        # 2. Extract Colors
        # Using default component filtering (Person class)
        colors = extract_outfit_colors(file_path, num_colors=2)
        
        # 3. Detect Style
        style = classify_outfit_style(file_path)
        
        # 4. Generate Recommendations
        recommendations = recommend_shoes(colors, style)
        
        return {
            "detected_style": style,
            "detected_colors": colors,
            "recommendations": recommendations
        }
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        # Cleanup
        if os.path.exists(file_path):
            os.remove(file_path)

@app.post("/recommend_outfit")
async def recommend_outfit_from_shoe(file: UploadFile = File(...)):
    # 1. Save uploaded file
    file_extension = file.filename.split(".")[-1]
    filename = f"shoe_{uuid.uuid4()}.{file_extension}"
    file_path = os.path.join(UPLOAD_DIR, filename)
    
    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
        
    try:
        # 2. Generate Outfit
        result = generate_outfit_from_shoe(file_path)
        return result
        
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        # Cleanup
        if os.path.exists(file_path):
            os.remove(file_path)

@app.get("/")
def home():
    return {"message": "SoleMate API is running. POST /recommend to get suggestions."}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
