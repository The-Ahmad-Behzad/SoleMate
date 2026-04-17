# color_extractor.py
import cv2
import numpy as np
from sklearn.cluster import KMeans
from ultralytics import YOLO
import webcolors
import os

# ---------------------------
# Closest CSS3 color name
# ---------------------------
# ---------------------------
# Closest CSS3 color name (Customized for Fashion)
# ---------------------------
def closest_color(requested_rgb):
    r, g, b = requested_rgb
    
    # Convert to HSV/HLS for better logic
    import colorsys
    h, l, s = colorsys.rgb_to_hls(r/255.0, g/255.0, b/255.0)
    
    # H: 0-1 (0=Red, 0.33=Green, 0.66=Blue)
    # L: 0-1 (Lightness)
    # S: 0-1 (Saturation)
    
    # --- CUSTOM FASHION LOGIC ---
    
    # 1. Very Dark Colors (Navy vs Black vs Dark Brown)
    if l < 0.20:
        if s < 0.15:
            return "Black" # True Neutral
        
        # Check hues
        if (h < 0.1 or h > 0.9): return "Deep Maroon" if s > 0.3 else "Dark Brown" # Red Hue
        if (0.55 < h < 0.75): return "Navy Blue" # Blue Hue
        if (0.2 < h < 0.45): return "Dark Olive" # Green hue
        
        return "Black" # Default fallback for dark
        
    # 2. Light Neutrals (Beige vs White vs Light Gray)
    if l > 0.80:
        if s < 0.10: return "White"
        if (0.05 < h < 0.2): return "Beige" # Yellow-ish tint implies Beige/Cream
        return "Off-White"

    # 3. Mid-Tones
    
    # Olive / Khaki checks
    # Yellow/Green Hue + Moderate Saturation + Mid-Dark Lightness
    if (0.15 < h < 0.4) and (0.2 < l < 0.6):
        if s < 0.4: return "Olive/Khaki"
        return "Green"

    # Maroon / Burgundy
    if (h < 0.05 or h > 0.95) and l < 0.4:
         return "Maroon"

    # Leather Brown check (Legacy + HLS)
    # If Warm Hue + Mid/Low Light + Moderate Saturation
    if (h < 0.12 or h > 0.9) and (0.2 < l < 0.5) and s > 0.2:
        return "Brown"

    # Fallback to CSS3 Distance
    min_dist = float("inf")
    closest_name = None
    css3_map = {name: webcolors.name_to_hex(name) for name in webcolors.names("css3")}
    
    for name, hex_code in css3_map.items():
        cr, cg, cb = webcolors.hex_to_rgb(hex_code)
        dist = (cr - r)**2 + (cg - g)**2 + (cb - b)**2
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
    
    # Ensure pixels are typically handled even if small count
    n_points = len(pixels)
    if n_points < num_colors:
        num_colors = n_points
    
    kmeans = KMeans(n_clusters=num_colors, n_init="auto", random_state=42)
    labels = kmeans.fit_predict(pixels)
    centers = kmeans.cluster_centers_.astype(int)
    
    total_pixels = len(pixels)
    unique_labels, counts = np.unique(labels, return_counts=True)
    
    weighted_colors = []
    for label, count in zip(unique_labels, counts):
        percentage = count / total_pixels
        if percentage < 0.05: # Ignore < 5%
            continue
            
        weighted_colors.append({
            "count": count,
            "center": centers[label],
            "percentage": percentage
        })
        
    weighted_colors.sort(key=lambda x: x['count'], reverse=True)
    
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
def extract_outfit_colors(image_path, model_path="yolov8n-fashionpedia.onnx", debug_filename=None, extract_shoe_crop=False):
    """
    Extracts colors from Upper Body, Lower Body, and Accessories (Belt, Watch, Glasses, Shoe).
    Uses Fashionpedia model (Detection Mode).
    """
    
    # Check if model exists
    if not os.path.exists(model_path):
        print(f"Model not found at {model_path}. Please ensure valid model path.")
        return []

    # Load Model - force task='detect'
    try:
        model = YOLO(model_path, task='detect') 
    except Exception as e:
        print(f"Error loading model: {e}")
        return []
    
    # Predict
    results = model.predict(image_path, verbose=False)[0]
    
    if len(results.boxes) == 0:
        print("No objects detected.")
        return []

    # Load Image
    img = cv2.imread(image_path)
    img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    h_img, w_img = img_rgb.shape[:2]
    
    # Fashionpedia Class Mappings
    # Removed 13 (glasses) from UPPER to prevent overlap
    UPPER_BODY_CLASSES = [0, 1, 2, 3, 4, 5, 10, 12] 
    LOWER_BODY_CLASSES = [6, 7, 8, 11]
    
    # Accessories
    ACCESSORIES = {
        23: 'shoe',
        13: 'glasses',
        16: 'tie',
        18: 'watch',
        19: 'belt'
    }

    final_colors = []
    
    shoe_crop_img = None
    
    # Debug Visualization
    debug_img = img.copy() if debug_filename else None

    # Iterate over detections
    for i, box in enumerate(results.boxes):
        cls_id = int(box.cls[0].item())
        conf = float(box.conf[0].item())
        
        # Filter low confidence - Lowered to 0.15 for small accessories
        if conf < 0.15: 
            continue
            
        # Determine Source Category
        source = None
        if cls_id in UPPER_BODY_CLASSES:
            source = 'shirt'
        elif cls_id in LOWER_BODY_CLASSES:
            source = 'pant'
        elif cls_id in ACCESSORIES:
            source = ACCESSORIES[cls_id]
            
        if not source:
            continue
            
        # Get Bounding Box Coordinates
        x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
        
        # Clamp to image
        x1, y1 = max(0, x1), max(0, y1)
        x2, y2 = min(w_img, x2), min(h_img, y2)
        
        # Crop Image
        crop = img_rgb[y1:y2, x1:x2]
        if crop.size == 0: continue
        
        # Reshape for KMeans
        pixels = crop.reshape(-1, 3)
        
        # Optimization: If crop is huge, sample it
        if len(pixels) > 5000:
            indices = np.random.choice(len(pixels), 5000, replace=False)
            pixels = pixels[indices]
            
        # Extract 1 dominant color for accessories, 2 for garments
        # CHANGED: Extract 2 for accessories too, to catch strap vs face
        n_c = 2 
        
        extracted = extract_dominant_colors(pixels, num_colors=n_c)
        
        # Smart Filter for Accessories (Watch/Belt)
        # If we find a "Leather" colo (Browns) in the top 2, prioritize it over Silver/Gray/White
        if source in ['watch', 'belt'] and len(extracted) > 1:
            # Check if secondary is better
            c1 = extracted[0]
            c2 = extracted[1]
            
            is_c1_leather = "Brown" in c1['name']
            is_c2_leather = "Brown" in c2['name']
            
            # If primary is NOT leather but secondary IS, and secondary is significant (>20% of relative mass?), swap.
            # actually, just swap if found. Watch face (white) might be 60%, strap (brown) 40%. We want the strap.
            if (not is_c1_leather) and is_c2_leather:
                 # Swap: Make the leather color the primary one we report
                 extracted = [c2, c1]

        for i, c in enumerate(extracted):
            c['source'] = source
            # Boost priority for accessories
            if source not in ['shirt', 'pant']:
                c['dominance'] = 100
                
            # Only append the first/primary color for accessories to avoid cluttering the recommendation input?
            # Or append all? recommend.py logic might look at all.
            # But the 'outfit_colors' list usually expects multiple entries.
            # Let's append both, but since we swapped them, the first one (Leather) will be picked up by recommend.py logic
            # which usually takes the "first found" or iterates. 
            # Actually recommend.py iterates: `next((c for c in outfit_colors if c.get('source') == 'belt'), None)`
            # This `next` will pick the first one we append here.
            final_colors.append(c)
            
            # For accessories, maybe we only need the top one after swapping
            if source in ['watch', 'belt', 'glasses', 'tie']:
                break # Just take the "winner"
            
        # Draw on Debug Image
        if debug_img is not None:
            color_bgr = (0, 255, 0) # Default Green
            if source == 'pant': color_bgr = (0, 0, 255)
            elif source == 'belt': color_bgr = (255, 255, 0)
            elif source == 'watch': color_bgr = (255, 0, 255)
            
            cv2.rectangle(debug_img, (x1, y1), (x2, y2), color_bgr, 2)
            cv2.putText(debug_img, f"{source} {conf:.2f}", (x1, y1-5), cv2.FONT_HERSHEY_SIMPLEX, 0.5, color_bgr, 2)
            
        if source == 'shoe' and extract_shoe_crop and shoe_crop_img is None:
            # Save the crop
            from PIL import Image
            shoe_crop_img = Image.fromarray(crop)

    if debug_filename and debug_img is not None:
        cv2.imwrite(debug_filename, debug_img)
        print(f"Saved debug image to {debug_filename}")
            
    if extract_shoe_crop:
        return final_colors, shoe_crop_img
    return final_colors


# ---------------------------
# Test
# ---------------------------
if __name__ == "__main__":
    # Test
    print("Testing with sample...")
    cols = extract_outfit_colors("sample_outfit.jpg", debug_filename="debug_fashion.jpg")
    for c in cols:
        print(c)
