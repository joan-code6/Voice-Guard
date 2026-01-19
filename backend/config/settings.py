import os
from dotenv import load_dotenv

load_dotenv()

MODEL_NAME = os.getenv("MODEL_NAME", "base")
DB_PATH = os.getenv("DB_PATH", "voiceguard.db")
