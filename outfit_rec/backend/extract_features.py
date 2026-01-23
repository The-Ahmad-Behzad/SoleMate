from transformers import CLIPProcessor, CLIPModel
from PIL import Image
import torch

# Load model once using transformers
# We use the same model weights but via Hugging Face Transformers
MODEL_ID = "patrickjohncyh/fashion-clip"

try:
    model = CLIPModel.from_pretrained(MODEL_ID)
    processor = CLIPProcessor.from_pretrained(MODEL_ID)
except Exception as e:
    # Fallback to standard OpenAI CLIP if fashion-clip specific weights fail to load in this structure
    # But usually patrickjohncyh/fashion-clip is compatible
    print(f"Warning: Could not load specific fashion-clip model ({e}). Falling back to openai/clip-vit-base-patch32")
    model = CLIPModel.from_pretrained("openai/clip-vit-base-patch32")
    processor = CLIPProcessor.from_pretrained("openai/clip-vit-base-patch32")

def extract_outfit_features(image_path: str):
    image = Image.open(image_path).convert("RGB")
    
    inputs = processor(images=image, return_tensors="pt")
    
    with torch.no_grad():
        image_features = model.get_image_features(**inputs)
    
    # Normalize to match original behavior if needed, but raw features are fine for similarity
    image_features = image_features / image_features.norm(p=2, dim=-1, keepdim=True)
    
    return image_features[0].tolist()

def classify_outfit_style(image_path: str):
    image = Image.open(image_path).convert("RGB")
    
    # Define candidate styles
    styles = ["casual outfit", "formal outfit", "sporty outfit", "business casual outfit", "streetwear outfit"]
    
    inputs = processor(text=styles, images=image, return_tensors="pt", padding=True)
    
    with torch.no_grad():
        outputs = model(**inputs)
        logits_per_image = outputs.logits_per_image  # this is the image-text similarity score
        probs = logits_per_image.softmax(dim=1)  # we can take the softmax to get the label probabilities

    # Get the index of the highest probability
    # probs is shape (1, num_styles)
    best_style_idx = probs.argmax().item()
    best_style = styles[best_style_idx]
    
    # Return simplified style for recommendation logic
    if "formal" in best_style or "business" in best_style:
        return "formal"
    elif "sporty" in best_style:
        return "sporty"
    else:
        return "casual"  # Default to casual

if __name__ == "__main__":
    print("Extracting features from sample image...")
    # Create valid dummy image if sample doesn't exist just for import test, but user provided one
    try:
        emb = extract_outfit_features("sample_outfit.jpg")
        print("Embedding vector length:", len(emb))

        print("Classifying style...")
        style = classify_outfit_style("sample_outfit.jpg")
        print("Detected Style:", style)
    except Exception as e:
        print(f"Test failed (likely due to missing image): {e}")
