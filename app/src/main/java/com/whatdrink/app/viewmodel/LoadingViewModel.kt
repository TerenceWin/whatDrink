package com.whatdrink.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatdrink.app.data.model.DrinkUiState
import com.whatdrink.app.data.repository.DrinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoadingViewModel @Inject constructor(
    private val repository: DrinkRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<DrinkUiState>(DrinkUiState.Loading)
    val uiState: StateFlow<DrinkUiState> = _uiState

    fun searchByBarcode(barcode: String){
        viewModelScope.launch {
            _uiState.value = DrinkUiState.Loading
            try{
                val drink = repository.getDrinkByBarcode(barcode)
                if (drink != null) {
                    _uiState.value = DrinkUiState.Success(drink)
                } else {
                    _uiState.value = DrinkUiState.NotFound
                }
            } catch (e: Exception) {
                _uiState.value = DrinkUiState.Error(e.message ?:
                "Failed to fetch drink. Please check your connection and try again.")
            }
        }
    }
}