package com.whatdrink.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.whatdrink.app.ui.screens.home.LanguageSelector

enum class BottomBarTab { HOME, MAP, PROFILE, NONE }

@Composable
fun BottomBar(
    activeTab: BottomBarTab = BottomBarTab.NONE,
    onOpenMap: () -> Unit = {},
    onGoHome: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        LanguageSelector()

        Spacer(modifier = Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BottomBarButton(
                icon = Icons.Filled.Map,
                label = "Map",
                active = activeTab == BottomBarTab.MAP,
                onClick = onOpenMap
            )
            BottomBarButton(
                icon = Icons.Filled.Home,
                label = "Home",
                active = activeTab == BottomBarTab.HOME,
                onClick = onGoHome
            )
            BottomBarButton(
                icon = Icons.Filled.Person,
                label = "Profile",
                active = activeTab == BottomBarTab.PROFILE,
                onClick = onOpenProfile
            )
        }
    }
}

@Composable
private fun BottomBarButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .background(
                color = if (active) MaterialTheme.colorScheme.primary else Color.White,
                shape = CircleShape
            )
            .size(52.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}
