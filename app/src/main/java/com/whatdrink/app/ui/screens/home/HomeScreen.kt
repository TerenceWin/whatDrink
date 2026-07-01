package com.whatdrink.app.ui.screens.home

import android.util.Log
import com.whatdrink.app.ui.components.BottomBar
import com.whatdrink.app.ui.components.BottomBarTab
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiFoodBeverage
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.whatdrink.app.R
import com.whatdrink.app.data.model.Drink
import com.whatdrink.app.data.model.HomeUiState
import com.whatdrink.app.ui.language.AppLanguage
import com.whatdrink.app.ui.language.LocalAppLanguage
import com.whatdrink.app.ui.language.strings
import com.whatdrink.app.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onDrinkClick: (String) -> Unit,
    onScanBarcode: () -> Unit = {},
    onOpenMap: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val strings = LocalAppLanguage.current.strings
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.drink_profile_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        SakuraAnimation(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 35.dp, end = 35.dp, bottom = 82.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = strings.searchHint,
                onScanBarcode = onScanBarcode,
                onSearch = onSearch
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (uiState) {
                is HomeUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                is HomeUiState.Error -> {
                    Text(
                        text = (uiState as HomeUiState.Error).message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is HomeUiState.Success -> {
                    val drinks = (uiState as HomeUiState.Success).drinks
                    val imageUrls = (uiState as HomeUiState.Success).imageUrls
                    Text(
                        text = strings.trending,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(drinks, key = { it.id }) { drink ->
                            DrinkCard(
                                drink = drink,
                                imageUrl = imageUrls[drink.id],
                                onClick = { onDrinkClick(drink.id) }
                            )
                        }
                    }
                }
            }
        }

        BottomBar(
            activeTab = BottomBarTab.HOME,
            onOpenMap = onOpenMap,
            onGoHome = {},
            onOpenProfile = onOpenProfile,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun DrinkCard(drink: Drink, imageUrl: String?, onClick: () -> Unit) {
    val lang = LocalAppLanguage.current
    val langKey = if (lang == AppLanguage.JP) "ja" else "en"
    val displayName = drink.name[langKey] ?: drink.name.values.firstOrNull() ?: drink.brand

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(19.dp),
        color = Color.White.copy(alpha = 0.88f),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(95.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onError = { Log.e("DrinkCard", "Image load failed for $imageUrl: ${it.result.throwable}") }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.EmojiFoodBeverage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(15.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 19.sp
                    ),
                    maxLines = 1
                )
                if (drink.brand.isNotBlank()) {
                    Text(
                        text = drink.brand,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "%.1f".format(drink.averageRating),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 15.sp)
                    )
                    if (drink.ranking > 0) {
                        Spacer(modifier = Modifier.width(13.dp))
                        Text(
                            text = "#${drink.ranking}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    onScanBarcode: () -> Unit,
    onSearch: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onScanBarcode,
                modifier = Modifier.size(53.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = "Barcode scanner",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true
                )
            }

            IconButton(
                onClick = { onSearch(query) },
                modifier = Modifier.size(53.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(
                onClick = { /* image recognition */ },
                modifier = Modifier.size(53.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ImageSearch,
                    contentDescription = "Image recognition",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
