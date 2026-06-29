package com.whatdrink.app.ui.screens.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

// Default center: Tokyo
private val TOKYO = GeoPoint(35.6895, 139.6917)

@Composable
fun MapScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var placeCount by remember { mutableStateOf<Int?>(null) }

    Configuration.getInstance().userAgentValue = context.packageName

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            controller.setCenter(TOKYO)
        }
    }

    val locationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            locationOverlay.enableMyLocation()
            locationOverlay.enableFollowLocation()
            if (!mapView.overlays.contains(locationOverlay)) {
                mapView.overlays.add(locationOverlay)
            }
        }
    }

    fun loadNearbyPlaces(lat: Double, lon: Double) {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val places = fetchNearbyPlaces(lat, lon)
                placeCount = places.size

                // Remove old place markers (keep location overlay)
                mapView.overlays.removeAll { it is Marker }

                places.forEach { place ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(place.lat, place.lon)
                        title = place.name ?: if (place.type == PlaceType.VENDING_MACHINE)
                            "Vending Machine" else "Convenience Store"
                        snippet = if (place.type == PlaceType.VENDING_MACHINE)
                            "🥤 Vending Machine" else "🏪 Convenience Store"
                        icon = createMarkerIcon(
                            color = if (place.type == PlaceType.VENDING_MACHINE)
                                android.graphics.Color.parseColor("#2196F3")  // blue
                            else
                                android.graphics.Color.parseColor("#4CAF50"), // green
                            context = context
                        )
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    mapView.overlays.add(marker)
                }
                mapView.invalidate()
            } catch (e: Exception) {
                errorMessage = "Could not load places. Check internet connection."
            }
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(50))
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }

            // Legend
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = Color(0xFF2196F3))
                Text("Vending", style = MaterialTheme.typography.labelSmall)
                LegendDot(color = Color(0xFF4CAF50))
                Text("Convenience", style = MaterialTheme.typography.labelSmall)
                placeCount?.let {
                    Text("($it)", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Bottom buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Search near me button
            ExtendedFloatingActionButton(
                onClick = {
                    if (!hasLocationPermission) {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        val center = locationOverlay.myLocation
                            ?: mapView.mapCenter as GeoPoint
                        loadNearbyPlaces(center.latitude, center.longitude)
                    }
                },
                containerColor = Color.White
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Near me")
            }
        }

        // Loading indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Error snackbar
        errorMessage?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = { TextButton(onClick = { errorMessage = null }) { Text("OK") } }
            ) { Text(msg) }
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, RoundedCornerShape(50))
    )
}

private fun createMarkerIcon(color: Int, context: android.content.Context): BitmapDrawable {
    val size = 36
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    val radius = size / 2f - 2f
    canvas.drawCircle(size / 2f, size / 2f, radius, paint)
    canvas.drawCircle(size / 2f, size / 2f, radius, strokePaint)
    return BitmapDrawable(context.resources, bitmap)
}
