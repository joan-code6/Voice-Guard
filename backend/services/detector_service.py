import re

class DetectorService:
    def __init__(self, bad_words_file="config/bad_words.txt"):
        with open(bad_words_file, 'r') as f:
            self.bad_words = [word.strip().lower() for word in f.readlines()]
    
    def check_text(self, text: str) -> dict:
        """
        Check if text contains bad words.
        Returns: {
            "detected": bool,
            "word": str or None,
            "matches": list
        }
        """
        text_lower = text.lower()
        matches = []
        for word in self.bad_words:
            pattern = r'\b' + re.escape(word) + r'\b'
            if re.search(pattern, text_lower):
                matches.append(word)
        return {
            "detected": len(matches) > 0,
            "word": matches[0] if matches else None,
            "matches": matches
        }
