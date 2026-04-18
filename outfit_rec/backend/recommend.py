# recommend.py
import colorsys

def get_color_category_hls(h, l, s):
    """
    Classify a color based on HLS values (0-1 range).
    Returns basic categories for logic: 'neutral', 'warm', 'cool', 'dark', 'light'
    """
    # Neutrals (Black, White, Gray, Beige-ish)
    if s < 0.15: # Very low saturation -> Gray/Black/White
        if l < 0.15: return 'black'
        if l > 0.85: return 'white'
        return 'gray'
    
    if l < 0.15: return 'dark' # Very dark colors (Navy, Dark Green etc) act as neutrals often
    if l > 0.85: return 'light' # Very pale pastels
    
    # Hues
    # h is 0-1. 0=Red, 0.33=Green, 0.66=Blue
    if (h < 0.1 or h > 0.9): return 'warm' # Red/Orange
    if (0.1 <= h < 0.45): return 'natural' # Yellow/Green
    if (0.45 <= h < 0.85): return 'cool' # Blue/Purple/Cyan
    
    return 'warm' # Fallback to warm (Magenta/Pink)

def recommend_shoes(outfit_colors, outfit_style):
    """
    outfit_colors: List of dicts with 'rgb' tuple (0-255) and 'source' (shirt, pant, belt, watch, etc.)
    outfit_style: 'casual', 'formal', 'sporty'
    """
    
    recommendations = []
    
    # 0. Pre-scan for Accessories & Formality Triggers
    has_belt = next((c for c in outfit_colors if c.get('source') == 'belt'), None)
    has_watch = next((c for c in outfit_colors if c.get('source') == 'watch'), None)
    has_tie = next((c for c in outfit_colors if c.get('source') == 'tie'), None)
    
    # If a tie is present, bump Style to Formal (unless already set to something specific by user?)
    # We'll assume presence of tie implies at least Business Casual / Formal
    if has_tie and outfit_style == 'casual':
        outfit_style = 'formal'
        
    # 1. Analyze dominant outfit color
    if not outfit_colors:
        return [{"type": "Generic Sneakers", "color": "White", "hex": "#FFFFFF", "reason": "No outfit colors detected."}]

    # Segregate colors
    upper_colors = [c for c in outfit_colors if c.get('source') == 'shirt']
    lower_colors = [c for c in outfit_colors if c.get('source') == 'pant']
    
    # Default to first color available if specific regions missing or generic
    primary_color_obj = upper_colors[0] if upper_colors else (lower_colors[0] if lower_colors else outfit_colors[0])
    primary_rgb = primary_color_obj['rgb']
    
    secondary_color_obj = lower_colors[0] if lower_colors else None

    # HLS Analysis of Primary Color (Shirt)
    r, g, b = [x/255.0 for x in primary_rgb]
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    cat = get_color_category_hls(h, l, s)
    
    suggested_colors = []
    
    # --- PRIORITY 1: ACCEPTED CRITERIA - MATCH LEATHERS (Belt/Watch) ---
    # According to fashion rules: Shoes should match Belt.
    if has_belt:
        belt_name = has_belt.get('name', 'Belt')
        belt_hex = has_belt.get('hex', '#000000')
        suggested_colors.append({
            "name": belt_name, 
            "hex": belt_hex, 
            "reason": f"Matches your {belt_name} Belt (The Golden Rule)."
        })
        
    # If no belt, maybe match Watch strap?
    elif has_watch:
        watch_name = has_watch.get('name', 'Watch')
        watch_hex = has_watch.get('hex', '#000000')
        # Only if detected color looks 'leathery' or neutral (skip bright plastic watches?)
        # For now, just suggest it.
        suggested_colors.append({
            "name": watch_name, 
            "hex": watch_hex, 
            "reason": f"Matches your {watch_name} Watch strap."
        })

    # --- STRATEGY: Neutral Safe Bet ---
    suggested_colors.append({
        "name": "White", "hex": "#FFFFFF", "reason": "Universal match for any outfit."
    })
    
    # --- STRATEGY: Contrast/Complementary (Based on Shirt) ---
    if cat in ['black', 'dark', 'gray']:
        suggested_colors.append({"name": "White", "hex": "#FFFFFF", "reason": f"Contrast with dark {primary_color_obj.get('name', 'outfit')}."})
        suggested_colors.append({"name": "Light Gray", "hex": "#D3D3D3", "reason": "Subtle contrast."})
    
    elif cat in ['white', 'light']:
        suggested_colors.append({"name": "Black", "hex": "#000000", "reason": f"Grounds the light {primary_color_obj.get('name', 'outfit')}."})
        suggested_colors.append({"name": "Navy", "hex": "#000080", "reason": "Classic contrast."})
        
    else: # Colorful
        suggested_colors.append({"name": "Black", "hex": "#000000", "reason": "Neutral base for colorful top."})
        suggested_colors.append({"name": "Beige/Cream", "hex": "#F5F5DC", "reason": "Soft neutral that doesn't clash."})
        
        # Complementary Logic
        comp_h = (h + 0.5) % 1.0
        c_r, c_g, c_b = colorsys.hls_to_rgb(comp_h, 0.5, 0.5) 
        comp_hex = "#{:02x}{:02x}{:02x}".format(int(c_r*255), int(c_g*255), int(c_b*255))
        suggested_colors.append({"name": "Complementary Pop", "hex": comp_hex, "reason": "Bold complementary color choice."})

    # --- STRATEGY: Match Pants ---
    if secondary_color_obj:
        suggested_colors.append({
            "name": secondary_color_obj.get('name', 'Pant Color'),
            "hex": secondary_color_obj.get('hex', '#000000'),
            "reason": f"Matches Pants ({secondary_color_obj.get('name', 'Secondary')}) for seamless look."
        })

    # Style Strategy
    shoe_types = []
    if outfit_style == 'formal':
        shoe_types = ["Oxfords", "Derbies", "Loafers", "Chelsea Boots"]
    elif outfit_style == 'sporty':
        shoe_types = ["Running Shoes", "Trainers", "Sport Sandals"]
    else: # Casual
        shoe_types = ["Sneakers", "Canvas Shoes", "Slip-ons", "Desert Boots"]

    # Combine
    final_recs = []
    
    # Deduplicate suggested colors based on Name or Hex
    unique_suggestions = {}
    for s in suggested_colors:
        key = s['hex']
        if key not in unique_suggestions:
            unique_suggestions[key] = s
            
    sorted_suggestions = list(unique_suggestions.values())

    # Limit total colors to check
    # If we have a belt match, that should definitely be #1
    
    for st in shoe_types:
        # Pick top 3 suitable colors
        for sc in sorted_suggestions[:3]: 
            final_recs.append({
                "type": st,
                "color_name": sc['name'],
                "hex": sc['hex'],
                "reason": f"{st} in {sc['name']} - {sc['reason']}" 
            })
            
    return final_recs

if __name__ == "__main__":
    # Test
    # Mock data with a belt
    mock_outfit = [
        {'source': 'shirt', 'name': 'Blue', 'hex': '#0000FF', 'rgb': (0,0,255)},
        {'source': 'belt', 'name': 'Brown', 'hex': '#A52A2A', 'rgb': (165,42,42)}
    ]
    recs = recommend_shoes(mock_outfit, 'casual')
    import pprint
    pprint.pprint(recs)
