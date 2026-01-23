import sys
import os
sys.path.append("d:/solemate_outfit/backend")

from color_extractor import extract_outfit_colors
from extract_features import classify_outfit_style
from recommend import recommend_shoes
import pprint

def test_pipeline():
    image_path = "d:/solemate_outfit/backend/sample_outfit.jpg"
    print(f"Testing with {image_path}...")
    
    # 1. Colors
    print("\n--- 1. Extracting Colors ---")
    colors = extract_outfit_colors(image_path, num_colors=3)
    if not colors:
        print("ERROR: No colors found or no person detected.")
    else:
        for c in colors:
            print(f"Color: {c['name']} ({c['hex']})")

    # 2. Style
    print("\n--- 2. Classifying Style ---")
    style = classify_outfit_style(image_path)
    print(f"Detected Style: {style}")
    
    # 3. Recommendations
    print("\n--- 3. Recommendations ---")
    recs = recommend_shoes(colors, style)
    pprint.pprint(recs)

if __name__ == "__main__":
    test_pipeline()
