import urllib.request
import urllib.error
import sys
import os
import json
import uuid

def post_multipart(url, file_path):
    """
    Sends a file via POST using standard library urllib (no requests dependency).
    """
    filename = os.path.basename(file_path)
    boundary = uuid.uuid4().hex
    
    with open(file_path, 'rb') as f:
        file_content = f.read()
        
    # Build multipart body
    # Using specific line endings as per spec
    part_boundary = f'--{boundary}'
    
    parts = [
        part_boundary.encode('utf-8'),
        f'Content-Disposition: form-data; name="file"; filename="{filename}"'.encode('utf-8'),
        b'Content-Type: application/octet-stream',
        b'',
        file_content,
        f'{part_boundary}--'.encode('utf-8'),
        b''
    ]
    
    body = b'\r\n'.join(parts)
    
    headers = {
        'Content-Type': f'multipart/form-data; boundary={boundary}',
        'Content-Length': str(len(body))
    }
    
    req = urllib.request.Request(url, data=body, headers=headers, method='POST')
    
    try:
        with urllib.request.urlopen(req) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8')
    except Exception as e:
        return 500, str(e)

def test_outfit_to_shoe(image_path, url="http://127.0.0.1:8000/recommend"):
    print(f"\n--- Testing OUTFIT -> SHOE on {image_path} ---")
    status, data = post_multipart(url, image_path)
    
    if status == 200:
        print("\n[SUCCESS]")
        print(f"Detected Style: {data.get('detected_style', 'Unknown').upper()}")
        
        print("\nDetected Colors:")
        for c in data.get("detected_colors", []):
            print(f"  [{c.get('source', 'UNKNOWN').upper()}] {c.get('name')} ({c.get('hex')}) - {c.get('dominance')}%")
            
        print("\nRecommendations:")
        for r in data.get("recommendations", []):
            print(f"  [{r.get('type')}] {r.get('color_name')} -> {r.get('reason')}")
    else:
        print(f"Error {status}: {data}")

def test_shoe_to_outfit(image_path, url="http://127.0.0.1:8000/recommend_outfit"):
    print(f"\n--- Testing SHOE -> OUTFIT on {image_path} ---")
    status, data = post_multipart(url, image_path)
            
    if status == 200:
        print("\n[SUCCESS]")
        print(f"Shoe Style: {data.get('shoe_style')}")
        print(f"Shoe Color: {data.get('shoe_color')}")
        print(f"Shoe Hex: {data.get('shoe_hex', 'N/A')}")
        print(f"Recommendation: {data.get('recommendation_text')}")
    else:
        print(f"Error {status}: {data}")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage:")
        print("  py test_api.py outfit <image_path>")
        print("  py test_api.py shoe <image_path>")
        sys.exit(1)
        
    mode = sys.argv[1]
    img = sys.argv[2]
    
    if not os.path.exists(img):
        print(f"Error: File {img} not found.")
        sys.exit(1)
        
    if mode == "outfit":
        test_outfit_to_shoe(img)
    elif mode == "shoe":
        test_shoe_to_outfit(img)
    else:
        print("Invalid mode. Use 'outfit' or 'shoe'.")
