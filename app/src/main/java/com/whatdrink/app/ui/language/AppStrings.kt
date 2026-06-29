package com.whatdrink.app.ui.language

data class AppStrings(
    // Home
    val searchHint: String,
    val trending: String,
    val newReleases: String,

    // Scan
    val scanTitle: String,
    val scanInstruction: String,
    val scanNotFound: String,
    val scanPermissionRequired: String,

    // Drink Profile
    val drinkReviews: String,
    val drinkWriteReview: String,
    val drinkAddToLog: String,
    val drinkNoReviews: String,

    // Log
    val logTitle: String,
    val logEmpty: String,

    // Auth
    val signInGoogle: String,
    val signOut: String,

    // General
    val loading: String,
    val errorGeneric: String,
    val retry: String,
)

val EnStrings = AppStrings(
    searchHint = "Search drinks...",
    trending = "Top 10 Drinks",
    newReleases = "New Releases",
    scanTitle = "Scan Barcode",
    scanInstruction = "Point your camera at the barcode",
    scanNotFound = "Drink not found. Want to add it?",
    scanPermissionRequired = "Camera permission is required to scan barcodes",
    drinkReviews = "Reviews",
    drinkWriteReview = "Write a Review",
    drinkAddToLog = "Add to My Log",
    drinkNoReviews = "No reviews yet. Be the first!",
    logTitle = "My Drink Log",
    logEmpty = "No drinks logged yet.\nStart scanning!",
    signInGoogle = "Sign in with Google",
    signOut = "Sign Out",
    loading = "Loading…",
    errorGeneric = "Something went wrong. Please try again.",
    retry = "Retry",
)

val JpStrings = AppStrings(
    searchHint = "飲み物を検索…",
    trending = "トップ10ドリンク",
    newReleases = "新発売",
    scanTitle = "バーコードをスキャン",
    scanInstruction = "バーコードにカメラを向けてください",
    scanNotFound = "飲み物が見つかりません。追加しますか？",
    scanPermissionRequired = "バーコードをスキャンするにはカメラの許可が必要です",
    drinkReviews = "レビュー",
    drinkWriteReview = "レビューを書く",
    drinkAddToLog = "ログに追加",
    drinkNoReviews = "まだレビューがありません。最初のレビューを書きましょう！",
    logTitle = "マイ飲み物ログ",
    logEmpty = "まだ飲み物が記録されていません。\nスキャンを始めましょう！",
    signInGoogle = "Googleでサインイン",
    signOut = "サインアウト",
    loading = "読み込み中…",
    errorGeneric = "エラーが発生しました。もう一度お試しください。",
    retry = "再試行",
)

val AppLanguage.strings: AppStrings
    get() = when (this) {
        AppLanguage.EN -> EnStrings
        AppLanguage.JP -> JpStrings
    }
