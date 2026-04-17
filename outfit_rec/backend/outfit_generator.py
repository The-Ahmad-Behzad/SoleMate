import cv2
import numpy as np
from extract_features import processor, model
from color_extractor import extract_dominant_colors
from PIL import Image
import torch

import trimesh

def extract_texture_from_glb(glb_path):
    """
    Extracts the first available baseColorTexture from a GLB file.
    Returns a PIL Image or None.
    """
    try:
        scene = trimesh.load(glb_path)
        
        # Iterate over geometry to find a texture
        for geom in scene.geometry.values():
            if hasattr(geom, 'visual') and hasattr(geom.visual, 'material'):
                mat = geom.visual.material
                if hasattr(mat, 'baseColorTexture') and mat.baseColorTexture:
                     return mat.baseColorTexture.convert("RGB")
                elif hasattr(mat, 'image') and mat.image:
                     return mat.image.convert("RGB")
                     
        return None
    except Exception as e:
        print(f"Error loading GLB: {e}")
        return None

def detect_shoe_style(image_input):
    """
    Classify the shoe image into specific categories.
    image_input: Path (str) or PIL Image
    """
    # Handle input type
    if isinstance(image_input, str):
         image = Image.open(image_input).convert("RGB")
    else:
         image = image_input

    styles = ["casual sneakers", "formal dress shoes", "sporty running shoes", "boots", "sandals"]
    
    inputs = processor(text=styles, images=image, return_tensors="pt", padding=True)
    
    with torch.no_grad():
        outputs = model(**inputs)
        probs = outputs.logits_per_image.softmax(dim=1)
        
    best_idx = probs.argmax().item()
    return styles[best_idx]

def generate_outfit_from_shoe(input_path):
    # Check if GLB
    valid_image = None
    
    if input_path.lower().endswith('.glb') or input_path.lower().endswith('.gltf'):
        print(f"Detected 3D Model: {input_path}")
        valid_image = extract_texture_from_glb(input_path)
        if valid_image is None:
             raise ValueError("Could not extract texture from 3D model.")
    else:
        valid_image = Image.open(input_path).convert("RGB")

    # 1. Detect Style
    shoe_style_full = detect_shoe_style(valid_image)
    
    # Simplify style for logic
    if "formal" in shoe_style_full or "dress" in shoe_style_full:
        style_category = "formal"
    elif "sporty" in shoe_style_full or "running" in shoe_style_full:
        style_category = "sporty"
    elif "boots" in shoe_style_full:
        style_category = "boots"
    else:
        style_category = "casual"

    # 2. Detect Color (Center crop logic for Image object)
    # Convert PIL to OpenCV (BGR) for consistency with existing color extractor
    img_np = np.array(valid_image)
    img_bgr = cv2.cvtColor(img_np, cv2.COLOR_RGB2BGR)
    
    h, w = img_bgr.shape[:2]
    # Crop center 50%
    cy, cx = h // 2, w // 2
    qy, qx = h // 4, w // 4
    center_crop = img_bgr[qy:cy+qy, qx:cx+qx]
    center_rgb = cv2.cvtColor(center_crop, cv2.COLOR_BGR2RGB)
    center_pixels = center_rgb.reshape(-1, 3)
    
    # Extract up to 2 dominant colors
    colors = extract_dominant_colors(center_pixels, num_colors=2)
    
    shoe_color_name = "Black"
    shoe_color_hex = "#000000"
    
    if colors:
        c1 = colors[0]
        shoe_color_name = c1['name']
        shoe_color_hex = c1['hex']
        
        # Check for second color if it's significant (generated extract_dominant_colors already filters < 5%)
        if len(colors) > 1:
            c2 = colors[1]
            # Verify dominance gap isn't massive (e.g. 90% vs 6%) - optional, but good for "White and Red" shoes
            shoe_color_name = f"{c1['name']} and {c2['name']}"
            shoe_color_hex = f"{c1['hex']}, {c2['hex']}"

    # 3. Generate Recommendation Logic
    recommendation = ""
    
    # Use the primary color for logic decisions
    primary_color_name = colors[0]['name'] if colors else "Black"
    
    if style_category == "formal":
        recommendation = f"A Formal outfit with Charcoal or Navy trousers and a Crisp White shirt would look great with your {shoe_color_name} dress shoes."
        
    elif style_category == "sporty":
        recommendation = f"A Sporty outfit with Black joggers and a Heather Gray performance tee matches perfectly with {shoe_color_name} running shoes."
        
    elif style_category == "boots":
        recommendation = f"A Rugged outfit with Dark Wash jeans and a Flannel shirt pairs well with these {shoe_color_name} boots."
        
    else: # Casual
        if primary_color_name.lower() in ['white', 'beige', 'lightgray', 'silver']:
             recommendation = f"A Casual outfit with Navy Chinos and a Maroon Polo matches your light {shoe_color_name} sneakers."
        elif primary_color_name.lower() in ['black', 'darkgray', 'darkslategray', 'navy']:
             recommendation = f"A Casual outfit with Light Blue jeans and a White T-Shirt contrasts nicely with your dark {shoe_color_name} sneakers."
        else: # Colorful
             recommendation = f"A Casual outfit with Neutral Beige pants and a White shirt lets your {shoe_color_name} sneakers stand out."
             
    return {
        "shoe_style": shoe_style_full,
        "shoe_color": shoe_color_name,
        "shoe_hex": shoe_color_hex,
        "recommendation_text": recommendation
    }

if __name__ == "__main__":
    # Test
    print("Testing with sample_outfit.jpg (Assuming it's a shoe for test)...")
    res = generate_outfit_from_shoe("sample_outfit.jpg")
    print(res)
