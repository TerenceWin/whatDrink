package com.whatdrink.app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.whatdrink.app.ui.language.AppLanguage
import com.whatdrink.app.ui.language.LocalAppLanguage
import com.whatdrink.app.ui.language.LocalSetLanguage
import com.whatdrink.app.ui.screens.auth.LoginScreen
import com.whatdrink.app.ui.screens.auth.RegisterScreen
import com.whatdrink.app.ui.screens.home.HomeScreen
import com.whatdrink.app.ui.screens.log.LogScreen
import com.whatdrink.app.ui.screens.profile.ProfileScreen
import com.whatdrink.app.ui.screens.map.MapScreen
import com.whatdrink.app.ui.screens.scan.BarcodeScannerScreen
import com.whatdrink.app.ui.screens.drinkprofile.DrinkProfileScreen
import com.whatdrink.app.util.normalizeBarcode
import com.whatdrink.app.ui.screens.loading.LoadingScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Log : Screen("log")
    object Profile : Screen("profile")
    object Login : Screen("login")
    object Register : Screen("register")
    object BarcodeScanner : Screen("barcode_scanner")
    object Map : Screen("map")
    object Loading : Screen("loading/{barcode}") {
        fun createRoute(barcode: String) = "loading/$barcode"
    }
    object DrinkProfile : Screen("drink/{drinkId}") {
        fun createRoute(drinkId: String) = "drink/$drinkId"
    }
}

@Composable
fun WhatDrinkNavHost() {
    val navController = rememberNavController()
    var currentLanguage by remember { mutableStateOf(AppLanguage.EN) }

    CompositionLocalProvider(
        LocalAppLanguage provides currentLanguage,
        LocalSetLanguage provides { currentLanguage = it }
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onDrinkClick = { drinkId ->
                        navController.navigate(Screen.DrinkProfile.createRoute(drinkId))
                    },
                    onScanBarcode = {
                        navController.navigate(Screen.BarcodeScanner.route)
                    },
                    onOpenMap = {
                        navController.navigate(Screen.Map.route) { launchSingleTop = true }
                    },
                    onOpenProfile = {
                        if (FirebaseAuth.getInstance().currentUser != null) {
                            navController.navigate(Screen.Profile.route) { launchSingleTop = true }
                        } else {
                            navController.navigate(Screen.Login.route) { launchSingleTop = true }
                        }
                    },
                    onSearch = { barcode ->
                        navController.navigate(Screen.Loading.createRoute(barcode))
                    }
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route) { launchSingleTop = true }
                    },
                    onOpenMap = {
                        navController.navigate(Screen.Map.route) { launchSingleTop = true }
                    },
                    onGoHome = {
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    },
                    onOpenProfile = {}
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                    onOpenMap = {
                        navController.navigate(Screen.Map.route) { launchSingleTop = true }
                    },
                    onGoHome = {
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    },
                    onOpenProfile = {
                        navController.navigate(Screen.Login.route) { launchSingleTop = true }
                    }
                )
            }
            composable(Screen.BarcodeScanner.route) {
                BarcodeScannerScreen(
                    onBarcodeDetected = { barcode ->
                        val normalized = normalizeBarcode(barcode)
                        navController.navigate(Screen.Loading.createRoute(normalized)) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onClose = { navController.popBackStack() }
                )
            }
            composable(Screen.Map.route) {
                MapScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Log.route) {
                LogScreen(onDrinkClick = { drinkId ->
                    navController.navigate(Screen.DrinkProfile.createRoute(drinkId))
                })
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onGoHome = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                    onOpenMap = { navController.navigate(Screen.Map.route) { launchSingleTop = true } },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Profile.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.DrinkProfile.route) { backStackEntry ->
                val drinkId = backStackEntry.arguments?.getString("drinkId") ?: return@composable
                DrinkProfileScreen(
                    drinkId = drinkId,
                    onBack = { navController.popBackStack() },
                    onOpenMap = {
                        navController.navigate(Screen.Map.route) { launchSingleTop = true }
                    },
                    onGoHome = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                    onOpenProfile = {
                        if (FirebaseAuth.getInstance().currentUser != null) {
                            navController.navigate(Screen.Profile.route) { launchSingleTop = true }
                        } else {
                            navController.navigate(Screen.Login.route) { launchSingleTop = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) { launchSingleTop = true }
                    }
                )
            }
            composable(Screen.Loading.route) { backStackEntry ->
                val barcode = backStackEntry.arguments?.getString("barcode") ?: return@composable
                LoadingScreen(
                    barcode = barcode,
                    onDrinkFound = { drinkId ->
                        navController.navigate(Screen.DrinkProfile.createRoute(drinkId))
                    },
                    onNotFound = {
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    }
                )
            }
        }
    }
}
