
from fastapi import FastAPI
from routes.analyze import router as analyze_router
import uvicorn

app = FastAPI(title="VoiceGuard Backend")
app.include_router(analyze_router)

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
