package com.himel.xkcdviewer.zoomable

import androidx.compose.animation.core.*
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private fun calculateBounds(boxDimension: Float, imageDimension: Float): Float {
    return if (imageDimension < boxDimension) {
        0f
    } else {
        (imageDimension - boxDimension) / 2
    }
}

@Composable
fun ZoomableImage(
    bitmap: ImageBitmap,
    contentDescription: String,
    doubleTapZoom: Float = 2f,
    modifier: Modifier = Modifier,
) {
    // Set bitmap as key, so as to reset zoom and offset levels
    // on image change
    val scale = remember(bitmap) { Animatable(1f) }
    val offsetX = remember(bitmap) {
        Animatable(0f, Float.VectorConverter).apply { updateBounds(0f, 0f) }
    }
    val offsetY = remember(bitmap) {
        Animatable(0f, Float.VectorConverter).apply { updateBounds(0f, 0f) }
    }

    var boxSize by remember { mutableStateOf(Size(0f, 0f)) }

    val sourceBitmapSize = remember(bitmap) { Size(bitmap.width.toFloat(), bitmap.height.toFloat()) }
    val imgSize = remember(boxSize, sourceBitmapSize) {
        sourceBitmapSize * ContentScale.Fit.computeScaleFactor(sourceBitmapSize, boxSize).scaleX
    }

    var flingVelocity by remember(bitmap) { mutableStateOf(Offset(0f, 0f)) }
    val threshold = 2f

    val composableScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .clipToBounds()
            .onGloballyPositioned { coordinates ->
                boxSize = coordinates.size.toSize()
            }
            .pointerInput(boxSize, bitmap) {
                detectTapGestures(
                    onDoubleTap = { tapCoords ->
                        if (scale.value > 1f) {
                            composableScope.launch {
                                scale.animateTo(1f, FloatTweenSpec(300))
                            }

                            composableScope.launch {
                                try {
                                    offsetX.animateTo(0f, TweenSpec(300))
                                } finally {
                                    offsetX.updateBounds(0f, 0f)
                                }
                            }

                            composableScope.launch {
                                try {
                                    offsetY.animateTo(0f, TweenSpec(300))
                                } finally {
                                    offsetY.updateBounds(0f, 0f)
                                }
                            }
                        } else {
                            val xBound =
                                calculateBounds(boxSize.width, imgSize.width * doubleTapZoom)
                            val yBound =
                                calculateBounds(boxSize.height, imgSize.height * doubleTapZoom)

                            composableScope.launch {
                                scale.animateTo(doubleTapZoom, FloatTweenSpec(300))
                            }

                            composableScope.launch {
                                offsetX.updateBounds(-xBound, xBound)
                                offsetX.animateTo(
                                    ((doubleTapZoom - 1) * (boxSize.width / 2 - tapCoords.x)),
                                    TweenSpec(300)
                                )
                            }

                            composableScope.launch {
                                offsetY.updateBounds(-yBound, yBound)
                                offsetY.animateTo(
                                    ((doubleTapZoom - 1) * (boxSize.height / 2 - tapCoords.y)),
                                    TweenSpec(300)
                                )
                            }
                        }
                    }
                )
            }
            .pointerInput(boxSize, bitmap) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        val dist = Offset(dragAmount.x, dragAmount.y)
                        val timeDelta =
                            change
                                .run { uptimeMillis - previousUptimeMillis }
                                .toFloat()

                        flingVelocity = dist / timeDelta

                        composableScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                        }
                    },
                    onDragEnd = {
                        val magnitude = flingVelocity.getDistance()

                        if (magnitude.isFinite() && magnitude > threshold) {
                            val unitVector = flingVelocity * 1000f
                            flingVelocity = Offset.Zero

                            composableScope.launch {
                                offsetX.animateDecay(
                                    unitVector.x,
                                    exponentialDecay()
                                )
                            }

                            composableScope.launch {
                                offsetY.animateDecay(
                                    unitVector.y,
                                    exponentialDecay()
                                )
                            }
                        }
                    }
                )
            }
            .transformable(state = rememberTransformableState { zoomChange, panChange, _ ->
                composableScope.launch {
                    val newScale = (scale.value * zoomChange).coerceIn(1f, 7f)

                    val xBound = calculateBounds(boxSize.width, imgSize.width * newScale)
                    val yBound = calculateBounds(boxSize.height, imgSize.height * newScale)

                    scale.snapTo(newScale)

                    offsetX.updateBounds(-xBound, xBound)
                    offsetX.snapTo(offsetX.value + panChange.x)
                    offsetY.updateBounds(-yBound, yBound)
                    offsetY.snapTo(offsetY.value + panChange.y)
                }
            }),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .graphicsLayer(
                    scaleX = scale.value,
                    scaleY = scale.value,
                )
        )
    }
}