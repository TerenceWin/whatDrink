package com.whatdrink.app.ui.screens.loading
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.whatdrink.app.data.model.DrinkUiState
import com.whatdrink.app.viewmodel.LoadingViewModel

@Composable
fun LoadingScreen(
    barcode: String,
    onDrinkFound: (String) -> Unit,
    onNotFound: () -> Unit,
    viewModel: LoadingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(barcode) {
        viewModel.searchByBarcode(barcode)
    }
    when (uiState) {
        is DrinkUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is DrinkUiState.Success -> {
            val drink = (uiState as DrinkUiState.Success).drink
            LaunchedEffect(drink.id) {
                onDrinkFound(drink.id)
            }
        }
        is DrinkUiState.NotFound -> {
            LaunchedEffect(Unit) {
                onNotFound()
            }
        }
        is DrinkUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text((uiState as DrinkUiState.Error).message)
            }
        }
    }
}