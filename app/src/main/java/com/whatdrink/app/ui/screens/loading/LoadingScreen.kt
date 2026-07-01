package com.whatdrink.app.ui.screens.loading

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.whatdrink.app.R
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

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.drink_profile_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        when (uiState) {
            is DrinkUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            is DrinkUiState.Success -> {
                val drink = (uiState as DrinkUiState.Success).drink
                LaunchedEffect(drink.id) {
                    onDrinkFound(drink.id)
                }
            }
            is DrinkUiState.NotFound -> {
                NotFoundCard(barcode = barcode, onGoBack = onNotFound)
            }
            is DrinkUiState.Error -> {
                NotFoundCard(
                    barcode = barcode,
                    message = (uiState as DrinkUiState.Error).message,
                    onGoBack = onNotFound
                )
            }
        }
    }
}

@Composable
private fun NotFoundCard(
    barcode: String,
    message: String? = null,
    onGoBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 35.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (message != null) "Something went wrong" else "Drink Not Found",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = message
                        ?: "No drink matched barcode\n$barcode",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onGoBack,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Go Back", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
