import requests
import sys
import pprint

def test_validation(image_path):
    url = "http://127.0.0.1:8000/validate_outfit_image"
    try:
        with open(image_path, "rb") as f:
            files = {"file": (image_path, f, "image/jpeg")}
            response = requests.post(url, files=files)
            
        if response.status_code == 200:
            print("Successfully Analyzed Image!\n")
            result = response.json()
            print(f"Overall Outfit Style: {result.get('outfit_style')}")
            print(f"Detected Shoe Style: {result.get('shoe_style')}")
            print(f"Detected Shoe Color: {result.get('shoe_color')}\n")
            print("--- Feedback ---")
            pprint.pprint(result.get('feedback'))
        else:
            print(f"Error {response.status_code}: {response.text}")
            
    except Exception as e:
         print(f"Connection Error: {e}. Is the server running?")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python test_validate_image.py <path_to_image>")
        sys.exit(1)
        
    test_validation(sys.argv[1])
