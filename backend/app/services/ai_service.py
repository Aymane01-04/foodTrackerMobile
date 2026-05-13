import os
import json
import logging
import base64
from dotenv import load_dotenv
from groq import Groq

# path absolu vers .env
BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ENV_PATH = os.path.join(BASE_DIR, ".env")

# charger .env
load_dotenv(dotenv_path=ENV_PATH)

# récupérer API KEY Groq (avec fallback sur la clé fournie si le .env est manquant)
groq_api_key = os.getenv("GROQ_API_KEY")

# Initialisation client Groq
client = None
if groq_api_key:
    client = Groq(api_key=groq_api_key)
else:
    logging.error(f"GROQ_API_KEY introuvable. Fichier cherché : {ENV_PATH}")

SYSTEM_PROMPT = """Tu es un expert en nutrition. Analyse le plat et réponds UNIQUEMENT en JSON valide sans markdown ni backticks, avec exactement ce format :
{"plat":"nom du plat","description":"description courte en 1 phrase","calories":0,"proteines":0,"glucides":0,"lipides":0,"confiance":"haute|moyenne|faible","note":"remarque courte si nécessaire"}
Les valeurs numériques sont des entiers pour la portion donnée. Pas de texte hors du JSON."""

def analyze_food_image(image_path: str):
    if not client:
        return {"error": "Groq client not initialized. Please check your API key."}

    try:
        # Encodage de l'image en base64 pour Groq Vision
        with open(image_path, "rb") as image_file:
            base64_image = base64.b64encode(image_file.read()).decode('utf-8')

        response = client.chat.completions.create(
            model="llama-3.2-11b-vision-preview",
            messages=[
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": SYSTEM_PROMPT + "\nAnalyse cette photo de plat."},
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": f"data:image/jpeg;base64,{base64_image}",
                            },
                        },
                    ],
                }
            ],
            temperature=0.1,
            max_tokens=1024,
            response_format={"type": "json_object"}
        )

        return json.loads(response.choices[0].message.content)
    except Exception as e:
        logging.error(f"Erreur Groq Vision: {str(e)}")
        return {"error": str(e)}

def analyze_meal_text(meal_name: str, ingredients: str):
    if not client:
        return {"error": "Groq client not initialized. Please check your API key."}

    try:
        prompt = f"{SYSTEM_PROMPT}\n\nPlat : {meal_name}\nIngrédients : {ingredients}"
        response = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {"role": "user", "content": prompt}
            ],
            temperature=0.1,
            max_tokens=1024,
            response_format={"type": "json_object"}
        )

        return json.loads(response.choices[0].message.content)
    except Exception as e:
        logging.error(f"Erreur Groq Texte: {str(e)}")
        return {"error": str(e)}
