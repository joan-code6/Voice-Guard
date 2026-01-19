# VoiceGuard Development Plan

## Project Overview

VoiceGuard is a Minecraft Paper plugin for voice chat moderation that monitors Simple Voice Chat audio streams, detects inappropriate language using OpenAI Whisper speech-to-text, and stores flagged recordings with metadata.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  Minecraft Server                       │
│  ┌───────────────────────────────────────────────────┐  │
│  │  VoiceGuard Plugin (Java)                         │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ Simple Voice Chat API Integration           │  │  │
│  │  │  - Listen to audio packets                  │  │  │
│  │  │  - Per-player audio streams                 │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ Privacy Consent System                       │  │  │
│  │  │  - Join event handler                        │  │  │
│  │  │  - Accept/Deny popup (chat/form)            │  │  │
│  │  │  - Persistent storage (player data)         │  │  │
│  │  │  - /privacy opt-out command                 │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ Circular Buffer (30s rolling)               │  │  │
│  │  │  - ArrayDeque<AudioChunk> per player        │  │  │
│  │  │  - Triggered: extend +15s recording         │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ HTTP Client                                  │  │  │
│  │  │  - POST raw Opus audio to Python backend   │  │  │
│  │  │  - Async/concurrent requests                │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                           │
                           │ HTTP POST
                           │ (raw Opus audio + metadata)
                           ▼
┌─────────────────────────────────────────────────────────┐
│              Python Backend Server                      │
│  ┌───────────────────────────────────────────────────┐  │
│  │  FastAPI Application                              │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ /analyze endpoint                            │  │  │
│  │  │  - Receive audio file                        │  │  │
│  │  │  - Decode Opus using FFmpeg                 │  │  │
│  │  │  - Convert to Whisper-compatible format    │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ Whisper STT Service                          │  │  │
│  │  │  - Load model (cached)                       │  │  │
│  │  │  - Transcribe audio                          │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ Bad Word Detector                            │  │  │
│  │  │  - Load config (bad_words.txt)               │  │  │
│  │  │  - Case-insensitive matching                │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ Storage Service                              │  │  │
│  │  │  - Save audio file (if detected)            │  │  │
│  │  │  - Insert DB record                          │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ┌────────────────┐       ┌─────────────────────────┐  │
│  │   Database     │       │   Audio Storage         │  │
│  │   (SQLite)     │       │   /audio_files/         │  │
│  │  - detections  │       │   - 2026-01-19/         │  │
│  │  - players     │       │     - uuid_timestamp.wav│  │
│  └────────────────┘       └─────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Technology Stack

### Java Plugin (Minecraft Paper 1.20.6)
- **Java 21**
- **Paper API 1.20.6**
- **Simple Voice Chat API** - Voice packet capture
- **OkHttp3** - HTTP client for backend communication
- **Gson** - JSON serialization
- **Maven** - Build tool

### Python Backend
- **Python 3.10+**
- **FastAPI** - Web framework
- **Uvicorn** - ASGI server
- **OpenAI Whisper** - Speech-to-text (base/small model recommended)
- **PyTorch** - Whisper dependency
- **FFmpeg-python** - Opus audio decoding
- **Pydub** - Audio processing
- **SQLAlchemy** - ORM
- **SQLite** - Database (or PostgreSQL for production)
- **python-multipart** - File uploads

## Implementation Phases

### Phase 1: Java Plugin Foundation

#### 1.1 Update Dependencies (pom.xml)
Add required dependencies:
```xml
<!-- Simple Voice Chat API -->
<dependency>
    <groupId>de.maxhenkel.voicechat</groupId>
    <artifactId>voicechat-api</artifactId>
    <version>2.5.0</version>
    <scope>provided</scope>
</dependency>

<!-- HTTP Client -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>

<!-- JSON -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

#### 1.2 Privacy Consent System
**Components:**
- `PrivacyManager.java` - Handles consent tracking
- `PrivacyCommand.java` - `/privacy opt-out` command
- `PlayerJoinListener.java` - Show consent popup on join
- `privacy.yml` - Persistent storage of player consent

**Workflow:**
1. Player joins server
2. Check if consent recorded in `privacy.yml`
3. If no consent:
   - Show consent popup (using chat messages or form)
   - Options: Accept or Deny
   - Accept: Record consent, allow play
   - Deny: Kick player with privacy message
4. If consent exists: Allow play
5. `/privacy opt-out` command: Kick player with message "You must accept voice monitoring to play on this server"

**Data Storage:**
```yaml
# privacy.yml
players:
  550e8400-e29b-41d4-a716-446655440000:
    consented: true
    timestamp: "2026-01-19T10:30:00Z"
```

#### 1.3 Simple Voice Chat Integration
**Components:**
- Implement `VoicechatPlugin` interface
- Register with Simple Voice Chat API
- Create `VoiceChatListener.java` for audio packet events
- Hook into `ServerReceiveEvent` or equivalent

**Key Tasks:**
- Detect when player speaks
- Extract raw Opus audio bytes from packets
- Track active voice sessions per player

#### 1.4 Circular Audio Buffer
**Components:**
- `AudioBuffer.java` - Thread-safe circular buffer per player
- `AudioChunk.java` - Data class (timestamp, bytes, player UUID)

**Implementation:**
- Use `ConcurrentLinkedDeque<AudioChunk>` for thread safety
- Maintain 30 seconds of rolling audio history
- Calculate buffer size: `sampleRate * channels * bytesPerSample * 30`
- When bad word detected: Stop circular buffering, extend recording +15s
- Combine 30s buffer + 15s extension → 45s total clip

#### 1.5 HTTP Client for Backend Communication
**Components:**
- `BackendClient.java` - OkHttp wrapper
- Async POST requests to Python backend

**Request Format:**
```json
POST /analyze
Content-Type: multipart/form-data

{
  "audio": <binary Opus file>,
  "player_uuid": "550e8400-e29b-41d4-a716-446655440000",
  "player_name": "PlayerName",
  "timestamp": "2026-01-19T10:30:00Z",
  "server_id": "main-server"
}
```

**Response Format:**
```json
{
  "detected": true/false,
  "word": "badword" or null,
  "transcript": "full transcript text",
  "confidence": 0.95
}
```

#### 1.6 Configuration File
**config.yml:**
```yaml
voiceguard:
  backend:
    url: "http://localhost:8000"
    timeout: 30
    retry_attempts: 3
  
  buffer:
    pre_trigger_seconds: 30
    post_trigger_seconds: 15
  
  privacy:
    consent_message: |
      &e&l[VoiceGuard] &r&7This server monitors voice chat for safety.
      &7Your voice may be recorded and analyzed if inappropriate language is detected.
      &7Type &a/accept&7 to continue or &c/deny&7 to leave.
    kick_message: "&cYou must accept voice monitoring to play on this server."
  
  enabled_worlds:
    - world
    - world_nether
    - world_the_end
  
  debug: false
```

### Phase 2: Python Backend

#### 2.1 Project Setup
**Directory Structure:**
```
voiceguard-backend/
├── main.py              # FastAPI application
├── requirements.txt     # Python dependencies
├── .env                 # Environment config
├── config/
│   ├── bad_words.txt    # Bad words list
│   └── settings.py      # App settings
├── services/
│   ├── whisper_service.py    # STT processing
│   ├── detector_service.py   # Bad word detection
│   └── storage_service.py    # DB & file storage
├── models/
│   └── database.py      # SQLAlchemy models
├── routes/
│   └── analyze.py       # API endpoints
└── audio_files/         # Storage directory
    └── YYYY-MM-DD/
```

#### 2.2 Dependencies (requirements.txt)
```txt
fastapi==0.109.0
uvicorn[standard]==0.27.0
openai-whisper==20231117
torch==2.1.2
torchaudio==2.1.2
pydub==0.25.1
ffmpeg-python==0.2.0
python-multipart==0.0.6
sqlalchemy==2.0.25
aiosqlite==0.19.0
python-dotenv==1.0.0
```

**System Dependencies:**
```bash
# FFmpeg (required for audio decoding)
apt-get install ffmpeg  # Linux
brew install ffmpeg     # macOS
choco install ffmpeg    # Windows
```

#### 2.3 FastAPI Application
**main.py:**
```python
from fastapi import FastAPI, UploadFile, File, Form
from services.whisper_service import WhisperService
from services.detector_service import DetectorService
from services.storage_service import StorageService
import uvicorn

app = FastAPI(title="VoiceGuard Backend")

# Initialize services on startup
@app.on_event("startup")
async def startup():
    app.state.whisper = WhisperService(model_name="base")
    app.state.detector = DetectorService()
    app.state.storage = StorageService()

@app.post("/analyze")
async def analyze_audio(
    audio: UploadFile = File(...),
    player_uuid: str = Form(...),
    player_name: str = Form(...),
    timestamp: str = Form(...),
    server_id: str = Form(...)
):
    # 1. Save uploaded Opus file temporarily
    # 2. Decode Opus → WAV using FFmpeg
    # 3. Transcribe with Whisper
    # 4. Check for bad words
    # 5. If detected: Save audio + log to DB
    # 6. Return result
    pass

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
```

#### 2.4 Whisper Service
**services/whisper_service.py:**
```python
import whisper
import torch

class WhisperService:
    def __init__(self, model_name="base"):
        # Check GPU availability
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
```

**Model Selection Guide:**
- `tiny` - Fastest, lowest accuracy (~1 sec/clip)
- `base` - **Recommended** - Good balance (~2-3 sec/clip)
- `small` - Better accuracy (~5-7 sec/clip)
- `medium` - High accuracy (~10-15 sec/clip)
- `large` - Best accuracy, very slow (~30+ sec/clip)

#### 2.5 Audio Decoding (Opus → WAV)
**services/audio_decoder.py:**
```python
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
```

#### 2.6 Bad Word Detection
**services/detector_service.py:**
```python
import re

class DetectorService:
    def __init__(self, bad_words_file="config/bad_words.txt"):
        with open(bad_words_file, 'r') as f:
            self.bad_words = [word.strip().lower() for word in f.readlines()]
    
    def check_text(self, text: str) -> dict:
        """
        Check if text contains bad words.
        Returns: {
            "detected": bool,
            "word": str or None,
            "matches": list
        }
        """
        text_lower = text.lower()
        matches = []
        
        for word in self.bad_words:
            # Use word boundaries to avoid partial matches
            pattern = r'\b' + re.escape(word) + r'\b'
            if re.search(pattern, text_lower):
                matches.append(word)
        
        return {
            "detected": len(matches) > 0,
            "word": matches[0] if matches else None,
            "matches": matches
        }
```

**config/bad_words.txt:**
```
badword1
badword2
offensive_term
```

#### 2.7 Database Schema
**models/database.py:**
```python
from sqlalchemy import Column, Integer, String, DateTime, Text
from sqlalchemy.ext.declarative import declarative_base
from datetime import datetime

Base = declarative_base()

class Detection(Base):
    __tablename__ = "detections"
    
    id = Column(Integer, primary_key=True, autoincrement=True)
    player_uuid = Column(String(36), nullable=False)
    player_name = Column(String(16), nullable=False)
    detected_word = Column(String(50), nullable=False)
    full_transcript = Column(Text, nullable=False)
    timestamp = Column(DateTime, default=datetime.utcnow)
    audio_file_path = Column(String(255), nullable=False)
    server_id = Column(String(50), nullable=False)
    
    def __repr__(self):
        return f"<Detection(player={self.player_name}, word={self.detected_word})>"
```

**Database Initialization:**
```python
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.orm import sessionmaker

DATABASE_URL = "sqlite+aiosqlite:///voiceguard.db"

engine = create_async_engine(DATABASE_URL, echo=True)
AsyncSessionLocal = sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

async def init_db():
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
```

#### 2.8 Storage Service
**services/storage_service.py:**
```python
import os
from datetime import datetime
from models.database import Detection, AsyncSessionLocal

class StorageService:
    def __init__(self, storage_dir="audio_files"):
        self.storage_dir = storage_dir
        os.makedirs(storage_dir, exist_ok=True)
    
    def get_file_path(self, player_uuid: str, timestamp: str) -> str:
        """Generate organized file path: audio_files/YYYY-MM-DD/uuid_timestamp.wav"""
        date = datetime.now().strftime("%Y-%m-%d")
        date_dir = os.path.join(self.storage_dir, date)
        os.makedirs(date_dir, exist_ok=True)
        
        filename = f"{player_uuid}_{timestamp.replace(':', '-')}.wav"
        return os.path.join(date_dir, filename)
    
    async def save_detection(self, detection_data: dict):
        """Save detection to database"""
        async with AsyncSessionLocal() as session:
            detection = Detection(**detection_data)
            session.add(detection)
            await session.commit()
```

### Phase 3: Integration & Testing

#### 3.1 End-to-End Flow Testing
1. **Test privacy consent**:
   - New player joins → popup appears
   - Test accept path → player stays
   - Test deny path → player kicked
   - Rejoin → no popup (consent remembered)
   - Test `/privacy opt-out` command

2. **Test audio capture**:
   - Player speaks in voice chat
   - Verify audio buffering (30s rolling)
   - Check console logs for packet capture

3. **Test backend communication**:
   - Trigger audio analysis (speak trigger word)
   - Verify HTTP POST to Python backend
   - Check backend receives Opus file correctly

4. **Test Whisper transcription**:
   - Backend decodes Opus → WAV
   - Whisper transcribes audio
   - Verify transcript accuracy

5. **Test bad word detection**:
   - Speak configured bad words
   - Verify detection in transcript
   - Check database record created
   - Verify audio file saved with correct naming

6. **Test false positives/negatives**:
   - Normal conversation → no detection
   - Variations of bad words → should detect

#### 3.2 Performance Testing
- **Multiple players speaking simultaneously**: Ensure buffers don't interfere
- **Backend load**: Test multiple concurrent transcription requests
- **Memory usage**: Monitor Java heap with many players
- **Whisper speed**: Measure transcription time per model size

#### 3.3 Error Handling
- **Backend offline**: Queue requests or log errors gracefully
- **Whisper processing failure**: Log error, don't crash
- **Audio format issues**: Validate before sending
- **Database errors**: Retry logic for transient failures

### Phase 4: Production Readiness

#### 4.1 Logging
**Java Plugin:**
- Log consent actions (accept/deny)
- Log audio buffer events
- Log HTTP request/response status
- Log detection events to console

**Python Backend:**
- Request logging (player, timestamp)
- Transcription results
- Detection events with confidence scores
- Error stack traces

#### 4.2 Configuration
**Java:** Externalize all settings to `config.yml`
**Python:** Use `.env` file for backend URL, model selection, DB path

#### 4.3 Performance Optimization
- **Java**: Use async HTTP client, dedicated thread pool for audio processing
- **Python**: Consider Redis queue for request buffering during high load
- **Whisper**: Load model once at startup, keep in memory
- **GPU**: Enable CUDA if available for 3-5x speed boost

#### 4.4 Data Retention
**Automatic cleanup policy:**
```python
# Delete audio files older than 30 days
def cleanup_old_files():
    cutoff_date = datetime.now() - timedelta(days=30)
    # Delete files and DB records older than cutoff_date
```

#### 4.5 Privacy Features
- **Consent management**: Persistent storage, never ask twice
- **Opt-out enforcement**: Kick immediately with clear message
- **Data access**: Add command for players to request their data
- **Transparency**: Show privacy policy on first join

#### 4.6 Documentation
- **README.md**: Installation, configuration, usage
- **API.md**: Python backend API documentation
- **PRIVACY.md**: Privacy policy and data handling
- **TROUBLESHOOTING.md**: Common issues and solutions

## Commands

### Player Commands
- `/privacy opt-out` - Opt out of voice monitoring (kicks player)

### Admin Commands (Future)
- `/voiceguard reload` - Reload configuration
- `/voiceguard stats` - Show detection statistics
- `/voiceguard export <player>` - Export player's data
- `/voiceguard clear <player>` - Clear player's detection history

## Development Checklist

### Java Plugin
- [ ] Update `pom.xml` with dependencies
- [ ] Implement `PrivacyManager` for consent tracking
- [ ] Create `PlayerJoinListener` for consent popup
- [ ] Implement `/privacy opt-out` command
- [ ] Create `privacy.yml` data storage
- [ ] Implement `VoicechatPlugin` interface
- [ ] Create `VoiceChatListener` for audio packets
- [ ] Build `AudioBuffer` circular buffer system
- [ ] Implement `BackendClient` HTTP client
- [ ] Create `config.yml` configuration
- [ ] Add comprehensive logging
- [ ] Test privacy consent flow
- [ ] Test audio capture and buffering
- [ ] Test backend communication

### Python Backend
- [ ] Setup project structure
- [ ] Create `requirements.txt`
- [ ] Implement FastAPI application (`main.py`)
- [ ] Create `WhisperService` for STT
- [ ] Implement Opus → WAV decoding
- [ ] Create `DetectorService` for bad words
- [ ] Setup `bad_words.txt` configuration
- [ ] Design database schema
- [ ] Implement `StorageService` for DB and files
- [ ] Create `/analyze` endpoint
- [ ] Add error handling and logging
- [ ] Test Whisper transcription locally
- [ ] Test bad word detection
- [ ] Test file storage organization

### Integration
- [ ] End-to-end testing with both components
- [ ] Performance testing with multiple players
- [ ] Error handling and recovery testing
- [ ] Privacy consent system testing
- [ ] Documentation (README, setup guides)

## Technical Notes

### Audio Format Details
- **Simple Voice Chat**: Outputs Opus codec
- **Java**: Send raw Opus bytes to Python (no decoding in Java)
- **Python**: Decode Opus using FFmpeg before Whisper
- **Whisper**: Accepts WAV, MP3, FLAC, etc. (not Opus directly)

### Memory Considerations
- **30s audio buffer per player**: ~480KB per player (16kHz, mono, 16-bit)
- **Whisper model size**: 
  - Tiny: ~75MB
  - Base: ~150MB
  - Small: ~500MB
  - Medium: ~1.5GB
  - Large: ~3GB
- **Recommendation**: Start with `base` model, upgrade if accuracy insufficient

### Privacy Compliance
- **GDPR**: Requires explicit consent (✅ implemented)
- **Data retention**: Auto-delete old recordings (recommended: 30 days)
- **Right to access**: Allow players to request their data
- **Right to deletion**: Allow players to request data deletion
- **Transparency**: Clear privacy policy and disclosure

## Future Enhancements

1. **Web Dashboard**: View detections, statistics, audio playback
2. **Machine Learning**: Train custom model on server-specific language
3. **Multiple Languages**: Support non-English detection
4. **Severity Levels**: Different actions for different word categories
5. **Appeal System**: Players can appeal false positives
6. **Integration**: Discord notifications for staff on detections
7. **Analytics**: Trend analysis, repeat offenders tracking
8. **Whitelist**: Trusted players exempt from monitoring

## Resources

- **Simple Voice Chat API**: https://modrepo.de/minecraft/voicechat/api
- **OpenAI Whisper**: https://github.com/openai/whisper
- **FastAPI Docs**: https://fastapi.tiangolo.com/
- **Paper API Docs**: https://docs.papermc.io/
