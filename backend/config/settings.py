import os
from dotenv import load_dotenv

load_dotenv()

BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8000")
MODEL_NAME = os.getenv("MODEL_NAME", "base")
DB_PATH = os.getenv("DB_PATH", "voiceguard.db")
