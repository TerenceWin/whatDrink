import requests
import json
import firebase_admin
from firebase_admin import credentials, firestore


cred = credentials.Certificate("serviceAccount.json")
firebase_admin.initialize_app(cred)
db = firestore.client()

YAHOO_APP_ID = "dmVyPTIwMjUwNyZpZD1BNEsyblFOQk11Jmhhc2g9WkRJeFlXSTVaRGswTTJZd1ptVmlZdw"

def fetch_from_yahoo(barcode: str) -> dict:
    url = f"https://shopping.yahooapis.jp/ShoppingWebService/V3/itemSearch?appid={YAHOO_APP_ID}&jan_code={barcode}&results=1"
    response = requests.get(url)
    data = response.json()
    hits = data.get("hits", [])
    if not hits:
        return {}
    item = hits[0]
    return {
    "barcode": barcode,
    "name": {
        "en": item.get("name", ""),
        "ja": item.get("name", "")
    },
    "brand": item.get("brand", {}).get("name", ""),
    "category": item.get("genreCategory", {}).get("name", ""),
    "imageUrl": item.get("image", {}).get("medium", ""),
    "source": "yahoo",
    "description": {
        "en": item.get("description", ""),
        "ja": item.get("description", "")
    },
    "nutrition": {
        "calories": 0,
        "protein": 0.0,
        "fat": 0.0,
        "carbohydrates": 0.0,
        "sugar": 0.0,
        "sodium": 0.0,
        "caffeine": 0.0
    }
}

def save_to_firestore(drink: dict):
    db.collection("drinkDetails").document(drink["barcode"]).set(drink)
    print(f"successfully saved: {drink['name']}")

if __name__ == "__main__":
    barcode = input("barcode: ")
    drink = fetch_from_yahoo(barcode)
    if drink:
        print(json.dumps(drink, ensure_ascii=False, indent=2))
        save_to_firestore(drink)
    else:
        print("didn't find the data")