# color_extractor_corrected.py
import cv2
import numpy as np
from sklearn.cluster import KMeans
from ultralytics import YOLO
import webcolors

# ---------------------------
# Closest CSS3 color name
# ---------------------------
def closest_color(requested_rgb):
    min_dist = float("inf")
    closest_name = None
    
    # Build dictionary dynamically as attributes changed in recent versions
    css3_map = {name: webcolors.name_to_hex(name) for name in webcolors.names("css3")}
    
    for name, hex_code in css3_map.items():
        r, g, b = webcolors.hex_to_rgb(hex_code)
        dist = (r - requested_rgb[0])**2 + (g - requested_rgb[1])**2 + (b - requested_rgb[2])**2
        if dist < min_dist:
            min_dist = dist
            closest_name = name
    return closest_name

# ---------------------------
# KMeans dominant colors
# ---------------------------
def extract_dominant_colors(pixels, num_colors=3):
    if len(pixels) == 0:
        return []
    kmeans = KMeans(n_clusters=num_colors, n_init="auto")
    kmeans.fit(pixels)
    colors = kmeans.cluster_centers_.astype(int)

    result = []
    result = []
    for color in colors:
        # Convert numpy int64 to native python int for JSON serialization
        rgb = tuple(map(int, color))
        hex_val = "#{:02x}{:02x}{:02x}".format(*rgb)
        name = closest_color(rgb)
        result.append({
            "rgb": rgb,
            "hex": hex_val,
            "name": name
        })
    return result

# ---------------------------
# Extract outfit pixels & colors
# ---------------------------
def extract_outfit_colors(image_path, num_colors=3, shirt_only=True, debug_filename=None):
    # Load YOLO segmentation model
    model = YOLO("yolov8n-seg.pt")  # small model

    # Run segmentation - Filter for class 0 (person)
    results = model.predict(image_path, classes=[0], verbose=False)[0]

    if not results.masks or len(results.masks.data) == 0:
        print("No person detected.")
        return []

    # Load original image
    img = cv2.imread(image_path)
    img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    h, w = img_rgb.shape[:2]

    # Combine all person masks (in case multiple people or fragmented masks, though we ideally want the main subject)
    # We'll assume the largest mask is the main subject if multiple are found
    
    masks = results.masks.data
    full_mask = np.zeros((h, w), dtype=bool)

    # Resize masks to match image size first because YOLO masks might be smaller
    for m in masks:
        m_np = m.cpu().numpy().astype(float) # convert to float for resizing
        m_resized = cv2.resize(m_np, (w, h)) > 0.5
        full_mask |= m_resized

    # Optionally crop to upper body (shirt) 
    # Logic: Simply cut the bottom half of the bounding box of the person, not the whole image
    if shirt_only:
        # Find bounding box of the mask
        rows = np.any(full_mask, axis=1)
        cols = np.any(full_mask, axis=0)
        if not np.any(rows) or not np.any(cols):
            return [] # Empty mask
            
        rmin, rmax = np.where(rows)[0][[0, -1]]
        cmin, cmax = np.where(cols)[0][[0, -1]]
        
        # Keep top 60% of the person's height for shirt/upper body
        person_height = rmax - rmin
        split_point = rmin + int(person_height * 0.60)
        
        full_mask[split_point:, :] = False

    # Debug: Save visualization if requested
    if debug_filename:
        # Create a black image
        debug_img = np.zeros_like(img)
        # Copy only the masked area from the original image
        debug_img[full_mask] = img[full_mask]
        cv2.imwrite(debug_filename, debug_img)
        print(f"Saved debug image to {debug_filename}")

    # Extract pixels inside mask
    outfit_pixels = img_rgb[full_mask]
    
    if len(outfit_pixels) == 0:
         return []

    # Extract dominant colors
    return extract_dominant_colors(outfit_pixels, num_colors=num_colors)

# ---------------------------
# Test
# ---------------------------
if __name__ == "__main__":
    colors = extract_outfit_colors("sample_outfit.jpg", num_colors=3, shirt_only=True)
    print("Detected outfit colors:")
    for c in colors:
        print(c)
