from fastapi import APIRouter, UploadFile, File, Form
from services.whisper_service import WhisperService
from services.detector_service import DetectorService
from services.audio_decoder import decode_opus_to_wav, get_audio_duration, extract_clip
from services.storage_service import StorageService
import tempfile
import os
import glob

router = APIRouter()

@router.post("/analyze")
def analyze_audio(
    audio: UploadFile = File(...),
    player_uuid: str = Form(...),
    player_name: str = Form(...),
    timestamp: str = Form(...),
    server_id: str = Form(...)
):
    audio_data = audio.file.read()
    storage = StorageService()
    file_path = storage.save_audio(player_uuid, server_id, player_name, timestamp, audio_data)
    return {"saved": True, "path": file_path}

@router.post("/analyze_batch")
def analyze_batch():
    storage = StorageService()
    opus_files = glob.glob(os.path.join(storage.storage_dir, "**", "*.opus"), recursive=True)
    detections = []
    for opus_path in opus_files:
        filename = os.path.basename(opus_path)
        parts = filename.replace('.opus', '').split('_')
        if len(parts) < 4:
            continue
        player_uuid = parts[0]
        server_id = parts[1]
        player_name = '_'.join(parts[2:-1])
        timestamp = parts[-1].replace('-', ':')
        
        temp_wav = opus_path.replace('.opus', '_temp.wav')
        decode_opus_to_wav(opus_path, temp_wav)
        
        whisper = WhisperService()
        result = whisper.transcribe(temp_wav)
        transcript = result["text"]
        
        detector = DetectorService()
        detection = detector.check_text(transcript)
        
        if detection["detected"]:
            segments = result.get("segments", [])
            bad_word = detection["word"]
            segment = None
            for seg in segments:
                if bad_word in seg["text"].lower():
                    segment = seg
                    break
            if segment:
                start = segment["start"]
                end = segment["end"]
                duration = get_audio_duration(opus_path)
                clip_start = max(0, start - 2)
                clip_end = min(duration, end + 2)
                if clip_end - clip_start > 60:
                    clip_end = clip_start + 60
                clip_path = opus_path.replace('.opus', f'_clip_{int(clip_start)}_{int(clip_end)}.opus')
                extract_clip(opus_path, clip_start, clip_end, clip_path)
                
                storage.save_detection({
                    "player_uuid": player_uuid,
                    "player_name": player_name,
                    "detected_word": bad_word,
                    "full_transcript": transcript,
                    "timestamp": timestamp,
                    "audio_file_path": clip_path,
                    "server_id": server_id
                })
                detections.append({
                    "file": opus_path,
                    "detected_word": bad_word,
                    "clip": clip_path
                })
        
        os.remove(temp_wav)
    
    return {"analyzed": len(opus_files), "detections": detections}
