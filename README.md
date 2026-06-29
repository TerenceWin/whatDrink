# What Drink?

A Japanese soft drink discovery app for tourists and locals.

## The Problem

Japan has one of the most exciting soft drink cultures in the world — seasonal exclusives, regional flavours, vending machine surprises. But for tourists and even locals, it comes with real frustrations:

- **Language barrier** — tourists can't read the label
- **No taste reference** — you don't know if it's good until you've bought it
- **New Release Trap** — new drinks look exciting but there's no community feedback yet
- **Discovery is hard** — no single place to explore what's out there

## The Solution

**What Drink?** solves this with a community-powered drink discovery app, built specifically for the Japanese soft drink market.

## Features

### Instant Barcode Scanner
Point your camera at any drink's barcode to identify it instantly. Powered by Google ML Kit. Supports EAN-13, UPC-A, and scanning directly from a gallery image.

### Drink Profile Page
See the drink's full profile — name, description, ingredients, and community verdict — before you buy.

### Community Reviews & Ratings
Read and write reviews from real users. Star ratings, written impressions, and honest takes from people who've actually tried the drink.

### Discovery & Trending
See what's popular right now and what's just hit the shelves. Never miss a new release.

### Personal Drink Log
Track every drink you've tried. Build your own collection over time.

### Multilingual Support
Full English and Japanese (日本語) support. Switch instantly between EN and JP from the home screen.

### New Release Alerts
Get notified when new drinks drop so you can be the first to try and review them.

### Nearby Store Finder
Find vending machines and convenience stores near you on an interactive map, powered by OpenStreetMap and Overpass API — completely free, no API key required.

## Target Audience

- **Tourists** visiting Japan who can't read Japanese labels
- **Drink enthusiasts** who want to explore and track what they've tried
- **The taste-curious** who want community guidance before committing to a purchase
- **Health-conscious consumers** who need ingredient/nutrition info fast

## Why Us?

We're the first app doing this on Android. A website ([sara-net](https://sara-net)) covers similar ground but is focused on selling vintage vending machines — very different. No app on the market currently does what we do.

## Monetization

The focus at launch is:
- **Free with no barriers** — grow the user base and build the review database
- **Validate the concept** — do people actually scan drinks?

After launch:
- Premium tier at **¥480/month** — removes ads and unlocks additional features

## Tech Stack

- **Android** (Kotlin, Jetpack Compose)
- **Google ML Kit** — barcode scanning
- **CameraX** — camera preview and image analysis
- **Firebase** — authentication, Firestore database, storage
- **OpenStreetMap / Overpass API** — nearby store map
- **Ktor** — HTTP client
- **Room** — local drink log
- **Hilt** — dependency injection

## Team

| Name | Role |
|---|---|
| Terence | UI/UX Designer, QA & Test Engineer |
| Michael | Mobile App Developer |
| Yuanshen Gao | Backend Engineer |

## Project Timeline

| Week | Focus |
|---|---|
| 1 | Planning & Design (Figma) |
| 2 | Core build — barcode scanner, drink profile, Google Sign-in, DB setup |
| 3 | Community features — reviews, star ratings |
| 4 | Polish & testing |
| 5 | Launch 🚀 |

## Firestore Database Structure

The app uses three Firestore collections. Each one has a distinct responsibility.

### `drinkDetails`

Stores the full profile of every drink in the database. This collection is queried when a user scans a barcode.

| Field | Type | Description |
|---|---|---|
| `barcode` | String | EAN-13 barcode (13 digits, zero-padded). Used to look up the drink after a scan. |
| `name` | Map | Localized drink name. Keys are language codes: `en`, `ja`. |
| `brand` | String | The manufacturer or brand name (e.g. "Suntory", "Coca-Cola Japan"). |
| `category` | String | Drink type (e.g. "Sports Drink", "Green Tea", "Energy Drink", "Soda", "Coffee"). |
| `imageUrl` | String | URL to the drink's image stored in Firebase Storage. Empty string if no image yet. |
| `source` | String | Where the data came from: `"firestore"` (manual), `"openfoodfacts"`, or `"deepseek"`. |
| `description` | Map | Localized description of the drink. Keys are language codes: `en`, `ja`. |
| `nutrition` | Map | Nutritional info per 100ml: `calories`, `protein`, `fat`, `carbohydrates`, `sugar`, `sodium`, `caffeine`. |

---

### `drinkStats`

Stores live counters for each drink. Separated from `drinkDetails` so high-frequency writes (views, ratings) don't block reads of static drink info.

| Field | Type | Description |
|---|---|---|
| `barcode` | String | EAN-13 barcode. Links this stat record to a drink in `drinkDetails`. |
| `views` | Int64 | Total number of times this drink's profile has been viewed. |
| `averageRating` | Double | Community star rating (1.0–5.0), recalculated whenever a new review is submitted. |
| `commentCount` | Int64 | Total number of reviews submitted for this drink. |

---

### `drinks`

Stores the pre-computed top 10 ranking cards shown on the home screen. This collection is written by an external Python script that runs every hour — the app only reads from it, never writes to it.

| Field | Type | Description |
|---|---|---|
| `barcode` | String | EAN-13 barcode. Can be used to navigate to the full drink profile. |
| `name` | Map | Localized drink name. Keys are language codes: `en`, `ja`. |
| `brand` | String | Brand name, displayed on the card. |
| `category` | String | Drink category, displayed as a tag on the card. |
| `imageUrl` | String | URL to the drink's image, displayed as the card thumbnail. |
| `ranking` | Int | Position in the top 10 (1 = most popular). |
| `averageRating` | Double | Community star rating snapshot at the time of the last ranking recalculation. |

---

## Getting Started

1. Clone the repository
2. Copy `app/google-services.json.example` to `app/google-services.json` and fill in your Firebase project credentials
3. Open in Android Studio and run on a device or emulator (API 26+)


