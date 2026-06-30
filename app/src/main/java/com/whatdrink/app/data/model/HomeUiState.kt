package com.whatdrink.app.data.model

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val drinks: List<Drink>, val imageUrls: Map<String, String?>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
