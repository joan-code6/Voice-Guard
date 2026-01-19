# VoiceGuard Deployment Guide

This guide explains how to set up and run the VoiceGuard Minecraft plugin and its Python backend for voice moderation.

---

## 1. Prerequisites

- **Java 21** (for the plugin)
- **Maven** (for building the plugin)
- **Python 3.10+** (for the backend)
- **FFmpeg** (for audio decoding)
- **pip** (Python package manager)

---

## 2. Plugin Setup (Java)

1. **Build the Plugin**
   - Open a terminal in the `plugin` directory.
   - Run:
     ```sh
     mvn clean package
     ```
   - The plugin JAR will be in `plugin/target/VoiceGuard-1.0.jar`.

2. **Install the Plugin**
   - Copy the JAR to your Minecraft server's `plugins/` folder.
   - Ensure `config.yml` and `privacy.yml` are present in the plugin's resource folder (they will be auto-generated if missing).

3. **Configure**
   - Edit `config.yml` to set the backend URL and other options as needed.

---

## 3. Backend Setup (Python)

1. **Install FFmpeg**
   - Windows: `choco install ffmpeg`
   - macOS: `brew install ffmpeg`
   - Linux: `sudo apt-get install ffmpeg`

2. **Install Python Dependencies**
   - Open a terminal in the `backend` directory.
   - (Recommended) Create a virtual environment:
     ```sh
     python -m venv .venv
     source .venv/bin/activate  # On Windows: .venv\Scripts\activate
     ```
   - Install requirements:
     ```sh
     pip install -r requirements.txt
     ```

3. **Configure Environment**
   - Edit `.env` to set backend URL, model, and DB path if needed.

4. **Initialize Database**
   - (Optional) Run a script to initialize the database, or let the backend auto-create tables on first run.

5. **Run the Backend**
   - Start the FastAPI server:
     ```sh
     uvicorn main:app --host 0.0.0.0 --port 8000
     ```
   - The backend will listen on the port specified in `.env` (default: 8000).

---

## 4. Running Everything

- **Start the backend first** so the plugin can connect.
- **Start your Minecraft server** with the plugin installed.
- Players will be prompted for privacy consent on join.
- Voice chat will be monitored and analyzed as described in the project plan.

---

## 5. Troubleshooting

- **Backend not reachable:**
  - Check backend URL in `config.yml` and `.env`.
  - Ensure backend is running and accessible from the server.
- **FFmpeg errors:**
  - Make sure FFmpeg is installed and in your PATH.
- **Python errors:**
  - Check that all dependencies are installed and the correct Python version is used.
- **Plugin errors:**
  - Check Minecraft server logs for stack traces.

---

## 6. Useful Commands

- **Reload plugin config:** `/voiceguard reload` (if implemented)
- **Opt-out:** `/privacy opt-out`
- **Accept/Deny consent:** `/accept` or `/deny`

---

## 7. Updating

- To update the plugin, rebuild with Maven and replace the JAR in your server's plugins folder.
- To update the backend, pull the latest code and re-install requirements if needed.

---

## 8. Notes

- For production, consider using a process manager (e.g., systemd, pm2, or supervisor) to keep the backend running.
- For best performance, use a GPU for Whisper if available.
- Data and audio files are stored in the backend's `audio_files/` directory and the SQLite database.
