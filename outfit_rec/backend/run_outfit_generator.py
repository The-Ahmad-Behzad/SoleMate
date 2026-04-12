# run_outfit_generator.py
import sys
import os
import pprint

# Import backend logic
from outfit_generator import generate_outfit_from_shoe

def main():
    if len(sys.argv) < 2:
        print("Usage: python run_outfit_generator.py <path_to_shoe_image>")
        print("Example: python run_outfit_generator.py my_sneakers.jpg")
        sys.exit(1)

    image_path = sys.argv[1]

    if not os.path.exists(image_path):
        print(f"Error: File '{image_path}' not found.")
        sys.exit(1)

    print(f"Analyzing shoe in '{image_path}'...")

    try:
        # Generate Outfit
        result = generate_outfit_from_shoe(image_path)
        
        print("\n--- Analysis ---")
        print(f"Shoe Style: {result['shoe_style']}")
        print(f"Shoe Color: {result['shoe_color']}")
        
        print("\n--- Outfit Recommendation ---")
        print(result['recommendation_text'])
        
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
