from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from app import database
from datetime import datetime

router = APIRouter()

class SyncUserRequest(BaseModel):
    uid: str
    email: str

@router.post("/sync-user")
async def sync_user(payload: SyncUserRequest):
    """Synchronise l'utilisateur Firebase avec MongoDB."""
    users_collection = database.db.users
    existing_user = await users_collection.find_one({"firebase_uid": payload.uid})

    if not existing_user:
        new_user = {
            "firebase_uid": payload.uid,
            "email": payload.email,
            "created_at": datetime.utcnow()
        }
        await users_collection.insert_one(new_user)
        return {"status": "created", "message": "User synced successfully"}

    return {"status": "exists", "message": "User already synced"}
