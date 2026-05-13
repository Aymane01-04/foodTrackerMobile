from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime

from app.schemas.common import PyObjectId


class DataCreate(BaseModel):
    title: str
    description: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    metadata: Optional[dict] = Field(default_factory=dict)


class DataUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    metadata: Optional[dict] = None


class DataResponse(BaseModel):
    id: PyObjectId = Field(alias="_id")
    user_id: str
    title: str
    description: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    metadata: dict
    created_at: datetime

    model_config = {
        "populate_by_name": True,
        "arbitrary_types_allowed": True,
        "json_encoders": {PyObjectId: str}
    }
