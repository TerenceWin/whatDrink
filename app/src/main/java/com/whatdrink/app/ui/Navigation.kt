package com.whatdrink.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.whatdrink.app.R
import com.whatdrink.app.ui.screens.home.HomeScreen
import com.whatdrink.app.ui.screens.log.LogScreen
import com.whatdrink.app.ui.screens.profile.ProfileScreen
import com.whatdrink.app.ui.screens.scan.ScanScreen
import com.whatdrink.app.ui.screens.drinkprofile.DrinkProfileScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Scan : Screen("scan")
    object Log : Screen("log")
    object Profile : Screen("profile")
    object DrinkProfile : Screen("drink/{drinkId}") {
        fun createRoute(drinkId: String) = "drink/$drinkId"
    }
}

@Composable
fun WhatDrinkNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavScreens = listOf(Screen.Home, Screen.Scan, Screen.Log, Screen.Profile)
    val showBottomBar = bottomNavScreens.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        Screen.Home -> Icons.Filled.Home
                                        Screen.Scan -> Icons.Filled.QrCodeScanner
                                        Screen.Log -> Icons.Filled.List
                                        Screen.Profile -> Icons.Filled.Person
                                        else -> Icons.Filled.Home
                                    },
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text(
                                    stringResource(
                                        when (screen) {
                                            Screen.Home -> R.string.nav_home
                                            Screen.Scan -> R.string.nav_scan
                                            Screen.Log -> R.string.nav_log
                                            Screen.Profile -> R.string.nav_profile
                                            else -> R.string.nav_home
                                        }
                                    )
                                )
                            },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(onDrinkClick = { drinkId ->
                    navController.navigate(Screen.DrinkProfile.createRoute(drinkId))
                })
            }
            composable(Screen.Scan.route) {
                ScanScreen(onDrinkFound = { drinkId ->
                    navController.navigate(Screen.DrinkProfile.createRoute(drinkId))
                })
            }
            composable(Screen.Log.route) {
                LogScreen(onDrinkClick = { drinkId ->
                    navController.navigate(Screen.DrinkProfile.createRoute(drinkId))
                })
            }
            composable(Screen.Profile.route) {
                ProfileScreen()
            }
            composable(Screen.DrinkProfile.route) { backStackEntry ->
                val drinkId = backStackEntry.arguments?.getString("drinkId") ?: return@composable
                DrinkProfileScreen(
                    drinkId = drinkId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
