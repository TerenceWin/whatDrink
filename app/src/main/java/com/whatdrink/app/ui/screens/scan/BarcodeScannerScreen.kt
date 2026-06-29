package com.whatdrink.app.ui.screens.scan

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

private const val TAG = "BarcodeScanner"

@Composable
fun BarcodeScannerScreen(
    onBarcodeDetected: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var showManualEntry by remember { mutableStateOf(false) }
    var galleryError by remember { mutableStateOf<String?>(null) }

    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )
    }

    // Gallery image picker — no extra permission needed (system picker handles it)
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val image = InputImage.fromFilePath(context, uri)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val value = barcodes.firstOrNull()?.rawValue
                    if (value != null) {
                        Log.d(TAG, "Gallery scan detected: $value")
                        onBarcodeDetected(value)
                    } else {
                        galleryError = "No barcode found in that image."
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Gallery scan error: ${e.message}")
                    galleryError = "Could not read image."
                }
        } catch (e: Exception) {
            Log.e(TAG, "Image load error: ${e.message}")
            galleryError = "Could not open image."
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                lifecycleOwner = lifecycleOwner,
                onBarcodeDetected = onBarcodeDetected
            )
            ScannerOverlay(
                onClose = onClose,
                onPickFromGallery = { galleryLauncher.launch("image/*") },
                onManualEntry = { showManualEntry = true }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Camera permission is required\nto scan barcodes.",
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Still allow gallery scan even without camera permission
                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Scan from gallery", color = Color.White)
                }
            }
        }

        if (showManualEntry) {
            ManualEntryDialog(
                onConfirm = { code ->
                    showManualEntry = false
                    if (code.isNotBlank()) onBarcodeDetected(code)
                },
                onDismiss = { showManualEntry = false }
            )
        }

        galleryError?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { galleryError = null }) { Text("OK") }
                }
            ) { Text(error) }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(
    modifier: Modifier,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val detected = remember { mutableStateOf(false) }
    val currentOnBarcodeDetected by rememberUpdatedState(onBarcodeDetected)

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .setTargetResolution(Size(640, 480))
                    .build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !detected.value) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                barcodeScanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        barcodes.firstOrNull()?.rawValue?.let { value ->
                                            if (!detected.value) {
                                                detected.value = true
                                                Log.d(TAG, "Camera detected: $value")
                                                currentOnBarcodeDetected(value)
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "ML Kit error: ${e.message}")
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Camera binding failed: ${e.message}")
                }
            }, executor)

            previewView
        },
        modifier = modifier
    )
}

@Composable
private fun ScannerOverlay(
    onClose: () -> Unit,
    onPickFromGallery: () -> Unit,
    onManualEntry: () -> Unit
) {
    val scanSize = 260.dp
    val cornerLength = 28.dp
    val cornerStrokeWidth = 4f

    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black.copy(alpha = 0.62f))
        )
        Row(modifier = Modifier.fillMaxWidth().height(scanSize)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.62f))
            )
            Box(modifier = Modifier.size(scanSize)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cLen = cornerLength.toPx()
                    val w = cornerStrokeWidth
                    val c = Color.White

                    drawLine(c, Offset(0f, cLen), Offset(0f, 0f), w, StrokeCap.Round)
                    drawLine(c, Offset(0f, 0f), Offset(cLen, 0f), w, StrokeCap.Round)
                    drawLine(c, Offset(size.width - cLen, 0f), Offset(size.width, 0f), w, StrokeCap.Round)
                    drawLine(c, Offset(size.width, 0f), Offset(size.width, cLen), w, StrokeCap.Round)
                    drawLine(c, Offset(0f, size.height - cLen), Offset(0f, size.height), w, StrokeCap.Round)
                    drawLine(c, Offset(0f, size.height), Offset(cLen, size.height), w, StrokeCap.Round)
                    drawLine(c, Offset(size.width - cLen, size.height), Offset(size.width, size.height), w, StrokeCap.Round)
                    drawLine(c, Offset(size.width, size.height - cLen), Offset(size.width, size.height), w, StrokeCap.Round)

                    val lineY = scanLineProgress * size.height
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.85f),
                        start = Offset(8f, lineY),
                        end = Offset(size.width - 8f, lineY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.62f))
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black.copy(alpha = 0.62f)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Point camera at barcode",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 24.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onPickFromGallery,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Photo,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick from gallery", color = Color.White)
                }
                TextButton(onClick = onManualEntry) {
                    Text(
                        text = "Enter code manually",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 8.dp, top = 8.dp),
        contentAlignment = Alignment.TopStart
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Close scanner",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ManualEntryDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter barcode") },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                placeholder = { Text("e.g. 4901085167572") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(code) }) { Text("Search") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
