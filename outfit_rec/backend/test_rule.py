import sys
import pprint
sys.path.append("c:/Users/Muhammad Umer Quresh/Desktop/outfit_rec/backend")

from feedback_engine import validate_outfit

# Test 1: Style Mismatch
print("Test 1: Formal Outfit + Sporty Shoe")
fb = validate_outfit(
    outfit_colors=[], 
    outfit_style='formal', 
    current_shoe_color={'name': 'Black'}, 
    current_shoe_style='sporty'
)
pprint.pprint(fb)
print()

# Test 2: The Golden Rule (Mismatched Leather)
print("Test 2: Brown Belt + Black Shoe")
fb = validate_outfit(
    outfit_colors=[{'source': 'belt', 'name': 'Brown', 'hex': '#something', 'rgb': (100,50,0)}],
    outfit_style='casual',
    current_shoe_color={'name': 'Black', 'hex': '#000'},
    current_shoe_style='casual'
)
pprint.pprint(fb)
print()

# Test 3: Black Hole
print("Test 3: Black Top + Black Pants + Black Shoes")
fb = validate_outfit(
    outfit_colors=[
        {'source': 'shirt', 'name': 'Black', 'rgb': (10, 10, 10)},
        {'source': 'pant', 'name': 'Navy', 'rgb': (10, 10, 15)}
    ],
    outfit_style='casual',
    current_shoe_color={'name': 'Black'},
    current_shoe_style='casual'
)
pprint.pprint(fb)
print()

# Test 4: Perfect Match
print("Test 4: Casual Outfit + White Sneakers")
fb = validate_outfit(
    outfit_colors=[
        {'source': 'shirt', 'name': 'Blue', 'rgb': (0, 0, 200)},
        {'source': 'pant', 'name': 'Beige', 'rgb': (200, 200, 150)}
    ],
    outfit_style='casual',
    current_shoe_color={'name': 'White'},
    current_shoe_style='casual'
)
pprint.pprint(fb)
print()
