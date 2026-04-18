import unittest
from recommend import recommend_shoes

class TestAccessoryRecommendations(unittest.TestCase):
    
    def test_belt_match(self):
        # Case: Belt is present
        outfit = [
            {'source': 'shirt', 'name': 'Blue', 'hex': '#0000FF', 'rgb': (0,0,255)},
            {'source': 'belt', 'name': 'Brown', 'hex': '#A52A2A', 'rgb': (165,42,42)}
        ]
        recs = recommend_shoes(outfit, 'casual')
        
        # Check if Brown (Belt color) is recommended
        found_belt_match = False
        for r in recs:
            if r['color_name'] == 'Brown' and "Matches your Brown Belt" in r['reason']:
                found_belt_match = True
                break
        
        self.assertTrue(found_belt_match, "Should recommend shoes matching the belt.")

    def test_tie_formality(self):
        # Case: Tie detected -> Should upgrade to Formal/Business
        outfit = [
            {'source': 'shirt', 'name': 'White', 'hex': '#FFFFFF', 'rgb': (255,255,255)},
            {'source': 'tie', 'name': 'Red', 'hex': '#FF0000', 'rgb': (255,0,0)} # Tie present
        ]
        # Request 'casual' but expect formal shoes due to tie
        recs = recommend_shoes(outfit, 'casual')
        
        # Check if formal shoes (Oxfords, Loafers) are recommended
        formal_types = ["Oxfords", "Derbies", "Loafers", "Chelsea Boots"]
        found_formal = any(r['type'] in formal_types for r in recs)
        
        self.assertTrue(found_formal, "Presence of tie should suggest formal shoes even if input was casual.")

    def test_watch_fallback(self):
        # Case: No belt, but watch present
        outfit = [
             {'source': 'shirt', 'name': 'Green', 'hex': '#008000', 'rgb': (0,128,0)},
             {'source': 'watch', 'name': 'Black', 'hex': '#000000', 'rgb': (0,0,0)}
        ]
        recs = recommend_shoes(outfit, 'casual')
        
        found_watch_match = False
        for r in recs:
            if "Matches your Black Watch" in r['reason']:
                found_watch_match = True
                break
        
        self.assertTrue(found_watch_match, "Should suggest matching the watch if no belt.")

if __name__ == '__main__':
    unittest.main()
