package com.whatdrink.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatdrink.app.data.model.HomeUiState
import com.whatdrink.app.data.repository.DrinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: DrinkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadTopDrinks()
    }

    private fun loadTopDrinks() {
        viewModelScope.launch {
            Log.d("HomeViewModel", "Starting collection")
            repository.getTrendingDrinks()
                .catch { e ->
                    Log.e("HomeViewModel", "Flow error: ${e.message}", e)
                    _uiState.value = HomeUiState.Error(e.message ?: "Failed to load drinks")
                }
                .collect { drinks ->
                    Log.d("HomeViewModel", "Collected ${drinks.size} drinks, updating state")
                    val imageUrls = drinks.associate { it.id to it.imageUrl.takeIf { url -> url.isNotBlank() } }
                    _uiState.value = HomeUiState.Success(drinks, imageUrls)
                }
        }
    }
}
