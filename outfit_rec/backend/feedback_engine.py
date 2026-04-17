# feedback_engine.py
from recommend import get_color_category_hls

def validate_outfit(outfit_colors, outfit_style, current_shoe_color, current_shoe_style):
    """
    Validates a submitted outfit + shoe against fashion rules.
    outfit_colors: [{'source': 'shirt', 'name': 'Blue', 'hex': '#...', 'rgb': (r,g,b)}, ...]
    outfit_style: 'casual', 'formal', 'sporty'
    current_shoe_color: dict with 'name' and 'hex' (e.g., {'name': 'Brown', 'hex': '#...'})
    current_shoe_style: 'casual', 'formal', 'sporty', 'boots'
    
    Returns:
    {
        "issue_detected": str (or "None" if perfect),
        "suggested_fix": str,
        "reason": str
    }
    """
    if not current_shoe_color or not current_shoe_style:
        return {
            "issue_detected": "Could not detect shoes in the image.",
            "suggested_fix": "Ensure your entire outfit including shoes is visible.",
            "reason": "Shoes are required to analyze the complete outfit."
        }
        
    has_belt = next((c for c in outfit_colors if c.get('source') == 'belt'), None)
    has_watch = next((c for c in outfit_colors if c.get('source') == 'watch'), None)
    
    upper_colors = [c for c in outfit_colors if c.get('source') == 'shirt']
    lower_colors = [c for c in outfit_colors if c.get('source') == 'pant']
    
    primary_color_obj = upper_colors[0] if upper_colors else (lower_colors[0] if lower_colors else None)
    
    # Analyze shirt contrast if available
    shirt_cat = 'neutral'
    if primary_color_obj:
        r, g, b = [x/255.0 for x in primary_color_obj['rgb']]
        import colorsys
        h, l, s = colorsys.rgb_to_hls(r, g, b)
        shirt_cat = get_color_category_hls(h, l, s)
        
    # Simplify shoe_style to match outfit_style logic
    shoe_style_core = current_shoe_style
    if shoe_style_core == 'boots':
        shoe_style_core = 'casual' # treat boots as casual for broad comparison

    shoe_c_name = current_shoe_color.get('name', 'Unknown')
    shoe_c_hex = current_shoe_color.get('hex', '#000000')

    # Hierarchical rule checking

    # 1. Formality Consistency
    if outfit_style != shoe_style_core:
        from recommend import recommend_shoes
        # Predict the best matching shoe for the rest of their outfit to find the optimal color
        ideal_shoes = recommend_shoes(outfit_colors, outfit_style)
        ideal_color = ideal_shoes[0]['color_name'] if ideal_shoes else "neutral"
        
        # Style mismatch
        types = "Oxfords, Derbies, or Loafers"
        if outfit_style == 'sporty': types = "Running Shoes or Trainers"
        elif outfit_style == 'casual': types = "Sneakers or Desert Boots"
        
        return {
            "issue_detected": f"Style Mismatch: You paired a {outfit_style.capitalize()} outfit with {shoe_style_core.capitalize()} shoes.",
            "suggested_fix": f"Swap them out for {ideal_color} {types}.",
            "reason": "Maintaining consistent formality ties the whole look together—let's make sure the shoes match the vibe of the outfit!"
        }
    # 2. The Golden Rule (Leather Matching)
    # Check if a brown/black belt/watch matches the shoes
    leather_names = ['brown', 'maroon', 'black', 'navy']
    for accessory in [has_belt, has_watch]:
        if accessory:
            acc_name = accessory.get('name', '').lower()
            if any(l in acc_name for l in leather_names):
                # We have a leather-like accessory
                # If shoe doesn't share the main color family:
                shoe_lname = shoe_c_name.lower()
                acc_base = next((l for l in leather_names if l in acc_name), None)
                if acc_base and acc_base not in shoe_lname:
                    return {
                        "issue_detected": f"Mismatched leathers (Shoes do not match the {accessory.get('source')}).",
                        "suggested_fix": f"Wear {accessory.get('name')} shoes to match your {accessory.get('source')}.",
                        "reason": "Classic styling requires leather accessories (belts/watches) to match shoe color exactly."
                    }

    # 3. Visual Separation (Monochromatic Dark)
    if shirt_cat in ['black', 'dark'] and lower_colors:
        pant_rgb = lower_colors[0]['rgb']
        pr, pg, pb = [x/255.0 for x in pant_rgb]
        ph, pl, ps = colorsys.rgb_to_hls(pr, pg, pb)
        pant_cat = get_color_category_hls(ph, pl, ps)
        
        if pant_cat in ['black', 'dark']:
            # Outfit is very dark. If shoe is dark too:
            if any(dark_w in shoe_c_name.lower() for dark_w in ['black', 'dark', 'navy']):
                return {
                    "issue_detected": "Lack of visual separation (monochromatic dark).",
                    "suggested_fix": "Swap for light-colored shoes (like White or Light Gray).",
                    "reason": "Adding bright shoes breaks up a dark outfit, preventing a 'black hole' effect."
                }

    # 4. Visual Grounding (Light Top)
    if shirt_cat in ['white', 'light']:
        if any(light_w in shoe_c_name.lower() for light_w in ['white', 'beige', 'light']):
            # Is pant also light? If pant is dark, light shoes on both ends can work, but dark shoes are preferred
            # Let's say: light shirt + light shoes = potential lack of grounding unless intentional summer look
            # But we leave it as a minor warning or skip for now to prioritize severe issues.
            pass

    # 5. Perfect or Acceptable Match
    return {
        "issue_detected": "None",
        "suggested_fix": "No changes needed.",
        "reason": f"Your {shoe_style_core} shoes pair well with this {outfit_style} look, and the colors are harmonious."
    }

if __name__ == "__main__":
    import pprint
    print("Test 1: Formal Outfit + Sporty Shoe")
    fb = validate_outfit(
        outfit_colors=[], 
        outfit_style='formal', 
        current_shoe_color={'name': 'Black'}, 
        current_shoe_style='sporty'
    )
    pprint.pprint(fb)
    print("\nTest 2: Brown Belt + Black Shoe")
    fb2 = validate_outfit(
        outfit_colors=[{'source': 'belt', 'name': 'Brown', 'hex': '#something', 'rgb': (100,50,0)}],
        outfit_style='casual',
        current_shoe_color={'name': 'Black', 'hex': '#000'},
        current_shoe_style='casual'
    )
    pprint.pprint(fb2)

