from fastapi import APIRouter, UploadFile, File, Form
from services.whisper_service import WhisperService
from services.detector_service import DetectorService
from services.audio_decoder import decode_opus_to_wav
from services.storage_service import StorageService
import tempfile
import os

router = APIRouter()

@router.post("/analyze")
def analyze_audio(
    audio: UploadFile = File(...),
    player_uuid: str = Form(...),
    player_name: str = Form(...),
    timestamp: str = Form(...),
    server_id: str = Form(...)
):
    # 1. Save uploaded Opus file temporarily
    with tempfile.NamedTemporaryFile(delete=False, suffix=".opus") as temp_opus:
        temp_opus.write(audio.file.read())
        temp_opus_path = temp_opus.name
    # 2. Decode Opus → WAV
    temp_wav_path = temp_opus_path.replace(".opus", ".wav")
    decode_opus_to_wav(temp_opus_path, temp_wav_path)
    # 3. Transcribe with Whisper
    whisper = WhisperService()
    result = whisper.transcribe(temp_wav_path)
    transcript = result["text"]
    # 4. Check for bad words
    detector = DetectorService()
    detection = detector.check_text(transcript)
    # 5. If detected: Save audio + log to DB
    response = {
        "detected": detection["detected"],
        "word": detection["word"],
        "transcript": transcript,
        "confidence": result.get("segments", [{}])[0].get("avg_logprob", 1.0)
    }
    if detection["detected"]:
        storage = StorageService()
        storage.save_detection({
            "player_uuid": player_uuid,
            "player_name": player_name,
            "detected_word": detection["word"],
            "full_transcript": transcript,
            "timestamp": timestamp,
            "audio_file_path": temp_wav_path,
            "server_id": server_id
        })
    os.remove(temp_opus_path)
    os.remove(temp_wav_path)
    return response
