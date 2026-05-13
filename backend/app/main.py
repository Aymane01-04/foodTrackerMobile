import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv

from app.routes.auth import router as auth_router
from app.routes.data import router as data_router
from app.database import connect_to_mongo, close_mongo_connection

load_dotenv()

app = FastAPI(
    title="FoodTracker Backend",
    version="1.0.0",
    description="FastAPI + MongoDB backend for Android apps",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.on_event("startup")
async def startup_db_client():
    await connect_to_mongo()

@app.on_event("shutdown")
async def shutdown_db_client():
    await close_mongo_connection()

app.include_router(auth_router, prefix="/auth", tags=["auth"])
app.include_router(data_router, prefix="/data", tags=["data"])

@app.get("/")
async def health_check():
    return {"status": "ok", "message": "FoodTracker backend is running"}

if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
