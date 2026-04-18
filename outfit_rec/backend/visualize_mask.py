from color_extractor import extract_outfit_colors

image_path = "sample_outfit.jpg"
output_path = "debug_output.jpg"

print(f"Processing {image_path}...")
colors = extract_outfit_colors(image_path, debug_filename=output_path)

print("\n--- Detected Colors (Sorted by Dominance) ---")
for c in colors:
    print(f"[{c['source'].upper()}] {c['name']} ({c['hex']}) - {c.get('dominance', 0)}%")

print("\nDONE!")
print(f"Check the file '{output_path}' in this directory.")
print("The image should show Shirt (Green Tint) and Pant (Red Tint).")
print("Blue Tint areas are DETECTED SKIN (Ignored).")
print("Untouched areas are background.")
