from datetime import datetime
from pydantic import BaseModel, EmailStr, Field
from typing import Optional

from app.schemas.common import PyObjectId


class UserCreate(BaseModel):
    firebase_uid: str
    email: EmailStr
    display_name: Optional[str] = None
    photo_url: Optional[str] = None


class UserResponse(BaseModel):
    id: PyObjectId = Field(alias="_id")
    firebase_uid: str
    email: EmailStr
    display_name: Optional[str] = None
    photo_url: Optional[str] = None
    created_at: datetime

    model_config = {
        "populate_by_name": True,
        "arbitrary_types_allowed": True,
        "json_encoders": {PyObjectId: str}
    }

class UserProfileRequest(BaseModel):
    uid: str
    poids: float
    taille: float
    genre: str
    objectif: str

class UserProfileResponse(BaseModel):
    uid: str
    poids: float
    taille: float
    genre: str
    objectif: str
    calories: Optional[int] = None
    proteins: Optional[int] = None
    carbs: Optional[int] = None
    fat: Optional[int] = None

class Token(BaseModel):
    access_token: str
    token_type: str


class TokenData(BaseModel):
    sub: Optional[str] = None
