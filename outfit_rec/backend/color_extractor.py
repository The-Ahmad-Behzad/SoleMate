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
    
    # Use KMeans to find clusters
    kmeans = KMeans(n_clusters=num_colors, n_init="auto", random_state=42)
    labels = kmeans.fit_predict(pixels)
    centers = kmeans.cluster_centers_.astype(int)
    
    # Calculate dominance (percentage of pixels in each cluster)
    total_pixels = len(pixels)
    unique_labels, counts = np.unique(labels, return_counts=True)
    
    # Combine into a list of tuples: (count, center_rgb)
    weighted_colors = []
    for label, count in zip(unique_labels, counts):
        percentage = count / total_pixels
        # Filter out negligible colors (e.g. less than 5%)
        # Adjust threshold as needed. 5% = 0.05
        if percentage < 0.05:
            continue
            
        weighted_colors.append({
            "count": count,
            "center": centers[label],
            "percentage": percentage
        })
        
    # Sort by count (descending) -> Most dominant first
    weighted_colors.sort(key=lambda x: x['count'], reverse=True)
    
    # Format output
    result = []
    for item in weighted_colors:
        rgb = tuple(map(int, item['center']))
        hex_val = "#{:02x}{:02x}{:02x}".format(*rgb)
        name = closest_color(rgb)
        
        result.append({
            "rgb": rgb,
            "hex": hex_val,
            "name": name,
            "dominance": round(item['percentage'] * 100, 2)
        })
        
    return result

# ---------------------------
# Extract outfit pixels & colors
# ---------------------------
def extract_outfit_colors(image_path, num_colors=3, debug_filename=None):
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

    # Combine all person masks
    masks = results.masks.data
    full_mask = np.zeros((h, w), dtype=bool)

    # Resize masks to match image size
    for m in masks:
        m_np = m.cpu().numpy().astype(float)
        m_resized = cv2.resize(m_np, (w, h)) > 0.5
        full_mask |= m_resized

    # ---------------------------
    # Skin Filtering
    # ---------------------------
    # Convert to HSV for skin detection
    img_hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    
    # Define skin color range in HSV
    lower_skin = np.array([0, 20, 70], dtype=np.uint8)
    upper_skin = np.array([20, 255, 255], dtype=np.uint8)
    
    skin_mask = cv2.inRange(img_hsv, lower_skin, upper_skin)
    
    # Dilate skin mask
    kernel = np.ones((3,3), np.uint8)
    skin_mask = cv2.dilate(skin_mask, kernel, iterations=2)
    skin_bool = skin_mask > 0
    
    # Remove skin from full mask first
    valid_outfit_mask = full_mask.copy()
    valid_outfit_mask[skin_bool] = False
    
    # ---------------------------
    # Smart Segmentation (Clustering)
    # ---------------------------
    # We want to separate the valid pixels into 2 clusters (Top vs Bottom)
    # Steps:
    # 1. Get pixels from valid_outfit_mask
    # 2. Run KMeans(k=2)
    # 3. Create two sub-masks
    # 4. Check spatial position (Centroid Y)
    
    valid_pixels = img_rgb[valid_outfit_mask]
    
    if len(valid_pixels) < 100:
        # Fallback if almost everything was masked out
        print("Warning: Not enough pixels after skin masking. Using full mask.")
        valid_outfit_mask = full_mask 
        
    # Get coordinates of valid pixels to reconstruct masks later
    # np.where returns (row_indices, col_indices)
    ys, xs = np.where(valid_outfit_mask)
    
    # If we still have no pixels, return empty
    if len(ys) == 0:
        return []
        
    # Stack features for clustering: Color (RGB) + Spatial (Y)?
    # For now, let's try just Color to separate distinct garments. 
    # Spatial info is used AFTER to label them.
    pixel_values = img_rgb[ys, xs] # shape (N, 3)
    
    try:
        # Run K-Means with k=2 to find "Shirt Color" and "Pant Color"
        kmeans_seg = KMeans(n_clusters=2, n_init="auto", random_state=42)
        labels = kmeans_seg.fit_predict(pixel_values)
        
        # reconstruct masks
        mask_a = np.zeros_like(full_mask)
        mask_b = np.zeros_like(full_mask)
        
        mask_a[ys[labels == 0], xs[labels == 0]] = True
        mask_b[ys[labels == 1], xs[labels == 1]] = True
        
        # Calculate centroids (Average Y) to determine Up/Down
        center_a = np.mean(ys[labels == 0]) if np.any(labels == 0) else 0
        center_b = np.mean(ys[labels == 1]) if np.any(labels == 1) else 0
        
        if center_a < center_b:
            # A is higher (Smaller Y) -> Upper Logic
            upper_mask = mask_a
            lower_mask = mask_b
        else:
            upper_mask = mask_b
            lower_mask = mask_a
            
    except Exception as e:
        print(f"Clustering failed: {e}. Fallback to spatial split.")
        # Fallback to simple split
        rows = np.any(valid_outfit_mask, axis=1)
        if not np.any(rows): return []
        rmin, rmax = np.where(rows)[0][[0, -1]]
        split_point = rmin + int((rmax - rmin) * 0.5)
        upper_mask = valid_outfit_mask.copy()
        upper_mask[split_point:, :] = False
        lower_mask = valid_outfit_mask.copy()
        lower_mask[:split_point, :] = False

    # Debug: Save visualization
    if debug_filename:
        debug_img = img.copy()
        # Tint Upper Body Green
        debug_img[upper_mask] = debug_img[upper_mask] * 0.5 + np.array([0, 255, 0]) * 0.5
        # Tint Lower Body Red
        debug_img[lower_mask] = debug_img[lower_mask] * 0.5 + np.array([0, 0, 255]) * 0.5
        # Tint Skin Blue
        skin_inside_person = skin_bool & full_mask
        debug_img[skin_inside_person] = debug_img[skin_inside_person] * 0.5 + np.array([255, 0, 0]) * 0.5
        
        cv2.imwrite(debug_filename, debug_img)
        print(f"Saved debug image to {debug_filename}")

    final_colors = []

    # Extract Upper Body Colors (from the refined mask)
    upper_pixels = img_rgb[upper_mask]
    if len(upper_pixels) > 0:
        # Get purely dominant colors within this zone
        upper_colors = extract_dominant_colors(upper_pixels, num_colors=num_colors)
        for c in upper_colors:
            c['source'] = 'shirt'
        final_colors.extend(upper_colors)

    # Extract Lower Body Colors
    lower_pixels = img_rgb[lower_mask]
    if len(lower_pixels) > 0:
        lower_colors = extract_dominant_colors(lower_pixels, num_colors=num_colors)
        for c in lower_colors:
            c['source'] = 'pant'
        final_colors.extend(lower_colors)

    return final_colors

# ---------------------------
# Test
# ---------------------------
if __name__ == "__main__":
    colors = extract_outfit_colors("sample_outfit.jpg", num_colors=2, debug_filename="debug_split.jpg")
    print("Detected outfit colors:")
    for c in colors:
        print(f"{c['source']}: {c['name']} ({c['hex']})")
