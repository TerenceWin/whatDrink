package com.whatdrink.app.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.whatdrink.app.R
import com.whatdrink.app.ui.language.LocalAppLanguage
import com.whatdrink.app.ui.language.strings

@Composable
fun HomeScreen(
    onDrinkClick: (String) -> Unit,
    onScanBarcode: () -> Unit = {},
    onOpenMap: () -> Unit = {},
    barcodeResult: String? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    val strings = LocalAppLanguage.current.strings

    LaunchedEffect(barcodeResult) {
        if (barcodeResult != null) searchQuery = barcodeResult
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.home_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        SakuraAnimation(modifier = Modifier.fillMaxSize())

        LanguageSelector(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 20.dp, bottom = 20.dp)
        )

        // Map button — bottom right
        IconButton(
            onClick = onOpenMap,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 20.dp)
                .background(Color.White, CircleShape)
                .size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Map,
                contentDescription = "Find nearby stores",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 35.dp, end = 35.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = strings.searchHint,
                onScanBarcode = onScanBarcode
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    onScanBarcode: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Barcode scanner icon
            IconButton(onClick = onScanBarcode) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = "Barcode scanner",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Text input
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true
                )
            }

            // Search icon
            IconButton(onClick = { /* trigger search */ }) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Image recognition icon
            IconButton(onClick = { /* image recognition */ }) {
                Icon(
                    imageVector = Icons.Filled.ImageSearch,
                    contentDescription = "Image recognition",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
