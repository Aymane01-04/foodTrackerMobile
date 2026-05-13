from pydantic import BaseModel
from typing import Optional

class MealTextAnalysisRequest(BaseModel):
    uid: str
    meal_name: str
    ingredients: str

class MealAnalysisResponse(BaseModel):
    plat: Optional[str] = None
    meal_name: Optional[str] = None
    description: Optional[str] = None
    calories: Optional[int] = None
    proteines: Optional[int] = None
    proteins: Optional[int] = None
    glucides: Optional[int] = None
    carbs: Optional[int] = None
    lipides: Optional[int] = None
    fat: Optional[int] = None
    confiance: Optional[str] = None
    note: Optional[str] = None
    error: Optional[str] = None
