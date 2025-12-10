import requests
import pprint
import sys

# URL of the local API
url = "http://127.0.0.1:8000/recommend"

# Image to test
image_path = "sample_outfit.jpg"

try:
    print(f"Sending {image_path} to {url}...")
    with open(image_path, "rb") as f:
        files = {"file": f}
        response = requests.post(url, files=files)
    
    if response.status_code == 200:
        print("\n--- Success! Response: ---")
        pprint.pprint(response.json())
    else:
        print(f"\nError {response.status_code}: {response.text}")

except FileNotFoundError:
    print(f"Error: Could not find {image_path}. Make sure you are in the 'backend' folder.")
except Exception as e:
    print(f"An error occurred: {e}")
