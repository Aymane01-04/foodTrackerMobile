import os
from motor.motor_asyncio import AsyncIOMotorClient
from typing import Union
from dotenv import load_dotenv

# Charger .env.example explicitement
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
load_dotenv(dotenv_path=os.path.join(BASE_DIR, ".env.example"))

MONGODB_URI = os.getenv("MONGODB_URI", "mongodb://localhost:27017")
MONGODB_DB = os.getenv("MONGODB_DB", "foodtracker")

client: Union[AsyncIOMotorClient, None] = None
db = None

async def connect_to_mongo():
    global client, db
    client = AsyncIOMotorClient(MONGODB_URI)
    db = client[MONGODB_DB]
    print(f"Connected to MongoDB at {MONGODB_URI}")

async def close_mongo_connection():
    global client
    if client:
        client.close()
        print("MongoDB connection closed")
