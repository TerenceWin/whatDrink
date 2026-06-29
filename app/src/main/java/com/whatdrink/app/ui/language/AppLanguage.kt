package com.whatdrink.app.ui.language

import androidx.compose.runtime.compositionLocalOf

enum class AppLanguage { EN, JP }

// Read the current language anywhere — like React's useContext(LangContext)
val LocalAppLanguage = compositionLocalOf { AppLanguage.EN }

// Call this to switch language — like React's context setter
val LocalSetLanguage = compositionLocalOf<(AppLanguage) -> Unit> { {} }
