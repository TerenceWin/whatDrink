package com.whatdrink.app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.whatdrink.app.ui.language.AppLanguage
import com.whatdrink.app.ui.language.LocalAppLanguage
import com.whatdrink.app.ui.language.LocalSetLanguage
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
    object BarcodeScanner : Screen("barcode_scanner")
    object Map : Screen("map")
    object Loading : Screen("loading/{barcode}"){
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
            composable(Screen.Home.route) { backStackEntry ->
                // Receive barcode result from scanner via savedStateHandle
                val barcodeResult by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("barcode_result", null)
                    .collectAsState()

                HomeScreen(
                    onDrinkClick = { drinkId ->
                        navController.navigate(Screen.DrinkProfile.createRoute(drinkId))
                    },
                    onScanBarcode = {
                        navController.navigate(Screen.BarcodeScanner.route)
                    },
                    onOpenMap = {
                        navController.navigate(Screen.Map.route)
                    },
                    onOpenProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onSearch = { barcode ->
                        navController.navigate(Screen.Loading.createRoute(barcode))
                    },
                    barcodeResult = barcodeResult
                )
            }
            composable(Screen.BarcodeScanner.route) {
                BarcodeScannerScreen(
                    onBarcodeDetected = { barcode ->
                        // Normalize UPC-A (12 digits) to EAN-13 (13 digits) by padding leading 0
                        val normalized = normalizeBarcode(barcode)
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("barcode_result", normalized)
                        navController.popBackStack()
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
                    onGoHome = { navController.popBackStack(Screen.Home.route, inclusive = false) }
                )
            }
            composable(Screen.DrinkProfile.route) { backStackEntry ->
                val drinkId = backStackEntry.arguments?.getString("drinkId") ?: return@composable
                DrinkProfileScreen(
                    drinkId = drinkId,
                    onBack = { navController.popBackStack() },
                    onGoHome = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                    onOpenProfile = { navController.navigate(Screen.Profile.route) }
                )
            }
            composable(Screen.Loading.route){ backStackEntry ->
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
