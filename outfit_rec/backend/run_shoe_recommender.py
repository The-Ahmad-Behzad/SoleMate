# run_shoe_recommender.py
import sys
import os
import pprint

# Import backend logic
from color_extractor import extract_outfit_colors
from recommend import recommend_shoes
from extract_features import classify_outfit_style # Auto-detect style

def main():
    if len(sys.argv) < 2:
        print("Usage: python run_shoe_recommender.py <path_to_outfit_image> [style]")
        print("Example: python run_shoe_recommender.py my_shirt.jpg")
        sys.exit(1)

    image_path = sys.argv[1]
    
    if not os.path.exists(image_path):
        print(f"Error: File '{image_path}' not found.")
        sys.exit(1)

    # Auto-detect style if not provided
    if len(sys.argv) > 2:
        style = sys.argv[2]
        print(f"Using provided style: {style}")
    else:
        print("Auto-detecting outfit style...")
        try:
            style = classify_outfit_style(image_path)
            print(f"Detected Style: {style.upper()}")
        except Exception as e:
            print(f"Style detection failed ({e}), defaulting to 'casual'.")
            style = "casual"

    print(f"\nAnalyzing outfit in '{image_path}'...")

    # 1. Extract Colors (using Fashionpedia logic)
    colors = extract_outfit_colors(image_path, debug_filename="debug_detection.jpg")
    
    if not colors:
        print("No colors detected!")
        sys.exit(0)

    print("\n--- Detected Components ---")
    for c in colors:
        print(f"[{c['source'].upper()}] {c['name']} (Dominance: {c.get('dominance')}%)")

    # 2. Recommend
    recommendations = recommend_shoes(colors, style)

    print("\n--- Shoe Recommendations ---")
    for i, rec in enumerate(recommendations, 1):
        print(f"{i}. {rec['type']} ({rec['color_name']})")
        print(f"   Reason: {rec['reason']}")

    print("\nDebug image saved to 'debug_detection.jpg'")

if __name__ == "__main__":
    main()
