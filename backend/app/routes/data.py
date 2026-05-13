import os
import shutil
import uuid
import logging
from typing import List
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File

from app.schemas.data import DataCreate, DataResponse, DataUpdate
from app.schemas.user import UserResponse, UserProfileRequest, UserProfileResponse
from app.schemas.meal import MealTextAnalysisRequest, MealAnalysisResponse
from app.services.auth_service import get_current_user
from app.services.data_service import (
    create_data, get_data_by_user, get_data_by_id,
    update_data, delete_data, save_user_profile, get_user_profile
)
from app.services.ai_service import analyze_food_image, analyze_meal_text
from app import database

router = APIRouter()
logger = logging.getLogger(__name__)

# --- Profile Routes ---

@router.get("/profile/{uid}")
async def check_profile_exists(uid: str):
    """Vérifie si le profil utilisateur existe."""
    profile = await database.db.profiles.find_one({"uid": uid})
    return {"exists": profile is not None}

@router.post("/profile", response_model=UserProfileResponse)
async def create_or_update_profile(
    profile: UserProfileRequest
):
    """Save or update user profile and return calculated nutrition."""
    return await save_user_profile(profile)

@router.get("/profile-data/{uid}", response_model=UserProfileResponse)
async def read_profile(uid: str):
    """Retrieve user profile by UID."""
    profile = await get_user_profile(uid)
    if not profile:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Profile not found")
    return profile

# --- AI Analysis Routes ---

@router.post("/analyze-meal")
async def analyze_meal(
    file: UploadFile = File(...),
    current_user: UserResponse = Depends(get_current_user)
):
    """Upload an image, analyze it with Gemini AI, and return nutritional info."""
    if not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="File must be an image")

    os.makedirs("temp", exist_ok=True)
    temp_path = os.path.join("temp", f"{uuid.uuid4()}{os.path.splitext(file.filename)[1]}")

    try:
        with open(temp_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
        result = analyze_food_image(temp_path)
        if "error" in result:
            raise HTTPException(status_code=500, detail=result["error"])
        return result
    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)

@router.post("/analyze-text-meal")
async def analyze_text_meal(
    request: MealTextAnalysisRequest
):
    """Analyze meal text and ingredients with Gemini AI and save to MongoDB."""
    try:
        # Appel au service AI avec le nouveau format
        result = analyze_meal_text(request.meal_name, request.ingredients)

        if "error" in result:
            return {"error": result["error"]}

        # Préparation du document pour MongoDB (on garde les noms standards en DB)
        meal_document = {
            "uid": request.uid,
            "meal_name": result.get("plat", request.meal_name),
            "ingredients": request.ingredients,
            "calories": result.get("calories", 0),
            "proteins": result.get("proteines", 0),
            "carbs": result.get("glucides", 0),
            "fat": result.get("lipides", 0),
            "description": result.get("description", ""),
            "confiance": result.get("confiance", "moyenne"),
            "note": result.get("note", ""),
            "created_at": datetime.utcnow().isoformat()
        }

        try:
            await database.db.meals.insert_one(meal_document)
        except Exception as mongo_err:
            logger.error(f"MongoDB Error: {str(mongo_err)}")
            # On continue quand même pour renvoyer le résultat à l'utilisateur

        # Retourne le résultat complet
        return result

    except Exception as global_err:
        logger.error(f"Global Route Error: {str(global_err)}")
        return {"error": f"Internal server error: {str(global_err)}"}

# --- Standard Data Routes ---

@router.post("", response_model=DataResponse, status_code=status.HTTP_201_CREATED)
async def create_user_data(data: DataCreate, current_user: UserResponse = Depends(get_current_user)):
    """Insert a new data record for the authenticated user."""
    return await create_data(data, current_user)

@router.get("/user/{user_id}", response_model=List[DataResponse])
async def read_user_data(user_id: str, current_user: UserResponse = Depends(get_current_user)):
    """Retrieve all data records for a specific user."""
    if current_user.firebase_uid != user_id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not authorized")
    return await get_data_by_user(user_id)

@router.get("/{data_id}", response_model=DataResponse)
async def read_single_data(data_id: str, current_user: UserResponse = Depends(get_current_user)):
    """Retrieve a single data record by its id."""
    data_item = await get_data_by_id(data_id)
    if data_item["user_id"] != current_user.firebase_uid:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not authorized")
    return data_item

@router.put("/{data_id}", response_model=DataResponse)
async def update_user_data(
    data_id: str,
    data: DataUpdate,
    current_user: UserResponse = Depends(get_current_user),
):
    """Update a user's existing data record."""
    return await update_data(data_id, data, current_user)

@router.delete("/{data_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_user_data(data_id: str, current_user: UserResponse = Depends(get_current_user)):
    """Delete a user's data record."""
    await delete_data(data_id, current_user)
