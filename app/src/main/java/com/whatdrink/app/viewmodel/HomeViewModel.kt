package com.whatdrink.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatdrink.app.data.model.HomeUiState
import com.whatdrink.app.data.repository.DrinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
                    Log.d("HomeViewModel", "Collected ${drinks.size} drinks")
                    // Show drinks immediately so the list is visible right away
                    _uiState.value = HomeUiState.Success(drinks, emptyMap())
                    // Resolve images in background and update
                    launch {
                        val imageUrls = coroutineScope {
                            drinks.map { drink ->
                                async {
                                    Log.d("HomeViewModel", "Fetching image for ${drink.id}, barcode=${drink.barcode}, imageUrl=${drink.imageUrl}")
                                    val url = when {
                                        drink.imageUrl.startsWith("https://") -> drink.imageUrl
                                        drink.imageUrl.startsWith("gs://") -> repository.getImageUrl(drink.imageUrl)
                                        drink.barcode.isNotBlank() -> repository.getImageUrl(drink.barcode)
                                        else -> null
                                    }
                                    Log.d("HomeViewModel", "Resolved image for ${drink.id}: $url")
                                    drink.id to url
                                }
                            }.awaitAll().toMap()
                        }
                        _uiState.value = HomeUiState.Success(drinks, imageUrls)
                    }
                }
        }
    }
}
