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
    outfit_colors: List of dicts with 'rgb' tuple (0-255).
    outfit_style: 'casual', 'formal', 'sporty'
    """
    
    recommendations = []
    
    # 1. Analyze dominant outfit color
    if not outfit_colors:
        return [{"type": "Generic Sneakers", "color": "White", "hex": "#FFFFFF", "reason": "No outfit colors detected."}]

    primary_color = outfit_colors[0]['rgb'] # Tuple (r,g,b)
    r, g, b = [x/255.0 for x in primary_color]
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    
    cat = get_color_category_hls(h, l, s)
    
    # Color Strategy
    suggested_colors = []
    
    # Strategy 1: The "Safe" Option (Neutral)
    suggested_colors.append({
        "name": "White", "hex": "#FFFFFF", "reason": "Universal match for any outfit."
    })
    
    # Strategy 2: Contrast/Complementary
    # If outfit is dark -> Suggest Light
    if cat in ['black', 'dark', 'gray']:
        suggested_colors.append({"name": "White", "hex": "#FFFFFF", "reason": "Contrast significantly with dark outfit."})
        suggested_colors.append({"name": "Light Gray", "hex": "#D3D3D3", "reason": "Subtle contrast."})
    
    # If outfit is light/white -> Suggest Dark/Contrast
    elif cat in ['white', 'light']:
        suggested_colors.append({"name": "Black", "hex": "#000000", "reason": "Grounds the light outfit."})
        suggested_colors.append({"name": "Navy", "hex": "#000080", "reason": "Classic contrast."})
        
    # If outfit is colorful (Warm/Cool) -> Neutral is best, OR Color Block
    else:
        suggested_colors.append({"name": "Black", "hex": "#000000", "reason": "Neutral base for colorful outfit."})
        suggested_colors.append({"name": "Beige/Cream", "hex": "#F5F5DC", "reason": "Soft neutral that doesn't clash."})
        
        # Complementary Logic
        comp_h = (h + 0.5) % 1.0
        # Convert back to RGB for display (simplified)
        c_r, c_g, c_b = colorsys.hls_to_rgb(comp_h, 0.5, 0.5) 
        comp_hex = "#{:02x}{:02x}{:02x}".format(int(c_r*255), int(c_g*255), int(c_b*255))
        suggested_colors.append({"name": "Complementary Pop", "hex": comp_hex, "reason": "Bold complementary color choice."})

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
    for st in shoe_types:
        # Pick 2 suitable colors for this shoe type
        # E.g. Formal shoes shouldn't usually be "Complementary Pop" (Green/Purple) unless very fashion forward
        # Keep it simple for now
        
        for sc in suggested_colors[:2]: # Top 2 color suggestions
            final_recs.append({
                "type": st,
                "color_name": sc['name'],
                "hex": sc['hex'],
                "reason": f"{st} in {sc['name']} - {sc['reason']}" 
            })
            
    return final_recs

if __name__ == "__main__":
    # Test
    # Blue-ish color
    test_rgb = (0, 0, 128) 
    recs = recommend_shoes([{'rgb': test_rgb}], 'casual')
    import pprint
    pprint.pprint(recs)
