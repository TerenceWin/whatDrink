package com.whatdrink.app.data.model

sealed class DrinkUiState {
    object Loading : DrinkUiState()
    data class Success(val drink: Drink) : DrinkUiState()
    object NotFound : DrinkUiState()
    data class Error(val message: String) : DrinkUiState()
}