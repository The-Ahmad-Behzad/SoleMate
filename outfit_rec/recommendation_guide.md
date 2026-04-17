# SoleMate Outfit Recommender: Complete Technical Guide

This document provides a comprehensive explanation of how the SoleMate Outfit Recommender system processes an image, extracts outfit features, and mathematically determines the best shoe recommendations. It is a deep-dive into the backend operations, specifically focusing on the intersection of computer vision, programmatic logic, and established menswear fashion theory.

---

## System Architecture

The recommendation engine operates as a pipeline passing through three core components:

1. **The Eye (`color_extractor.py`)**: Uses object detection to isolate individual garments and extract their dominant colors.
2. **The Brain (`extract_features.py`)**: Uses an AI model (CLIP) to analyze the image holistically and determine its "formality".
3. **The Stylist (`recommend.py`)**: A rule-based engine that marries the extracted colors with the detected style to pick the exact shoe type and color based on universally accepted style principles.

---

## 1. Object Detection & Color Extraction

When an image is passed to `extract_outfit_colors` in `color_extractor.py`, it goes through a multi-step extraction process:

### A. AI Segmentation (YOLOv8)
The system uses the `yolov8n-fashionpedia.onnx` model (an object detection model specifically trained on fashion) to locate distinct items in the image. It places bounding boxes around:
* **Upper Body** (Shirts, Jackets, Coats)
* **Lower Body** (Pants, Shorts, Skirts)
* **Accessories** (Belts, Watches, Ties, Glasses)

### B. Color Clustering (K-Means)
Once a distinct item (e.g., a shirt) is isolated and cropped:
1. The cropped image is converted into an array of RGB pixels.
2. If the crop is very large, it randomly samples 5,000 pixels to maintain high performance.
3. The pixels are fed into a **K-Means Clustering** algorithm to group similar colors together and extract the **Top 2 Dominant Colors** for that specific item.

### C. Smart Accessory Parsing & Color Identification
Accessories are notoriously difficult to analyze (e.g., a watch face might be white, but the leather strap is brown). The system specifically handles this:
* **Leather Check**: If evaluating a watch or a belt, it specifically checks if either of the top 2 colors is a "leather" color (like Brown). If the secondary color is brown leather, it swaps it to priority #1, entirely ignoring the metal/white watch face.
* **Translation**: The raw RGB values are converted mathematically into HLS (Hue, Lightness, Saturation) and compared against a CSS3/custom fashion vocabulary (e.g., "Olive/Khaki", "Navy Blue", "Maroon", "Beige").

---

## 2. Style Detection

While the color extractor tears the image apart, `classify_outfit_style` in `extract_features.py` looks at the outfit as a whole using OpenAI's **CLIP model**.

* **Zero-Shot Classification**: Instead of being hardcoded to recognize a "t-shirt", the model is fed the entire image and asked to compare its similarity to five text prompts: `"casual outfit"`, `"formal outfit"`, `"sporty outfit"`, `"business casual outfit"`, and `"streetwear outfit"`.
* It calculates probability scores for each phrase and selects the highest one, boiling it down to a simple internal status: `formal`, `casual`, or `sporty`.

---

## 3. The Recommendation Logic & Fashion Theory

Once the colors and the style are identified, the data is passed to `recommend_shoes` in `recommend.py`. This is where the programmatic logic intentionally mimics the universal rules of classic men's style (as codified in professional styling guides, tailoring theory, and modern fashion standards):

### Step 1: Formality Overrides
Before looking at colors, the system checks for hard rules:
* **The Tie Override**: If object detection found a tie (`source == 'tie'`), the system forces the outfit style to `"formal"`.

Based on the final style state, it prepares a bucket of shoe shapes to use later:
* **Formal**: Oxfords, Derbies, Loafers, Chelsea Boots
* **Sporty**: Running Shoes, Trainers, Sport Sandals
* **Casual**: Sneakers, Canvas Shoes, Slip-ons, Desert Boots

### Step 2: HLS Math & Contrast Strategies (The Core Engine)
The system employs multiple strategies sequentially to build a list of suggested shoe colors based directly on real-world style rules:

#### Strategy A: The Golden Rule (Match Leathers)
* **The Code Concept:** If a belt or watch is detected, prioritize matching its exact string hex color to the shoes.
* **The Fashion Theory:** This is the most unbreakable foundational rule in classic menswear. If you wear a brown leather belt, your shoes *must* be brown leather. A black watch strap requires black shoes. The system programmatically forces this logic to the top of the recommendation list to ensure the wearer always adheres to standard dress etiquette.

#### Strategy B: Top/Shirt Contrast & "Grounding"
The system converts the primary shirt's base RGB color into HLS (Hue, Lightness, Saturation) to mathematically understand its "lightness" (`l`) and "saturation" (`s`).
* **White/Light Shirts:**
    * **The Code Concept:** If `Lightness > 0.85`, purposefully append `Black` and `Navy` to the recommendations.
    * **The Fashion Theory:** This implements the principle of "visual grounding." A very light top has low visual weight. Adding dark shoes (like Navy or Black) anchors the outfit to the floor, preventing the wearer from looking top-heavy or unbalanced.
* **Dark/Black Shirts:**
    * **The Code Concept:** If `Lightness < 0.15`, push `White` and `Light Gray`.
    * **The Fashion Theory:** This creates visual separation. Wearing a dark shirt with dark pants and dark shoes often results in the wearer looking like a "black hole" or uniform. Injecting a contrasting light shoe breaks up the silhouette.
* **Colorful/Saturated Shirts:**
    * **The Code Concept:** For saturated colors, recommend safe neutrals (`Black` or `Beige`). Additionally, perform a dynamic mathematical calculation (`Current Hue + 0.5`) on the color wheel.
    * **The Fashion Theory:** Neutrals ensure the outfit does not clash, while the `+0.5 Hue` calculation generates a "Complementary Color Pop," a staple color-theory technique for vibrant streetwear and modern style.

#### Strategy C: Trouser Integration (Seamless Look)
* **The Code Concept:** Add the dynamically extracted color of the **Lower Body** to the shoe suggestion pool.
* **The Fashion Theory:** This executes a classic tailoring trick. Matching your shoe color closely to your trouser color creates an uninterrupted vertical line from the waist down, naturally elongating the leg and creating a slimmer silhouette.

#### Strategy D: The Neutral Guarantee
* **The Code Concept:** Append `White` to every output list as a universal baseline.
* **The Fashion Theory:** Over the last decade, the minimalist white sneaker has dominated modern casual dressing. It is universally accepted as a safe, stylish shoe that pairs effortlessly with practically any casual or inherently casual-leaning business look.

### Step 3: Final Output Assembly
Finally, the system de-duplicates all the generated color ideas. For every shoe type determined in Step 1 (e.g., Loafers, Oxfords), it matches the shape with the top 3 deduced colors and outputs the final JSON containing the `type`, `color_name`, `hex` code, and the human-readable `reason` that triggered the match (which explicitly references these classic fashion rules).
