import trimesh
import os
import numpy as np
from PIL import Image

# Path provided by user
glb_path = r"D:\SoleMate\android\app\src\main\assets\models\shoes\airmax_270_left.glb"

if not os.path.exists(glb_path):
    print(f"Error: File not found at {glb_path}")
    exit(1)

print(f"Loading {glb_path}...")
scene = trimesh.load(glb_path)

print("Values in scene:", scene)

# Try to find textures
textures_found = 0

# Scene can contain geometry directly or in a graph
for name, geom in scene.geometry.items():
    print(f"\nGeometry: {name}")
    if hasattr(geom, 'visual'):
        vis = geom.visual
        print(f"  Visual Type: {type(vis)}")
        
        if hasattr(vis, 'material'):
            mat = vis.material
            print(f"  Material: {mat}")
            
            # Check for PBR material textures
            if hasattr(mat, 'baseColorTexture') and mat.baseColorTexture:
                print("  [MATCH] Found BaseColorTexture!")
                img = mat.baseColorTexture
                # Convert to RGB to be safe and usable for analysis
                img = img.convert("RGB")
                # Save as PNG to support transparency
                img.save("debug_texture.png")
                print("  Saved texture to debug_texture.png")
                textures_found += 1
            
            # Check for standard image material
            elif hasattr(mat, 'image') and mat.image:
                print("  [MATCH] Found Material Image!")
                # Save as PNG to support transparency
                mat.image.save("debug_texture.png")
                print("  Saved texture to debug_texture.png")
                textures_found += 1

if textures_found == 0:
    print("No textures found. Might be vertex colors.")
else:
    print(f"Found {textures_found} textures.")
