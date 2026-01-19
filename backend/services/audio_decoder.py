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
