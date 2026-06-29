package com.whatdrink.app.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

private class SakuraPetal(
    var x: Float,
    var y: Float,
    var rotation: Float,
    val speed: Float,
    val size: Float,
    val swayAmplitude: Float,
    val swayFrequency: Float,
    val swayOffset: Float,
    val horizontalDrift: Float,
    var time: Float = 0f
)

private fun newPetal(screenWidth: Float, screenHeight: Float, spreadY: Boolean = false): SakuraPetal {
    return SakuraPetal(
        x = Random.nextFloat() * screenWidth * 0.35f,
        y = if (spreadY) Random.nextFloat() * screenHeight else -Random.nextFloat() * 80f - 20f,
        rotation = Random.nextFloat() * 360f,
        speed = Random.nextFloat() * 1.8f + 1.2f,
        size = Random.nextFloat() * 15f + 10f,
        swayAmplitude = Random.nextFloat() * 1.2f + 0.4f,
        swayFrequency = Random.nextFloat() * 1.2f + 0.6f,
        swayOffset = Random.nextFloat() * (Math.PI * 2).toFloat(),
        horizontalDrift = Random.nextFloat() * 0.6f + 0.2f
    )
}

private fun DrawScope.drawPetal(petal: SakuraPetal) {
    val path = Path().apply {
        val w = petal.size
        val h = petal.size * 1.6f
        moveTo(0f, -h / 2f)
        cubicTo(w / 2f, -h / 4f, w / 2f, h / 4f, 0f, h / 2f)
        cubicTo(-w / 2f, h / 4f, -w / 2f, -h / 4f, 0f, -h / 2f)
        close()
    }
    val innerPath = Path().apply {
        val w = petal.size * 0.45f
        val h = petal.size * 0.75f
        moveTo(0f, -h / 2f)
        cubicTo(w / 2f, -h / 4f, w / 2f, h / 4f, 0f, h / 2f)
        cubicTo(-w / 2f, h / 4f, -w / 2f, -h / 4f, 0f, -h / 2f)
        close()
    }
    withTransform({
        translate(petal.x, petal.y)
        rotate(petal.rotation)
    }) {
        drawPath(path, color = Color(0xFFFFB7C5).copy(alpha = 0.88f))
        drawPath(innerPath, color = Color(0xFFFFE4EE).copy(alpha = 0.55f))
    }
}

@Composable
fun SakuraAnimation(modifier: Modifier = Modifier) {
    var screenSize by remember { mutableStateOf(IntSize.Zero) }
    var tick by remember { mutableLongStateOf(0L) }
    val petals = remember { mutableListOf<SakuraPetal>() }

    LaunchedEffect(screenSize) {
        if (screenSize != IntSize.Zero && petals.isEmpty()) {
            repeat(30) {
                petals.add(newPetal(screenSize.width.toFloat(), screenSize.height.toFloat(), spreadY = true))
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            if (screenSize == IntSize.Zero) continue
            val w = screenSize.width.toFloat()
            val h = screenSize.height.toFloat()
            petals.forEach { petal ->
                petal.time += 0.016f
                petal.y += petal.speed
                petal.x += petal.horizontalDrift + petal.swayAmplitude * sin(
                    petal.time * petal.swayFrequency + petal.swayOffset
                ).toFloat()
                petal.rotation += 0.7f
                if (petal.y > h + 60f || petal.x > w + 60f) {
                    val fresh = newPetal(w, h)
                    petal.x = fresh.x
                    petal.y = fresh.y
                    petal.time = 0f
                }
            }
            tick++
        }
    }

    // Reading tick here (composable scope) subscribes this composable to each frame tick,
    // causing Canvas to redraw with updated petal positions.
    val frame = tick

    Canvas(modifier = modifier.onSizeChanged { screenSize = it }) {
        petals.forEach { drawPetal(it) }
    }
}
