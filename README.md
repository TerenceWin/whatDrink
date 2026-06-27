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

## Getting Started

1. Clone the repository
2. Copy `app/google-services.json.example` to `app/google-services.json` and fill in your Firebase project credentials
3. Open in Android Studio and run on a device or emulator (API 26+)
