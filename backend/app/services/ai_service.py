import os
import json
import logging
import re
import base64
from io import BytesIO
from dotenv import load_dotenv
from PIL import Image
from groq import AsyncGroq

# Chemin vers le fichier .env
BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ENV_PATH = os.path.join(BASE_DIR, ".env")

# Charger les variables d'environnement
load_dotenv(dotenv_path=ENV_PATH)

# --- Configuration GROQ (Texte + Vision) ---
groq_api_key = os.getenv("GROQ_API_KEY")
if not groq_api_key:
    logging.error("GROQ_API_KEY introuvable dans le .env")
    groq_client = None
else:
    groq_client = AsyncGroq(api_key=groq_api_key)

def clean_json_response(text: str) -> dict:
    """Nettoie la réponse pour extraire le JSON même si l'IA a inclus du markdown."""
    try:
        # Supprimer les blocs de code markdown si présents
        text = re.sub(r'```json\s*', '', text, flags=re.IGNORECASE)
        text = re.sub(r'```\s*', '', text)
        text = text.strip()
        # Extraire uniquement ce qui se trouve entre les premières et dernières accolades
        start = text.find('{')
        end = text.rfind('}')
        if start != -1 and end != -1:
            text = text[start:end+1]
        return json.loads(text)
    except Exception as e:
        logging.error(f"Erreur de parsing JSON : {text}")
        return {"error": f"Réponse AI invalide : {str(e)}"}

def encode_image_to_base64(image_path: str) -> str:
    """Convertit une image en base64."""
    with Image.open(image_path) as img:
        buffer = BytesIO()
        img.save(buffer, format="JPEG")
        return base64.b64encode(buffer.getvalue()).decode("utf-8")

async def analyze_food_image(image_path: str):
    """Analyse l'image avec GROQ Vision."""
    if not groq_client:
        return {"error": "Groq non configuré (clé manquante)"}

    try:
        base64_image = encode_image_to_base64(image_path)
        data_url = f"data:image/jpeg;base64,{base64_image}"

        prompt = """Tu es un expert en nutrition. Analyse cette image de nourriture.
        Estime pour la portion visible : calories, protéines, glucides, lipides.
        Retourne UNIQUEMENT un JSON valide avec ce format exact :
        {"calories": 0, "proteins": 0, "carbs": 0, "fat": 0}
        Sans texte supplémentaire, sans markdown."""

        completion = await groq_client.chat.completions.create(
            model="meta-llama/llama-4-scout-17b-16e-instruct",
            messages=[
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": prompt},
                        {"type": "image_url", "image_url": {"url": data_url}}
                    ]
                }
            ],
            response_format={"type": "json_object"},
            temperature=0.2,
            max_tokens=1024
        )

        return clean_json_response(completion.choices[0].message.content)
    except Exception as e:
        logging.error(f"Erreur Groq Vision: {str(e)}")
        return {"error": f"Erreur d'analyse image : {str(e)}"}

async def analyze_meal_text(meal_name: str, ingredients: str = ""):
    """Analyse le texte avec GROQ uniquement."""
    if not groq_client:
        return {"error": "Groq non configuré (clé manquante)"}

    try:
        # On garde les champs attendus par le backend actuel (plat, description, etc.) pour ne rien casser
        system_prompt = """Tu es un expert en nutrition. Analyse le plat et réponds UNIQUEMENT en JSON valide sans markdown, avec ce format exact :
        {"plat":"nom du plat","description":"description courte","calories":0,"proteines":0,"glucides":0,"lipides":0,"confiance":"haute|moyenne|faible","note":""}
        Les valeurs numériques sont des entiers."""

        user_prompt = f"Plat : {meal_name}\nIngrédients : {ingredients}"

        completion = await groq_client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt}
            ],
            response_format={"type": "json_object"}
        )

        return json.loads(completion.choices[0].message.content)
    except Exception as e:
        logging.error(f"Erreur Groq Texte: {str(e)}")
        return {"error": f"Erreur d'analyse texte : {str(e)}"}
