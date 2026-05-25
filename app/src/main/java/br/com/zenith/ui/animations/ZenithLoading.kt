package br.com.zenith.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.exp

@Composable
fun CenteredZenithLoading(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 120.dp,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ZenithLoading(modifier = Modifier.size(size))
    }
}

@Composable
fun ZenithLoading(
    modifier: Modifier = Modifier,
    strokeWidth: Float = 40f,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pathLoader")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3600
                0f    at 0    using CubicBezierEasing(0.85f, 0f, 0.15f, 1f)
                180f at 1600 using LinearEasing
                180f at 1800 using CubicBezierEasing(0.85f, 0f, 0.15f, 1f)
                360f at 3400 using LinearEasing
                360f at 3600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val breath by infiniteTransition.animateFloat(
        initialValue = 1.05f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3600
                1.05f at 0    using CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
                0.82f at 800  using CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
                1.05f at 1600 using LinearEasing
                1.05f at 1800 using CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
                0.82f at 2600 using CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
                1.05f at 3400 using LinearEasing
                1.05f at 3600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "breath"
    )

    val strokeColor = Color(0xFF238D25)

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3600
                0.18f at 0    using CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
                0.06f at 800  using CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
                0.22f at 1600 using LinearEasing
                0.22f at 1800 using CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
                0.06f at 2600 using CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
                0.22f at 3400 using LinearEasing
                0.18f at 3600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "glowAlpha"
    )


    Canvas(modifier = modifier) {
        val canvasW = size.width
        val canvasH = size.height

        val pathOrigW = 456.066f - 35.1907f
        val pathOrigH = 353.649f - 13.5028f

        val baseScale = minOf(a = canvasW / pathOrigW, b = canvasH / pathOrigH) * 0.9f

        val offsetX = (canvasW - pathOrigW * baseScale) / 2f - 35.1907f * baseScale
        val offsetY = (canvasH - pathOrigH * baseScale) / 2f - 13.5028f * baseScale

        fun fx(x: Float) = x * baseScale + offsetX
        fun fy(y: Float) = y * baseScale + offsetY

        val fullPath = Path().apply {
            moveTo(fx(35.1907f), fy(140.372f))
            cubicTo(
                fx(174.714f), fy(228.834f),
                fx(326.468f), fy(13.5028f),
                fx(319.07f),  fy(25.7461f)
            )
            cubicTo(
                fx(311.672f), fy(37.9894f),
                fx(180.234f), fy(344.277f),
                fx(175.816f), fy(353.649f)
            )
            cubicTo(
                fx(171.399f), fy(363.021f),
                fx(317.133f), fy(144.953f),
                fx(456.066f), fy(233.405f)
            )
        }

        withTransform({
            rotate(degrees = rotation, pivot = center)
            scale(scale = breath, pivot = center)
        }) {
            val layers   = 20

            for (i in layers downTo 1) {
                val t = i.toFloat() / layers
            }

            drawPath(
                path  = fullPath,
                color = strokeColor,
                style = Stroke(
                    width = strokeWidth,
                    cap   = StrokeCap.Square,
                    join  = StrokeJoin.Round
                )
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 260)
@Composable
fun ZenithLoadingPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        ZenithLoading(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        )
    }
}
