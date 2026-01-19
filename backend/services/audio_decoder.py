import ffmpeg
import os

def decode_opus_to_wav(opus_path: str, wav_path: str):
    """
    Decode Opus audio to WAV format using FFmpeg.
    """
    try:
        (
            ffmpeg
            .input(opus_path)
            .output(wav_path, acodec='pcm_s16le', ar='16000', ac='1')
            .overwrite_output()
            .run(quiet=True)
        )
        return wav_path
    except ffmpeg.Error as e:
        print(f"FFmpeg error: {e.stderr.decode()}")
        raise

def get_audio_duration(file_path: str) -> float:
    """
    Get the duration of an audio file using FFmpeg.
    """
    probe = ffmpeg.probe(file_path)
    return float(probe['streams'][0]['duration'])

def extract_clip(input_path: str, start: float, end: float, output_path: str):
    """
    Extract a clip from an audio file using FFmpeg.
    """
    try:
        (
            ffmpeg
            .input(input_path, ss=start, t=end-start)
            .output(output_path, acodec='libopus')
            .overwrite_output()
            .run(quiet=True)
        )
    except ffmpeg.Error as e:
        print(f"FFmpeg error: {e.stderr.decode()}")
        raise
