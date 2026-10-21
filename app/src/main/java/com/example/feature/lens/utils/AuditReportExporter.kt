package com.example.feature.lens.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.feature.assessment.util.EstimateConstants
import com.example.feature.lens.ui.Room
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

/**
 * Generates and exports comprehensive Markdown energy audit reports for SolarEase Lens.
 * Replaces placeholder PDF actions with genuine, shareable UTF-8 Markdown documents.
 */
object AuditReportExporter {

    private const val REPORTS_DIR = "reports"
    private const val MIME_TYPE_MARKDOWN = "text/markdown"

    /**
     * Sanitizes user-entered room and appliance text to preserve Markdown table formatting
     * and prevent layout corruption.
     */
    fun sanitizeMarkdownText(text: String?, maxLength: Int = 80): String {
        if (text.isNullOrBlank()) return "Unspecified"
        return text
            .replace("\r\n", " ")
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("|", "\\|")
            .replace("`", "'")
            .trim()
            .take(maxLength)
            .ifEmpty { "Unspecified" }
    }

    /**
     * Generates standard Markdown audit report content from surveyed rooms.
     */
    fun generateMarkdownContent(rooms: List<Room>, date: Date = Date()): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(date)
        val tariff = EstimateConstants.TARIFF_PER_KWH

        val totalConnectedKw: Double = rooms.sumOf { it.connectedLoadKw.toDouble() }
        val simultaneousKw: Double = totalConnectedKw * 0.68
        val totalMonthlyKwh: Double = rooms.sumOf { it.monthlyKwh.toDouble() }
        val totalMonthlyBill: Double = rooms.sumOf { it.monthlyCost.toDouble() }
        val totalAppliancesCount: Int = rooms.sumOf { room -> room.appliances.sumOf { it.quantity } }

        val hasValidInventory = totalMonthlyKwh > 0.0 && totalAppliancesCount > 0
        val dailyAvgKwh: Double = if (totalMonthlyKwh > 0.0) totalMonthlyKwh / 30.0 else 0.0

        // Solar sizing formulas strictly matching ApplianceResultScreen
        val rawPvSize: Double = if (hasValidInventory) dailyAvgKwh / (4.5 * 0.79) else 0.0
        val recommendedPvKw: Double = if (!hasValidInventory) 0.0 else (ceil(rawPvSize * 2.0) / 2.0).coerceAtLeast(1.0)
        val solarMonthlyGen: Double = if (hasValidInventory) recommendedPvKw * 4.33 * 30.0 * 0.79 else 0.0
        val annualSavings: Double = if (hasValidInventory) solarMonthlyGen * 12.0 * tariff else 0.0
        val monthlySavings: Double = if (hasValidInventory) solarMonthlyGen * tariff else 0.0
        val offsetPct: Double = if (hasValidInventory && totalMonthlyKwh > 0.0) {
            ((solarMonthlyGen / totalMonthlyKwh) * 100.0).coerceAtMost(150.0)
        } else {
            0.0
        }

        val sb = StringBuilder()

        // Document Title & Metadata
        sb.append("# SolarEase Energy Audit Report\n\n")
        sb.append("**Date Generated:** $dateStr  \n")
        sb.append("**Audit Type:** SolarEase Lens Whole-Home Energy Audit  \n")
        sb.append("**Applied Tariff Rate:** ₹${String.format(Locale.US, "%.2f", tariff)} / kWh  \n\n")
        sb.append("---\n\n")

        // 1. Executive Summary
        sb.append("## 1. Executive Summary\n\n")
        sb.append("| Metric | Value |\n")
        sb.append("|:---|:---|\n")
        sb.append("| Surveyed Zones | ${rooms.size} |\n")
        sb.append("| Total Connected Appliances | $totalAppliancesCount |\n")
        sb.append("| Peak Connected Load | ${String.format(Locale.US, "%.2f", totalConnectedKw)} kW |\n")
        sb.append("| Estimated Simultaneous Peak (0.68 Diversity) | ${String.format(Locale.US, "%.2f", simultaneousKw)} kW |\n")
        sb.append("| Estimated Monthly Energy Consumption | ${String.format(Locale.US, "%.1f", totalMonthlyKwh)} kWh/month |\n")
        sb.append("| Estimated Monthly Electricity Cost | ₹${String.format(Locale.US, "%.2f", totalMonthlyBill)}/month |\n\n")

        sb.append("---\n\n")

        // 2. Solar PV Sizing & Offset Feasibility
        sb.append("## 2. Solar PV Sizing & Offset Feasibility\n\n")
        if (!hasValidInventory) {
            sb.append("> ⚠️ **Notice: Insufficient Audit Data**  \n")
            sb.append("> No electrical load was recorded during this audit. Solar capacity sizing, estimated generation, grid offset, and financial savings recommendations cannot be computed. Please complete a room inventory scan before calculating solar requirements.\n\n")
            sb.append("| Solar Feasibility Metric | Status |\n")
            sb.append("|:---|:---|\n")
            sb.append("| Recommended System Size | 0.0 kW (Insufficient Data) |\n")
            sb.append("| Estimated Monthly Generation | 0.0 kWh/month |\n")
            sb.append("| Grid Energy Offset | 0.0% |\n")
            sb.append("| Estimated Monthly Bill Savings | ₹0.00 |\n")
            sb.append("| Estimated Annual Bill Savings | ₹0.00 |\n\n")
        } else {
            sb.append("| Solar Feasibility Metric | Value |\n")
            sb.append("|:---|:---|\n")
            sb.append("| Recommended Solar System Capacity | ${String.format(Locale.US, "%.1f", recommendedPvKw)} kW |\n")
            sb.append("| Estimated Monthly Solar Generation | ${String.format(Locale.US, "%.1f", solarMonthlyGen)} kWh/month |\n")
            sb.append("| Projected Grid Energy Offset | ${String.format(Locale.US, "%.1f", offsetPct)}% |\n")
            sb.append("| Estimated Monthly Bill Savings | ₹${String.format(Locale.US, "%.2f", monthlySavings)}/month |\n")
            sb.append("| Estimated Annual Bill Savings | ₹${String.format(Locale.US, "%.2f", annualSavings)}/year |\n\n")
            sb.append("> **Assumptions:** Calculated assuming 4.5 Peak Sun Hours (PSH), 0.79 Performance Ratio (PR), and domestic net-metering tariff of ₹${String.format(Locale.US, "%.2f", tariff)}/kWh.\n\n")
        }

        sb.append("---\n\n")

        // 3. Room-by-Room Breakdown
        sb.append("## 3. Zone Breakdown\n\n")
        if (rooms.isEmpty()) {
            sb.append("_No rooms configured for this audit._\n\n")
        } else {
            rooms.forEachIndexed { index, room ->
                val safeRoomName = sanitizeMarkdownText(room.name)
                val roomKw = String.format(Locale.US, "%.2f", room.connectedLoadKw)
                val roomKwh = String.format(Locale.US, "%.1f", room.monthlyKwh)
                val roomCost = String.format(Locale.US, "%.2f", room.monthlyCost)

                sb.append("### 3.${index + 1}. $safeRoomName (${room.status.name})\n\n")
                sb.append("- **Connected Load:** $roomKw kW\n")
                sb.append("- **Estimated Monthly Consumption:** $roomKwh kWh/mo (₹$roomCost/mo)\n\n")

                if (room.appliances.isEmpty()) {
                    sb.append("> _No electrical appliances recorded in this zone._\n\n")
                } else {
                    sb.append("| Appliance | Qty | Rated Power | Daily Usage | Days/Mo | Monthly Energy | Monthly Cost |\n")
                    sb.append("|:---|:---:|:---:|:---:|:---:|:---:|:---:|\n")
                    room.appliances.forEach { app ->
                        val safeLabel = sanitizeMarkdownText(app.label)
                        val powerStr = if (app.powerWatts > 0f) "${app.powerWatts.toInt()} W" else "0 W (Unspecified)"
                        val hoursStr = "${String.format(Locale.US, "%.1f", app.usageHoursPerDay)} h"
                        val kwhStr = "${String.format(Locale.US, "%.1f", app.monthlyKwh)} kWh"
                        val costStr = "₹${String.format(Locale.US, "%.2f", app.monthlyCost)}"

                        sb.append("| $safeLabel | ${app.quantity} | $powerStr | $hoursStr | ${app.monthlyUsageDays} | $kwhStr | $costStr |\n")
                    }
                    sb.append("\n")
                }
            }
        }

        sb.append("---\n\n")

        // 4. Consolidated Appliance Inventory
        sb.append("## 4. Complete Appliance Inventory\n\n")
        val allAppliances = rooms.flatMap { room -> room.appliances.map { room to it } }
        if (allAppliances.isEmpty()) {
            sb.append("_No appliances registered across any surveyed zone._\n\n")
        } else {
            sb.append("| Zone | Appliance | Quantity | Rated Power | Daily Usage | Monthly Energy | Monthly Cost |\n")
            sb.append("|:---|:---|:---:|:---:|:---:|:---:|:---:|\n")
            allAppliances.forEach { (room, app) ->
                val safeRoom = sanitizeMarkdownText(room.name)
                val safeLabel = sanitizeMarkdownText(app.label)
                val powerStr = if (app.powerWatts > 0f) "${app.powerWatts.toInt()} W" else "0 W"
                val hoursStr = "${String.format(Locale.US, "%.1f", app.usageHoursPerDay)} h"
                val kwhStr = "${String.format(Locale.US, "%.1f", app.monthlyKwh)} kWh"
                val costStr = "₹${String.format(Locale.US, "%.2f", app.monthlyCost)}"

                sb.append("| $safeRoom | $safeLabel | ${app.quantity} | $powerStr | $hoursStr | $kwhStr | $costStr |\n")
            }
            sb.append("\n")
        }

        sb.append("---\n\n")

        // 5. Methodology & Disclaimers
        sb.append("## 5. Audit Methodology & Disclaimers\n\n")
        sb.append("- Appliance energy estimates are computed via the formula: `(Rated Power in Watts * Daily Hours * Days per Month * Quantity) / 1000`.\n")
        sb.append("- Electricity cost projections use the domestic standard grid tariff of ₹${String.format(Locale.US, "%.2f", tariff)}/kWh.\n")
        sb.append("- Solar PV generation estimates use a standard regional solar insolation of 4.5 PSH with a system Performance Ratio (PR) of 0.79.\n")
        sb.append("- Actual rooftop solar output may vary depending on local azimuth, tilt angle, temperature coefficients, shading factors, and seasonal cloud coverage.\n")

        return sb.toString()
    }

    /**
     * Creates a UTF-8 Markdown file in reportsDir and returns the generated File.
     */
    fun exportReport(reportsDir: File, rooms: List<Room>, date: Date = Date()): Result<File> {
        return try {
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(date)
            val file = File(reportsDir, "solarease_energy_audit_$timestamp.md")

            val content = generateMarkdownContent(rooms, date)

            OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8).use { writer ->
                writer.write(content)
                writer.flush()
            }

            if (!file.exists() || file.length() == 0L) {
                Result.failure(IllegalStateException("Exported report file is empty or could not be created."))
            } else {
                Result.success(file)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a UTF-8 Markdown file in cacheDir/reports and returns the generated File.
     */
    fun exportReport(context: Context, rooms: List<Room>, date: Date = Date()): Result<File> {
        return exportReport(File(context.cacheDir, REPORTS_DIR), rooms, date)
    }

    /**
     * Opens the Android native share sheet to save or share the generated Markdown report.
     */
    fun shareReport(context: Context, file: File): Result<Unit> {
        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = MIME_TYPE_MARKDOWN
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "SolarEase Energy Audit Report (${file.name})")
                putExtra(Intent.EXTRA_TEXT, "Attached is the SolarEase Energy Audit Report in Markdown format.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Export Energy Audit Report (.md)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
