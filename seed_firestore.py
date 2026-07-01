import firebase_admin
from firebase_admin import credentials, firestore

# ── Update this path to your service account key ──
SERVICE_ACCOUNT_PATH = "/Users/terencewin/Desktop/serviceAccountKey.json"

cred = credentials.Certificate(SERVICE_ACCOUNT_PATH)
firebase_admin.initialize_app(cred)
db = firestore.client()

# ── Test drinks for drinkDetails collection (barcode scan results) ──
drink_details = [
    {
        "barcode": "4987035332510",
        "name": {"en": "Pocari Sweat", "ja": "ポカリスエット"},
        "brand": "Otsuka Pharmaceutical",
        "category": "Sports Drink",
        "volume": "900ml",
        "imageUrl": "",
        "source": "firestore",
        "description": {
            "en": "A mild isotonic drink that replenishes water and electrolytes.",
            "ja": "水分と電解質を補給するマイルドなスポーツドリンク。"
        },
        "nutrition": {
            "calories": 25, "protein": 0.1, "fat": 0.0,
            "carbohydrates": 6.2, "sugar": 6.2,
            "sodium": 49.0, "caffeine": 0.0
        }
    },
    {
        "barcode": "4902102113342",
        "name": {"en": "Aquarius", "ja": "アクエリアス"},
        "brand": "Coca-Cola Japan",
        "category": "Sports Drink",
        "imageUrl": "",
        "source": "firestore",
        "description": {
            "en": "A refreshing sports drink with citrus flavor.",
            "ja": "さわやかな柑橘系スポーツドリンク。"
        },
        "nutrition": {
            "calories": 21, "protein": 0.0, "fat": 0.0,
            "carbohydrates": 5.2, "sugar": 5.2,
            "sodium": 34.0, "caffeine": 0.0
        }
    },
    {
        "barcode": "4901085616124",
        "name": {"en": "Oronamin C", "ja": "オロナミンC"},
        "brand": "Otsuka Pharmaceutical",
        "category": "Energy Drink",
        "imageUrl": "",
        "source": "firestore",
        "description": {
            "en": "A vitamin C enriched carbonated energy drink.",
            "ja": "ビタミンCたっぷりの炭酸エネルギードリンク。"
        },
        "nutrition": {
            "calories": 76, "protein": 0.0, "fat": 0.0,
            "carbohydrates": 18.0, "sugar": 18.0,
            "sodium": 21.0, "caffeine": 0.0
        }
    },
    {
        "barcode": "4902102072946",
        "name": {"en": "Georgia Coffee Original", "ja": "ジョージア オリジナル"},
        "brand": "Coca-Cola Japan",
        "category": "Coffee",
        "imageUrl": "",
        "source": "firestore",
        "description": {
            "en": "Japan's most popular canned coffee with a rich, sweet taste.",
            "ja": "日本で最も人気のある缶コーヒー。甘くてリッチな味わい。"
        },
        "nutrition": {
            "calories": 51, "protein": 1.1, "fat": 1.1,
            "carbohydrates": 8.7, "sugar": 8.7,
            "sodium": 55.0, "caffeine": 50.0
        }
    },
    {
        "barcode": "4901777317420",
        "name": {"en": "Kirin Lemon", "ja": "キリンレモン"},
        "brand": "Kirin",
        "category": "Soda",
        "imageUrl": "",
        "source": "firestore",
        "description": {
            "en": "A classic Japanese lemon soda with a crisp refreshing taste.",
            "ja": "すっきりとした味わいのクラシックなレモンソーダ。"
        },
        "nutrition": {
            "calories": 38, "protein": 0.0, "fat": 0.0,
            "carbohydrates": 9.5, "sugar": 9.5,
            "sodium": 10.0, "caffeine": 0.0
        }
    },
    {
        "barcode": "4901085616636",
        "name": {"en": "Suntory Green Tea Iyemon", "ja": "伊右衛門"},
        "brand": "Suntory",
        "category": "Green Tea",
        "imageUrl": "",
        "source": "firestore",
        "description": {
            "en": "A premium green tea brewed with quality Japanese tea leaves.",
            "ja": "品質の高い日本茶葉で淹れたプレミアム緑茶。"
        },
        "nutrition": {
            "calories": 0, "protein": 0.0, "fat": 0.0,
            "carbohydrates": 0.0, "sugar": 0.0,
            "sodium": 5.0, "caffeine": 20.0
        }
    },
    {
        "barcode": "4902102141239",
        "name": {"en": "Ayataka Green Tea", "ja": "綾鷹"},
        "brand": "Coca-Cola Japan",
        "category": "Green Tea",
        "imageUrl": "",
        "source": "firestore",
        "description": {
            "en": "A rich Japanese green tea with a slightly cloudy appearance.",
            "ja": "少し濁りのある、濃厚な日本の緑茶。"
        },
        "nutrition": {
            "calories": 0, "protein": 0.0, "fat": 0.0,
            "carbohydrates": 0.0, "sugar": 0.0,
            "sodium": 5.0, "caffeine": 18.0
        }
    },
    {
        "barcode": "4902102072359",
        "name": {"en": "Real Gold", "ja": "リアルゴールド"},
        "brand": "Coca-Cola Japan",
        "category": "Energy Drink",
        "imageUrl": "",
        "source": "firestore",
        "description": {
            "en": "A long-standing Japanese energy drink with a citrus kick.",
            "ja": "柑橘系のキックが効いた、日本の定番エナジードリンク。"
        },
        "nutrition": {
            "calories": 46, "protein": 0.0, "fat": 0.0,
            "carbohydrates": 11.5, "sugar": 11.5,
            "sodium": 15.0, "caffeine": 30.0
        }
    },
    {
        "barcode": "4901777034693",
        "name": {"en": "C.C. Lemon", "ja": "C.C.レモン"},
        "brand": "Suntory",
        "category": "Soda",
        "imageUrl": "",
        "source": "firestore",
        "description": {
            "en": "A vitamin C packed lemon carbonated drink with 70 lemons worth of vitamin C.",
            "ja": "レモン70個分のビタミンCが入った炭酸飲料。"
        },
        "nutrition": {
            "calories": 34, "protein": 0.0, "fat": 0.0,
            "carbohydrates": 8.5, "sugar": 8.5,
            "sodium": 15.0, "caffeine": 0.0
        }
    },
    {
        "barcode": "4901085615004",
        "name": {"en": "Dakara", "ja": "DAKARA"},
        "brand": "Suntory",
        "category": "Sports Drink",
        "imageUrl": "",
        "source": "firestore",
        "description": {
            "en": "A health-conscious sports drink designed to reset your body.",
            "ja": "体をリセットする健康志向のスポーツドリンク。"
        },
        "nutrition": {
            "calories": 23, "protein": 0.0, "fat": 0.0,
            "carbohydrates": 5.7, "sugar": 5.7,
            "sodium": 40.0, "caffeine": 0.0
        }
    },
]

# ── Test data for drinkStats collection ──
drink_stats = [
    {"barcode": "4987035332510", "views": 0, "averageRating": 0, "commentCount": 0},
    {"barcode": "4902102113342", "views": 0, "averageRating": 0, "commentCount": 0},
    {"barcode": "4901085616124", "views": 0, "averageRating": 0, "commentCount": 0},
    {"barcode": "4902102072946", "views": 0, "averageRating": 0, "commentCount": 0},
    {"barcode": "4901777317420", "views": 0, "averageRating": 0, "commentCount": 0},
    {"barcode": "4901085616636", "views": 0, "averageRating": 0, "commentCount": 0},
    {"barcode": "4902102141239", "views": 0, "averageRating": 0, "commentCount": 0},
    {"barcode": "4902102072359", "views": 0, "averageRating": 0, "commentCount": 0},
    {"barcode": "4901777034693", "views": 0, "averageRating": 0, "commentCount": 0},
    {"barcode": "4901085615004", "views": 0, "averageRating": 0, "commentCount": 0},
]

# ── Top 10 ranking for homepage ──
drinks_ranking = [
    {"barcode": "4902102072946", "name": {"en": "Georgia Coffee Original", "ja": "ジョージア オリジナル"}, "brand": "Coca-Cola Japan", "category": "Coffee",       "imageUrl": "", "ranking": 1,  "averageRating": 4.7},
    {"barcode": "4987035332510", "name": {"en": "Pocari Sweat",            "ja": "ポカリスエット"},       "brand": "Otsuka",          "category": "Sports Drink", "volume": "900ml", "imageUrl": "", "ranking": 2,  "averageRating": 4.5},
    {"barcode": "4901777034693", "name": {"en": "C.C. Lemon",              "ja": "C.C.レモン"},           "brand": "Suntory",         "category": "Soda",         "imageUrl": "", "ranking": 3,  "averageRating": 4.4},
    {"barcode": "4901085616636", "name": {"en": "Suntory Green Tea Iyemon","ja": "伊右衛門"},             "brand": "Suntory",         "category": "Green Tea",    "imageUrl": "", "ranking": 4,  "averageRating": 4.3},
    {"barcode": "4902102113342", "name": {"en": "Aquarius",                "ja": "アクエリアス"},         "brand": "Coca-Cola Japan", "category": "Sports Drink", "imageUrl": "", "ranking": 5,  "averageRating": 4.2},
    {"barcode": "4902102141239", "name": {"en": "Ayataka Green Tea",       "ja": "綾鷹"},                 "brand": "Coca-Cola Japan", "category": "Green Tea",    "imageUrl": "", "ranking": 6,  "averageRating": 4.1},
    {"barcode": "4901085616124", "name": {"en": "Oronamin C",              "ja": "オロナミンC"},          "brand": "Otsuka",          "category": "Energy Drink", "imageUrl": "", "ranking": 7,  "averageRating": 4.0},
    {"barcode": "4901085615004", "name": {"en": "Dakara",                  "ja": "DAKARA"},               "brand": "Suntory",         "category": "Sports Drink", "imageUrl": "", "ranking": 8,  "averageRating": 3.9},
    {"barcode": "4901777317420", "name": {"en": "Kirin Lemon",             "ja": "キリンレモン"},         "brand": "Kirin",           "category": "Soda",         "imageUrl": "", "ranking": 9,  "averageRating": 3.8},
    {"barcode": "4902102072359", "name": {"en": "Real Gold",               "ja": "リアルゴールド"},       "brand": "Coca-Cola Japan", "category": "Energy Drink", "imageUrl": "", "ranking": 10, "averageRating": 3.6},
]

def seed():
    print("Seeding drinkDetails...")
    for drink in drink_details:
        db.collection("drinkDetails").document(drink["barcode"]).set(drink)
        print(f"  ✓ {drink['name']['en']}")

    print("\nSeeding drinkStats...")
    for stat in drink_stats:
        db.collection("drinkStats").document(stat["barcode"]).set(stat)
        print(f"  ✓ {stat['barcode']}")

    print("\nSeeding drinks (homepage ranking)...")
    for drink in drinks_ranking:
        db.collection("drinks").document(drink["barcode"]).set(drink)
        print(f"  ✓ #{drink['ranking']} {drink['name']['en']}")

    print("\n✅ Done! All collections seeded.")

def reset_stats():
    print("Resetting drinkStats to zero...")
    for stat in drink_stats:
        db.collection("drinkStats").document(stat["barcode"]).update({
            "averageRating": 0,
            "commentCount": 0,
            "views": 0,
        })
        print(f"  ✓ {stat['barcode']} reset")
    print("\n✅ Done! All drinkStats reset to zero.")

if __name__ == "__main__":
    seed()
    # Uncomment to reset all stats to zero:
    # reset_stats()
