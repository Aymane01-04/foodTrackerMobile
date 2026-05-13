from datetime import datetime
from bson import ObjectId
from fastapi import HTTPException, status

from app import database
from app.schemas.data import DataCreate, DataUpdate
from app.schemas.user import UserResponse, UserProfileRequest


async def create_data(data: DataCreate, current_user: UserResponse) -> dict:
    document = data.dict()
    document["user_id"] = current_user.firebase_uid
    document["created_at"] = datetime.utcnow().isoformat()

    result = await database.db.data.insert_one(document)
    new_data = await database.db.data.find_one({"_id": result.inserted_id})
    return new_data


async def get_data_by_user(user_id: str) -> list[dict]:
    cursor = database.db.data.find({"user_id": user_id})
    return [item async for item in cursor]

# --- Profile Logic ---

async def save_user_profile(profile_data: UserProfileRequest) -> dict:
    """Create or update a user profile and calculate initial nutrition."""
    # Calcul simple des besoins (similaire au frontend pour cohérence)
    weight = profile_data.poids
    goal = profile_data.objectif

    # Base: 30 kcal par kg pour maintien
    total_calories = int(weight * 30)
    if goal == "Perte de poids":
        total_calories -= 500
    elif goal == "Prise de masse":
        total_calories += 500

    proteins = int(weight * 2)
    fat = int(weight * 1)
    carbs = int((total_calories - (proteins * 4) - (fat * 9)) / 4)

    profile_dict = profile_data.dict()
    profile_dict["calories"] = max(0, total_calories)
    profile_dict["proteins"] = proteins
    profile_dict["carbs"] = max(0, carbs)
    profile_dict["fat"] = fat
    profile_dict["updated_at"] = datetime.utcnow()

    await database.db.profiles.update_one(
        {"uid": profile_data.uid},
        {"$set": profile_dict},
        upsert=True
    )

    return profile_dict

async def get_user_profile(uid: str) -> dict:
    profile = await database.db.profiles.find_one({"uid": uid})
    if not profile:
        return None
    return profile

# --- End Profile Logic ---

async def get_data_by_id(data_id: str) -> dict:
    if not ObjectId.is_valid(data_id):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid data id")

    data = await database.db.data.find_one({"_id": ObjectId(data_id)})
    if not data:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Data not found")
    return data


async def update_data(data_id: str, data_update: DataUpdate, current_user: UserResponse) -> dict:
    existing = await get_data_by_id(data_id)
    if existing["user_id"] != current_user.firebase_uid:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not authorized")

    update_payload = {k: v for k, v in data_update.dict().items() if v is not None}
    if not update_payload:
        return existing

    await database.db.data.update_one({"_id": ObjectId(data_id)}, {"$set": update_payload})
    updated = await database.db.data.find_one({"_id": ObjectId(data_id)})
    return updated


async def delete_data(data_id: str, current_user: UserResponse) -> None:
    existing = await get_data_by_id(data_id)
    if existing["user_id"] != current_user.firebase_uid:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not authorized")

    await database.db.data.delete_one({"_id": ObjectId(data_id)})
