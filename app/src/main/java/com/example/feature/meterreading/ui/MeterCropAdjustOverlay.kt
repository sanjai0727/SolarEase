package com.example.feature.meterreading.ui

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.platform.LocalDensity
import com.example.feature.meterreading.util.CameraCoordinateMapper
import kotlin.math.hypot

private val BrandGreen = Color(0xFF386A20)
private val ReticleEmerald = Color(0xFF10B981)

private enum class DragHandle { NONE, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/**
 * Interactive on-device display crop adjuster.
 * Allows the user to select or fine-tune the exact bounding box around the meter LCD
 * or rotating number wheels, completely eliminating erroneous faceplate readings.
 * Accurately aligns the selection reticle with the [ContentScale.Fit] rendered bitmap.
 */
@Composable
fun MeterCropAdjustOverlay(
    fullBitmap: Bitmap,
    initialRectNormalized: RectF,
    onApply: (RectF) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rectNorm by remember { mutableStateOf(initialRectNormalized) }
    var activeHandle by remember { mutableStateOf(DragHandle.NONE) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Display full meter photo fitted to screen
        Image(
            bitmap = fullBitmap.asImageBitmap(),
            contentDescription = "Full meter photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Interactive dragging canvas
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val canvasW = constraints.maxWidth.toFloat()
            val canvasH = constraints.maxHeight.toFloat()

            val fit = remember(canvasW, canvasH, fullBitmap.width, fullBitmap.height) {
                CameraCoordinateMapper.calculateFitBounds(
                    containerWidth = canvasW,
                    containerHeight = canvasH,
                    imageWidth = fullBitmap.width.toFloat(),
                    imageHeight = fullBitmap.height.toFloat()
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(fit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val leftPx = fit.destLeft + rectNorm.left * fit.destWidth
                                val topPx = fit.destTop + rectNorm.top * fit.destHeight
                                val widthPx = rectNorm.width() * fit.destWidth
                                val heightPx = rectNorm.height() * fit.destHeight

                                val cornerRadius = with(density) { 36.dp.toPx() }
                                val dTL = hypot(offset.x - leftPx, offset.y - topPx)
                                val dTR = hypot(offset.x - (leftPx + widthPx), offset.y - topPx)
                                val dBL = hypot(offset.x - leftPx, offset.y - (topPx + heightPx))
                                val dBR = hypot(offset.x - (leftPx + widthPx), offset.y - (topPx + heightPx))

                                activeHandle = when {
                                    dTL <= cornerRadius -> DragHandle.TOP_LEFT
                                    dTR <= cornerRadius -> DragHandle.TOP_RIGHT
                                    dBL <= cornerRadius -> DragHandle.BOTTOM_LEFT
                                    dBR <= cornerRadius -> DragHandle.BOTTOM_RIGHT
                                    offset.x in leftPx..(leftPx + widthPx) && offset.y in topPx..(topPx + heightPx) -> DragHandle.MOVE
                                    else -> DragHandle.MOVE
                                }
                            },
                            onDragEnd = { activeHandle = DragHandle.NONE },
                            onDragCancel = { activeHandle = DragHandle.NONE },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (fit.destWidth > 0f && fit.destHeight > 0f) {
                                    val deltaNormX = dragAmount.x / fit.destWidth
                                    val deltaNormY = dragAmount.y / fit.destHeight
                                    val minW = 0.10f
                                    val minH = 0.05f

                                    rectNorm = when (activeHandle) {
                                        DragHandle.TOP_LEFT -> {
                                            val newL = (rectNorm.left + deltaNormX).coerceIn(0f, rectNorm.right - minW)
                                            val newT = (rectNorm.top + deltaNormY).coerceIn(0f, rectNorm.bottom - minH)
                                            RectF(newL, newT, rectNorm.right, rectNorm.bottom)
                                        }
                                        DragHandle.TOP_RIGHT -> {
                                            val newR = (rectNorm.right + deltaNormX).coerceIn(rectNorm.left + minW, 1f)
                                            val newT = (rectNorm.top + deltaNormY).coerceIn(0f, rectNorm.bottom - minH)
                                            RectF(rectNorm.left, newT, newR, rectNorm.bottom)
                                        }
                                        DragHandle.BOTTOM_LEFT -> {
                                            val newL = (rectNorm.left + deltaNormX).coerceIn(0f, rectNorm.right - minW)
                                            val newB = (rectNorm.bottom + deltaNormY).coerceIn(rectNorm.top + minH, 1f)
                                            RectF(newL, rectNorm.top, rectNorm.right, newB)
                                        }
                                        DragHandle.BOTTOM_RIGHT -> {
                                            val newR = (rectNorm.right + deltaNormX).coerceIn(rectNorm.left + minW, 1f)
                                            val newB = (rectNorm.bottom + deltaNormY).coerceIn(rectNorm.top + minH, 1f)
                                            RectF(rectNorm.left, rectNorm.top, newR, newB)
                                        }
                                        DragHandle.MOVE, DragHandle.NONE -> {
                                            val curW = rectNorm.width()
                                            val curH = rectNorm.height()
                                            val newL = (rectNorm.left + deltaNormX).coerceIn(0f, 1f - curW)
                                            val newT = (rectNorm.top + deltaNormY).coerceIn(0f, 1f - curH)
                                            RectF(newL, newT, newL + curW, newT + curH)
                                        }
                                    }
                                }
                            }
                        )
                    }
            ) {
                val leftPx = fit.destLeft + rectNorm.left * fit.destWidth
                val topPx = fit.destTop + rectNorm.top * fit.destHeight
                val widthPx = rectNorm.width() * fit.destWidth
                val heightPx = rectNorm.height() * fit.destHeight

                // Semi-transparent darkened mask outside crop box
                drawRect(color = Color.Black.copy(alpha = 0.65f))

                // Cut out selected viewport inside the image
                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset(leftPx, topPx),
                    size = Size(widthPx, heightPx),
                    blendMode = BlendMode.Clear
                )

                // High-visibility bounding frame
                drawRect(
                    color = ReticleEmerald,
                    topLeft = Offset(leftPx, topPx),
                    size = Size(widthPx, heightPx),
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Corner handles
                val handleLen = 20.dp.toPx()
                val handleThick = 4.dp.toPx()
                val cornerCol = Color.White

                // Top-Left
                drawLine(cornerCol, Offset(leftPx, topPx), Offset(leftPx + handleLen, topPx), handleThick)
                drawLine(cornerCol, Offset(leftPx, topPx), Offset(leftPx, topPx + handleLen), handleThick)
                // Top-Right
                drawLine(cornerCol, Offset(leftPx + widthPx, topPx), Offset(leftPx + widthPx - handleLen, topPx), handleThick)
                drawLine(cornerCol, Offset(leftPx + widthPx, topPx), Offset(leftPx + widthPx, topPx + handleLen), handleThick)
                // Bottom-Left
                drawLine(cornerCol, Offset(leftPx, topPx + heightPx), Offset(leftPx + handleLen, topPx + heightPx), handleThick)
                drawLine(cornerCol, Offset(leftPx, topPx + heightPx), Offset(leftPx, topPx + heightPx - handleLen), handleThick)
                // Bottom-Right
                drawLine(cornerCol, Offset(leftPx + widthPx, topPx + heightPx), Offset(leftPx + widthPx - handleLen, topPx + heightPx), handleThick)
                drawLine(cornerCol, Offset(leftPx + widthPx, topPx + heightPx), Offset(leftPx + widthPx, topPx + heightPx - handleLen), handleThick)
            }
        }

        // Top guidance banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.90f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp, start = 20.dp, end = 20.dp)
        ) {
            Text(
                text = "Drag the box to frame only the LCD display or rotating number wheels",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        // Bottom action buttons
        Surface(
            color = Color(0xFF0F172A).copy(alpha = 0.92f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("Cancel", color = Color.White, modifier = Modifier.padding(start = 6.dp))
                }

                Button(
                    onClick = { onApply(rectNorm) },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text("Apply Crop", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}
