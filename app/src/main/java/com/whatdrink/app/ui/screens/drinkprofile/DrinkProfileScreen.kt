package com.whatdrink.app.ui.screens.drinkprofile

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiFoodBeverage
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.whatdrink.app.R
import com.whatdrink.app.data.model.Comment
import com.whatdrink.app.data.model.DrinkDetail
import com.whatdrink.app.data.model.DrinkStats
import com.whatdrink.app.ui.components.BottomBar
import com.whatdrink.app.ui.components.BottomBarTab
import com.whatdrink.app.ui.language.AppLanguage
import com.whatdrink.app.ui.language.LocalAppLanguage
import com.whatdrink.app.viewmodel.DrinkProfileUiState
import com.whatdrink.app.viewmodel.DrinkProfileViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DrinkProfileScreen(
    drinkId: String,
    onBack: () -> Unit,
    onOpenMap: () -> Unit = {},
    onGoHome: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    viewModel: DrinkProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(drinkId) { viewModel.load(drinkId) }

    val uiState by viewModel.uiState.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val isPostingComment by viewModel.isPostingComment.collectAsState()
    var showCommentSheet by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.drink_profile_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is DrinkProfileUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is DrinkProfileUiState.Success -> DrinkProfileContent(
                        detail = state.detail,
                        stats = state.stats,
                        imageUrl = state.imageUrl,
                        comments = comments,
                        onCommentClick = {
                            if (viewModel.isLoggedIn) showCommentSheet = true
                            else onNavigateToLogin()
                        }
                    )
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
            BottomBar(
                activeTab = BottomBarTab.NONE,
                onOpenMap = onOpenMap,
                onGoHome = onGoHome,
                onOpenProfile = onOpenProfile
            )
        }

        if (showCommentSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showCommentSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Add Comment",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Write your comment...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.postComment(commentText) { onNavigateToLogin() }
                            commentText = ""
                            showCommentSheet = false
                        },
                        enabled = commentText.isNotBlank() && !isPostingComment,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isPostingComment) CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        else Text("Post")
                    }
                }
            }
        }
    }
}

@Composable
private fun DrinkProfileContent(
    detail: DrinkDetail,
    stats: DrinkStats,
    imageUrl: String?,
    comments: List<Comment>,
    onCommentClick: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val langKey = if (lang == AppLanguage.JP) "ja" else "en"
    val displayName = detail.name[langKey] ?: detail.name.values.firstOrNull() ?: detail.id

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image — 50dp from top edge, same width as home screen search bar (35dp horizontal padding)
        Spacer(modifier = Modifier.height(80.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 35.dp, vertical = 10.dp)
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 5.dp),
                    onError = { Log.e("DrinkProfile", "Image load error: ${it.result.throwable}") },
                    onSuccess = { Log.d("DrinkProfile", "Image loaded OK") }
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

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (lang == AppLanguage.JP) "コメント (${comments.size})" else "Comments (${comments.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        if (comments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            comments.forEach { comment ->
                                CommentCard(comment = comment)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = if (lang == AppLanguage.JP) "評価" else "Rating",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            TextButton(onClick = onCommentClick) {
                                Text(text = if (lang == AppLanguage.JP) "コメント" else "Comment")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
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

@Composable
private fun CommentCard(comment: Comment) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF5F5F5),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.username.ifBlank { "Anonymous" },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                comment.createdAt?.toDate()?.let {
                    Text(
                        text = SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = comment.context,
                style = MaterialTheme.typography.bodyMedium
            )
        }
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
