package com.example.feature.meterreading.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.example.ui.theme.LocalIsDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.ReadOnlyComposable
import com.example.feature.meterreading.model.MeterOcrStatus
import com.example.feature.meterreading.model.MeterReadingCandidate
import com.example.feature.meterreading.parser.ReadingPlausibilityStatus
import com.example.ui.theme.*
import java.util.Locale

private val BrandGreen: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) PrimaryDark else Color(0xFF386A20)
private val BrandGreenLight: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) PrimaryContainerDark else Color(0xFFE5F2D9)
private val WarningAmber = SolarEaseSemanticColors.WarningText
private val WarningAmberBg = SolarEaseSemanticColors.WarningContainer
private val ErrorRed = SolarEaseSemanticColors.ErrorText
private val ErrorRedBg = SolarEaseSemanticColors.ErrorContainer
private val SlateText: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) OnBackgroundDark else Color(0xFF1E293B)
private val SlateMuted: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) OnSurfaceVariantDark else Color(0xFF475569)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterReviewBottomSheet(
    croppedBitmap: Bitmap,
    candidate: MeterReadingCandidate,
    editableText: String,
    validationError: String?,
    plausibilityStatus: ReadingPlausibilityStatus,
    isSaving: Boolean,
    sheetState: SheetState,
    isAutoLocalized: Boolean = true,
    onAdjustCrop: () -> Unit = {},
    onTextChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onRetake: () -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onRetake,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Drag handle / Title
                Text(
                    text = "Review Meter Reading",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateText
                )

                // A. Captured Image Crop Preview with "Adjust Crop" button overlay
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = croppedBitmap.asImageBitmap(),
                            contentDescription = "Photographed meter display",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Adjust crop box action chip
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Viewfinder reticle crop",
                        fontSize = 12.sp,
                        color = SlateMuted,
                        fontWeight = FontWeight.Medium
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clickable(role = Role.Button) { onAdjustCrop() }
                            .semantics { role = Role.Button }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Adjust Display Box",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandGreen
                            )
                        }
                    }
                }

            // B. Recognition Status Chip
            val (chipBg, chipTextColor, chipBorder, chipIcon) = when (candidate.status) {
                MeterOcrStatus.DETECTED_SUCCESS -> {
                    Quad(BrandGreenLight, BrandGreen, BrandGreen.copy(alpha = 0.3f), Icons.Default.Check)
                }
                MeterOcrStatus.NON_KWH_WARNING -> {
                    Quad(WarningAmberBg, WarningAmber, WarningAmber.copy(alpha = 0.3f), Icons.Default.Warning)
                }
                MeterOcrStatus.AMBIGUOUS_VERIFY_NEEDED -> {
                    Quad(WarningAmberBg, WarningAmber, WarningAmber.copy(alpha = 0.3f), Icons.Default.Info)
                }
                else -> {
                    Quad(ErrorRedBg, ErrorRed, ErrorRed.copy(alpha = 0.3f), Icons.Default.Warning)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = chipBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = chipIcon,
                        contentDescription = null,
                        tint = chipTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = candidate.statusMessage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = chipTextColor
                    )
                }
            }

            // Plausibility Warning Note if lower than previous or large jump
            if (plausibilityStatus is ReadingPlausibilityStatus.Warning) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WarningAmberBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                        Text(
                            text = plausibilityStatus.message,
                            fontSize = 12.sp,
                            color = WarningAmber,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (plausibilityStatus is ReadingPlausibilityStatus.LowerThanPrevious) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ErrorRedBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                        Text(
                            text = plausibilityStatus.message,
                            fontSize = 12.sp,
                            color = ErrorRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // C & D. Editable Numeric Field
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Cumulative Units (kWh)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateText
                )

                OutlinedTextField(
                    value = editableText,
                    onValueChange = onTextChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. 16596.7") },
                    trailingIcon = {
                        Text(
                            text = "kWh",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateMuted,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    isError = validationError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                if (validationError != null) {
                    Text(
                        text = validationError,
                        fontSize = 12.sp,
                        color = ErrorRed,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Text(
                    text = "Preserve decimal digit (e.g. .7) if shown on your meter display.",
                    fontSize = 11.sp,
                    color = SlateMuted,
                    modifier = Modifier.padding(start = 4.dp)
                )

                // Optional alternative reading chip (e.g. 7-segment 5 vs 6 or decimal point ambiguity)
                candidate.alternativeCandidate?.let { alt ->
                    val altFormatted = if (alt % 1.0 == 0.0) {
                        String.format(Locale.US, "%.0f", alt)
                    } else {
                        String.format(Locale.US, "%.1f", alt)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTextChanged(altFormatted) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Did display show $altFormatted kWh?",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E40AF)
                                )
                            }
                            Text(
                                text = "Use $altFormatted",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }
                }
            }

            // E. Three Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onConfirm,
                    enabled = editableText.isNotBlank() && !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Confirm Reading", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                OutlinedButton(
                    onClick = onRetake,
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retake Photo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                TextButton(
                    onClick = onManualEntry,
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp), tint = SlateMuted)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enter Manually Instead", color = SlateMuted, fontSize = 13.sp)
                }
            }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
