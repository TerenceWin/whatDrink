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
        "name": item.get("name", ""),
        "brand": item.get("brand", {}).get("name", ""),
        "price": item.get("price", 0),
        "category": item.get("genreCategory", {}).get("name", ""),
        "imageUrl": item.get("image", {}).get("medium", ""),
        "description": item.get("description", ""),
        "averageRating": 0.0,
        "reviewCount": 0,
        "likeCount": 0,
    }

def save_to_firestore(drink: dict):
    db.collection("drinks").document(drink["barcode"]).set(drink)
    print(f"successfully saved: {drink['name']}")

if __name__ == "__main__":
    barcode = input("barcode: ")
    drink = fetch_from_yahoo(barcode)
    if drink:
        print(json.dumps(drink, ensure_ascii=False, indent=2))
        save_to_firestore(drink)
    else:
        print("didn't find the data")