package com.whatdrink.app.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whatdrink.app.ui.language.AppLanguage
import com.whatdrink.app.ui.language.LocalAppLanguage
import com.whatdrink.app.ui.language.LocalSetLanguage

@Composable
fun LanguageSelector(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val currentLanguage = LocalAppLanguage.current
    val setLanguage = LocalSetLanguage.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(AppLanguage.EN, AppLanguage.JP).forEach { lang ->
                    LangButton(
                        label = lang.name,
                        selected = lang == currentLanguage,
                        onClick = {
                            setLanguage(lang)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Main toggle button — shows current language
        LangButton(
            label = currentLanguage.name,
            selected = true,
            onClick = { expanded = !expanded }
        )
    }
}

@Composable
private fun LangButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .shadow(elevation = 4.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.Black else Color.Gray
        )
    }
}
