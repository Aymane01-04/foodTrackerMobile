import os
from datetime import datetime, timedelta
from typing import Optional

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError, jwt
from google.auth.transport import requests
from google.oauth2 import id_token

from app import database
from app.schemas.user import TokenData, UserCreate, UserResponse

JWT_SECRET_KEY = os.getenv("JWT_SECRET_KEY", "change-me")
JWT_ALGORITHM = os.getenv("JWT_ALGORITHM", "HS256")
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", "60"))
FIREBASE_PROJECT_ID = os.getenv("FIREBASE_PROJECT_ID")

oauth2_scheme = HTTPBearer()


def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    payload = data.copy()
    expire = datetime.utcnow() + (expires_delta or timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES))
    payload.update({"exp": expire})
    return jwt.encode(payload, JWT_SECRET_KEY, algorithm=JWT_ALGORITHM)


async def verify_firebase_token(firebase_token: str) -> dict:
    try:
        request = requests.Request()
        decoded_token = id_token.verify_firebase_token(firebase_token, request)
        if FIREBASE_PROJECT_ID and decoded_token.get("aud") != FIREBASE_PROJECT_ID:
            raise ValueError("Firebase token audience does not match project ID")
        return decoded_token
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid Firebase token",
        ) from exc


async def create_user_if_not_exists(token_data: dict) -> dict:
    users_collection = database.db.users
    firebase_uid = token_data["uid"]
    existing_user = await users_collection.find_one({"firebase_uid": firebase_uid})
    if existing_user:
        return existing_user

    new_user = UserCreate(
        firebase_uid=firebase_uid,
        email=token_data.get("email", ""),
        display_name=token_data.get("name"),
        photo_url=token_data.get("picture"),
    ).dict()
    new_user["created_at"] = datetime.utcnow()

    result = await users_collection.insert_one(new_user)
    new_user["_id"] = result.inserted_id
    return new_user


async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(oauth2_scheme),
) -> UserResponse:
    token = credentials.credentials
    try:
        payload = jwt.decode(token, JWT_SECRET_KEY, algorithms=[JWT_ALGORITHM])
        firebase_uid: str = payload.get("sub")
        if not firebase_uid:
            raise JWTError("Missing subject claim")
    except JWTError as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Could not validate credentials",
        ) from exc

    user = await database.db.users.find_one({"firebase_uid": firebase_uid})
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found",
        )
    return UserResponse(**user)
