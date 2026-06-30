package com.whatdrink.app.ui.screens.drinkprofile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiFoodBeverage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.whatdrink.app.R
import com.whatdrink.app.data.model.DrinkDetail
import com.whatdrink.app.data.model.DrinkStats
import com.whatdrink.app.ui.language.AppLanguage
import com.whatdrink.app.ui.language.LocalAppLanguage
import com.whatdrink.app.ui.screens.home.LanguageSelector
import com.whatdrink.app.viewmodel.DrinkProfileUiState
import com.whatdrink.app.viewmodel.DrinkProfileViewModel

@Composable
fun DrinkProfileScreen(
    drinkId: String,
    onBack: () -> Unit,
    onGoHome: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    viewModel: DrinkProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(drinkId) { viewModel.load(drinkId) }

    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.drink_profile_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            when (val state = uiState) {
                is DrinkProfileUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is DrinkProfileUiState.Success -> DrinkProfileContent(state.detail, state.stats, state.imageUrl)
                is DrinkProfileUiState.NotFound -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Drink not found")
                    }
                }
                is DrinkProfileUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}")
                    }
                }
            }
        }

        LanguageSelector(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 20.dp, bottom = 20.dp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onGoHome,
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Go to home",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = onOpenProfile,
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "User profile",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun DrinkProfileContent(detail: DrinkDetail, stats: DrinkStats, imageUrl: String?) {
    val lang = LocalAppLanguage.current
    val langKey = if (lang == AppLanguage.JP) "ja" else "en"
    val displayName = detail.name[langKey] ?: detail.name.values.firstOrNull() ?: detail.id

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image — 50dp from top edge, same width as home screen search bar (35dp horizontal padding)
        Spacer(modifier = Modifier.height(80.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 35.dp)
                .height(250.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.EmojiFoodBeverage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Name + stats
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 35.dp)
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "%.1f".format(stats.averageRating),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (stats.ranking > 0) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "#${stats.ranking}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (lang == AppLanguage.JP) "${stats.views} 閲覧" else "${stats.views} views",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (lang == AppLanguage.JP) "${stats.commentCount} レビュー" else "${stats.commentCount} reviews",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Nutrition section
            if (detail.nutrition.isNotEmpty()) {
                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = if (lang == AppLanguage.JP) "栄養成分" else "Nutrition",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()

                val nutritionOrder = listOf("calories", "protein", "fat", "carbohydrates", "sugar", "sodium", "caffeine")
                val sortedNutrition = nutritionOrder.mapNotNull { key ->
                    detail.nutrition[key]?.let { key to it }
                } + detail.nutrition.filter { it.key !in nutritionOrder }.entries.map { it.key to it.value }

                sortedNutrition.forEach { (key, value) ->
                    val isSugar = key.lowercase() == "sugar"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (isSugar) 16.dp else 0.dp,
                                top = 10.dp,
                                bottom = 10.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = nutritionLabel(key, lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSugar) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatNutritionValue(key, value),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = if (lang == AppLanguage.JP) "コメント" else "Comments",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun nutritionLabel(key: String, lang: AppLanguage): String {
    val jp = lang == AppLanguage.JP
    return when (key.lowercase()) {
        "calories"      -> if (jp) "カロリー"   else "Calories"
        "protein"       -> if (jp) "タンパク質" else "Protein"
        "fat"           -> if (jp) "脂質"       else "Fat"
        "carbohydrates" -> if (jp) "炭水化物"   else "Carbohydrates"
        "sugar"         -> if (jp) "糖質"       else "Sugars"
        "sodium"        -> if (jp) "ナトリウム" else "Sodium"
        "caffeine"      -> if (jp) "カフェイン" else "Caffeine"
        else            -> key.replaceFirstChar { it.uppercase() }
    }
}

private fun formatNutritionValue(key: String, value: Any): String {
    val number = when (value) {
        is Long   -> value.toDouble()
        is Int    -> value.toDouble()
        is Double -> value
        is Float  -> value.toDouble()
        else      -> return value.toString()
    }
    return when (key.lowercase()) {
        "calories"          -> "${number.toInt()} kcal"
        "sodium", "caffeine" -> "${number.toInt()} mg"
        else                -> "%.1f g".format(number)
    }
}
