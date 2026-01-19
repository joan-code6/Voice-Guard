import whisper
import torch

class WhisperService:
    def __init__(self, model_name="base"):
        device = "cuda" if torch.cuda.is_available() else "cpu"
        print(f"Loading Whisper model '{model_name}' on {device}...")
        self.model = whisper.load_model(model_name, device=device)
        print("Whisper model loaded successfully.")
    
    def transcribe(self, audio_path: str) -> dict:
        """
        Transcribe audio file to text.
        Returns: {
            "text": "full transcript",
            "segments": [...],
            "language": "en"
        }
        """
        result = self.model.transcribe(audio_path)
        return result
