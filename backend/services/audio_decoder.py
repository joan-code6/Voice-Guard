import ffmpeg
import os

# Diagnostic check: some environments install a different PyPI package named
# `ffmpeg` which conflicts with `ffmpeg-python` (the package we need).
# If the imported module doesn't expose the ffmpeg-python API, raise a
# clear error that includes the module path to help debugging.
if not hasattr(ffmpeg, "input") or not hasattr(ffmpeg, "probe"):
    mod_path = getattr(ffmpeg, "__file__", "(built-in or unknown)")
    available = sorted([name for name in dir(ffmpeg) if not name.startswith("__")])
    raise ImportError(
        "Imported 'ffmpeg' module does not look like ffmpeg-python.\n"
        f"Module path: {mod_path}\n"
        f"Available attributes: {available[:30]}\n"
        "If you have the PyPI package 'ffmpeg' installed, uninstall it\n"
        "and install 'ffmpeg-python' (pip uninstall ffmpeg; pip install ffmpeg-python)."
    )

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
