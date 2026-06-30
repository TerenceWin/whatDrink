package com.whatdrink.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatdrink.app.data.model.DrinkDetail
import com.whatdrink.app.data.model.DrinkStats
import com.whatdrink.app.data.repository.DrinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DrinkProfileUiState {
    object Loading : DrinkProfileUiState()
    data class Success(val detail: DrinkDetail, val stats: DrinkStats, val imageUrl: String?) : DrinkProfileUiState()
    object NotFound : DrinkProfileUiState()
    data class Error(val message: String) : DrinkProfileUiState()
}

@HiltViewModel
class DrinkProfileViewModel @Inject constructor(
    private val repository: DrinkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DrinkProfileUiState>(DrinkProfileUiState.Loading)
    val uiState: StateFlow<DrinkProfileUiState> = _uiState

    fun load(drinkId: String) {
        viewModelScope.launch {
            _uiState.value = DrinkProfileUiState.Loading
            try {
                // Increment first so getDrinkStats returns the updated view count
                repository.incrementViews(drinkId)

                val detailDeferred = async { repository.getDrinkDetail(drinkId) }
                val statsDeferred  = async { repository.getDrinkStats(drinkId) }
                val imageDeferred  = async { repository.getImageUrl(drinkId) }

                val detail   = detailDeferred.await()
                val stats    = statsDeferred.await()
                val imageUrl = imageDeferred.await()

                _uiState.value = if (detail != null && stats != null)
                    DrinkProfileUiState.Success(detail, stats, imageUrl)
                else
                    DrinkProfileUiState.NotFound
            } catch (e: Exception) {
                _uiState.value = DrinkProfileUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
