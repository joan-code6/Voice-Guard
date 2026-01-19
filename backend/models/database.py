import os
from mysql.connector import pooling
from datetime import datetime
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

# MySQL Database Configuration
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'user': os.getenv('DB_USER', 'root'),
    'password': os.getenv('DB_PASSWORD', ''),
    'database': os.getenv('DB_NAME', 'test'),
    'port': int(os.getenv('DB_PORT', 3306))
}

# Connection Pool
connection_pool = pooling.MySQLConnectionPool(pool_name="voiceguard_pool",
                                              pool_size=5,
                                              **DB_CONFIG)

def get_connection():
    return connection_pool.get_connection()

def init_db():
    query = """
    CREATE TABLE IF NOT EXISTS detections (
        id INT AUTO_INCREMENT PRIMARY KEY,
        player_uuid VARCHAR(36) NOT NULL,
        player_name VARCHAR(16) NOT NULL,
        detected_word VARCHAR(50) NOT NULL,
        full_transcript TEXT NOT NULL,
        timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
        audio_file_path VARCHAR(255) NOT NULL,
        server_id VARCHAR(50) NOT NULL
    );
    """
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute(query)
    conn.commit()
    cursor.close()
    conn.close()
