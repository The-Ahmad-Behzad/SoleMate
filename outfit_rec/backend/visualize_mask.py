from color_extractor import extract_outfit_colors

image_path = "sample_outfit.jpg"
output_path = "debug_output.jpg"

print(f"Processing {image_path}...")
extract_outfit_colors(image_path, num_colors=3, shirt_only=True, debug_filename=output_path)

print("\nDONE!")
print(f"Check the file '{output_path}' in this directory.")
print("The parts of the image that are VISIBLE are what the AI uses for color detection.")
print("The BLACK areas are ignored (background).")
