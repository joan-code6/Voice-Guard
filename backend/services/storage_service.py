import os
from datetime import datetime
from models.database import get_connection

class StorageService:
    def __init__(self, storage_dir="audio_files"):
        self.storage_dir = storage_dir
        os.makedirs(storage_dir, exist_ok=True)
    
    def get_file_path(self, player_uuid: str, timestamp: str) -> str:
        date = datetime.now().strftime("%Y-%m-%d")
        date_dir = os.path.join(self.storage_dir, date)
        os.makedirs(date_dir, exist_ok=True)
        filename = f"{player_uuid}_{timestamp.replace(':', '-')}.wav"
        return os.path.join(date_dir, filename)
    
    def save_detection(self, detection_data: dict):
        query = """
        INSERT INTO detections (player_uuid, player_name, detected_word, full_transcript, timestamp, audio_file_path, server_id)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
        """
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute(query, (
            detection_data["player_uuid"],
            detection_data["player_name"],
            detection_data["detected_word"],
            detection_data["full_transcript"],
            detection_data["timestamp"],
            detection_data["audio_file_path"],
            detection_data["server_id"]
        ))
        conn.commit()
        cursor.close()
        conn.close()
