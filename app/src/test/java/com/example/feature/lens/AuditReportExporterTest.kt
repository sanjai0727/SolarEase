package com.example.feature.lens

import com.example.feature.lens.ui.EditableAppliance
import com.example.feature.lens.ui.Room
import com.example.feature.lens.ui.RoomStatus
import com.example.feature.lens.utils.AuditReportExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.charset.StandardCharsets
import java.util.Date

class AuditReportExporterTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    @Test
    fun `test normal audit export contains expected headings, metrics, tables, and solar sizing`() {
        val fan = EditableAppliance(
            id = "app_1",
            label = "Ceiling Fan",
            powerWatts = 75f,
            usageHoursPerDay = 12f,
            monthlyUsageDays = 30,
            quantity = 2
        )
        val tv = EditableAppliance(
            id = "app_2",
            label = "Television (LED 43\")",
            powerWatts = 100f,
            usageHoursPerDay = 5f,
            monthlyUsageDays = 30,
            quantity = 1
        )
        val fridge = EditableAppliance(
            id = "app_3",
            label = "Refrigerator (Single Door)",
            powerWatts = 150f,
            usageHoursPerDay = 24f,
            monthlyUsageDays = 30,
            quantity = 1
        )

        val room1 = Room(
            id = "r_1",
            name = "Living Room",
            appliances = listOf(fan, tv),
            status = RoomStatus.COMPLETED,
            order = 0
        )
        val room2 = Room(
            id = "r_2",
            name = "Kitchen",
            appliances = listOf(fridge),
            status = RoomStatus.COMPLETED,
            order = 1
        )

        val md = AuditReportExporter.generateMarkdownContent(listOf(room1, room2), Date(1700000000000L))

        assertTrue("Should include document title", md.contains("# SolarEase Energy Audit Report"))
        assertTrue("Should include executive summary", md.contains("## 1. Executive Summary"))
        assertTrue("Should report 2 surveyed zones", md.contains("| Surveyed Zones | 2 |"))
        assertTrue("Should report 4 total appliances (2 fans + 1 tv + 1 fridge)", md.contains("| Total Connected Appliances | 4 |"))
        assertTrue("Should include solar feasibility section", md.contains("## 2. Solar PV Sizing & Offset Feasibility"))
        assertFalse("Normal audit must NOT show insufficient data warning", md.contains("Notice: Insufficient Audit Data"))
        assertTrue("Should contain Living Room zone", md.contains("### 3.1. Living Room (COMPLETED)"))
        assertTrue("Should contain Kitchen zone", md.contains("### 3.2. Kitchen (COMPLETED)"))
        assertTrue("Should list Ceiling Fan in table", md.contains("| Ceiling Fan | 2 | 75 W |"))
        assertTrue("Should list Refrigerator in table", md.contains("| Refrigerator (Single Door) | 1 | 150 W |"))
        assertTrue("Should include complete appliance inventory", md.contains("## 4. Complete Appliance Inventory"))
        assertTrue("Should include methodology and disclaimers", md.contains("## 5. Audit Methodology & Disclaimers"))
    }

    @Test
    fun `test zero-load and insufficient-data audit shows explicit warning and zeroed solar sizing`() {
        val emptyRoom1 = Room(
            id = "r_1",
            name = "Bedroom",
            appliances = emptyList(),
            status = RoomStatus.PENDING,
            order = 0
        )
        val emptyRoom2 = Room(
            id = "r_2",
            name = "Guest Room",
            appliances = emptyList(),
            status = RoomStatus.SKIPPED,
            order = 1
        )

        val md = AuditReportExporter.generateMarkdownContent(listOf(emptyRoom1, emptyRoom2))

        assertTrue("Must display explicit insufficient data warning", md.contains("⚠️ **Notice: Insufficient Audit Data**"))
        assertTrue("Must display 0.0 kW recommended size", md.contains("| Recommended System Size | 0.0 kW (Insufficient Data) |"))
        assertTrue("Must display 0.0 kWh generation", md.contains("| Estimated Monthly Generation | 0.0 kWh/month |"))
        assertTrue("Must display 0.0% offset", md.contains("| Grid Energy Offset | 0.0% |"))
        assertTrue("Must display ₹0.00 savings", md.contains("| Estimated Annual Bill Savings | ₹0.00 |"))
        assertFalse("Must never fabricate non-zero solar recommendations", md.contains("Recommended Solar System Capacity"))
    }

    @Test
    fun `test empty rooms list handles gracefully without errors`() {
        val md = AuditReportExporter.generateMarkdownContent(emptyList())

        assertTrue("Title present", md.contains("# SolarEase Energy Audit Report"))
        assertTrue("Notice for empty zones present", md.contains("_No rooms configured for this audit._"))
        assertTrue("Insufficient data warning present", md.contains("Notice: Insufficient Audit Data"))
    }

    @Test
    fun `test special characters and long names are safely sanitized for markdown tables`() {
        val rawRoomName = "Master Bed | High-Voltage & AC [Zone 1] \n Second Line"
        val rawAppLabel = "Special | Split AC `Inverter` *High Load* \r\n 5-Star"

        val sanitizedRoom = AuditReportExporter.sanitizeMarkdownText(rawRoomName)
        val sanitizedApp = AuditReportExporter.sanitizeMarkdownText(rawAppLabel)

        assertFalse("Pipe characters must be escaped", sanitizedRoom.contains(" | "))
        assertTrue("Pipes must be escaped to \\|", sanitizedRoom.contains("\\|"))
        assertFalse("Newlines must be replaced with spaces", sanitizedRoom.contains("\n"))
        assertFalse("Newlines must be replaced with spaces", sanitizedApp.contains("\r"))
        assertFalse("Backticks should be converted", sanitizedApp.contains("`"))

        val room = Room(
            id = "r_spec",
            name = rawRoomName,
            appliances = listOf(
                EditableAppliance(
                    id = "a_spec",
                    label = rawAppLabel,
                    powerWatts = 1500f,
                    usageHoursPerDay = 4f,
                    monthlyUsageDays = 30,
                    quantity = 1
                )
            ),
            status = RoomStatus.COMPLETED
        )

        val md = AuditReportExporter.generateMarkdownContent(listOf(room))
        assertTrue("Markdown generated successfully", md.isNotEmpty())
        assertTrue("Sanitized label appears in table row", md.contains("Special \\| Split AC 'Inverter'"))
    }

    @Test
    fun `test file creation writes non-empty valid UTF-8 file in reports directory`() {
        val reportsDir = tempFolder.newFolder("reports")

        val fan = EditableAppliance(
            id = "app_1",
            label = "Table Fan",
            powerWatts = 50f,
            usageHoursPerDay = 8f,
            monthlyUsageDays = 30,
            quantity = 1
        )
        val room = Room(
            id = "r_1",
            name = "Study Room",
            appliances = listOf(fan),
            status = RoomStatus.COMPLETED
        )

        val result = AuditReportExporter.exportReport(reportsDir, listOf(room), Date())
        assertTrue("Export must succeed", result.isSuccess)

        val file = result.getOrNull()
        assertNotNull("File must not be null", file)
        assertTrue("File must exist on disk", file!!.exists())
        assertTrue("File must be non-empty", file.length() > 0)
        assertTrue("Filename must follow pattern", file.name.startsWith("solarease_energy_audit_"))
        assertTrue("Filename must have .md extension", file.name.endsWith(".md"))

        val content = file.readText(StandardCharsets.UTF_8)
        assertTrue("Must be valid UTF-8 content", content.startsWith("# SolarEase Energy Audit Report"))
        assertTrue("Contains room name", content.contains("Study Room"))
        assertTrue("Contains appliance", content.contains("Table Fan"))
    }
}
