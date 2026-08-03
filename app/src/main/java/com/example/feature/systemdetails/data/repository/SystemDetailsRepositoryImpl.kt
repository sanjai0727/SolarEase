package com.example.feature.systemdetails.data.repository

import com.example.feature.systemdetails.domain.model.*
import com.example.feature.systemdetails.domain.repository.SystemDetailsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SystemDetailsRepositoryImpl : SystemDetailsRepository {
    override fun getSystemDetails(): Flow<Result<SolarSystemDetails>> = flow {
        delay(1500)
        
        val currentTime = System.currentTimeMillis()
        val oneDay = 86400000L
        
        val mockData = SolarSystemDetails(
            systemCapacity = 5.0,
            systemType = "On Grid",
            plantStatus = "Online",
            installationDate = currentTime - oneDay * 30, // 30 days ago
            plantHealth = 98,
            installationAddress = "No.12 Anna Nagar, Chennai",
            liveStatus = LiveStatus(
                currentPowerOutput = 2.8,
                todayGeneration = 18.4,
                gridStatus = "Connected",
                lastUpdated = currentTime - 60000,
                weatherCondition = "Sunny"
            ),
            panelInfo = PanelInformation(
                manufacturer = "SunPower",
                model = "Maxeon 3",
                wattage = 400,
                count = 12,
                technology = "Monocrystalline",
                efficiency = 22.6,
                warranty = "25 Years",
                orientation = "South (180°)",
                tiltAngle = "15°",
                mountType = "Roof Mount"
            ),
            inverterInfo = InverterInformation(
                manufacturer = "SolarEdge",
                model = "SE5000H",
                capacity = 5.0,
                serialNumber = "SE-2026-987654",
                firmwareVersion = "v4.12.3",
                warranty = "12 Years",
                status = "Operating normally"
            ),
            installationInfo = InstallationInformation(
                company = "SolarEase Energy Solutions",
                engineerName = "Ravi Kumar",
                installationDate = currentTime - oneDay * 30,
                commissioningDate = currentTime - oneDay * 28,
                gridSyncDate = currentTime - oneDay * 25,
                surveyDate = currentTime - oneDay * 45,
                completionDate = currentTime - oneDay * 25
            ),
            roofLayout = RoofLayout(
                roofOrientation = "South Facing",
                areaUsed = 24.5,
                areaAvailable = 35.0
            ),
            specifications = SystemSpecification(
                estimatedAnnualGeneration = 7500.0,
                performanceRatio = 0.78,
                dcCapacity = 4.8,
                acCapacity = 5.0,
                expectedLifetime = 25,
                systemVoltage = "230V / 50Hz",
                netMeterNumber = "NM-84739201",
                electricityBoard = "TANGEDCO",
                connectionType = "Three Phase"
            ),
            warranty = WarrantySummary(
                panelWarranty = "Valid",
                inverterWarranty = "Valid",
                installationWarranty = "Valid",
                amcStatus = "Active",
                panelExpiryDate = currentTime + oneDay * 365 * 25,
                inverterExpiryDate = currentTime + oneDay * 365 * 12,
                installationExpiryDate = currentTime + oneDay * 365 * 5
            ),
            health = SystemHealth(
                overallHealth = "Excellent",
                healthPercentage = 98,
                generationEfficiency = 96.5,
                lastInspectionDate = currentTime - oneDay * 30,
                dustCleaningStatus = "Clean",
                nextMaintenanceDate = currentTime + oneDay * 180
            ),
            serviceHistory = listOf(
                ServiceHistoryItem("Warranty Activated", currentTime - oneDay * 25, "System warranty has been activated.", true),
                ServiceHistoryItem("Grid Sync Completed", currentTime - oneDay * 25, "Net meter installed and synced with grid.", true),
                ServiceHistoryItem("Installation Completed", currentTime - oneDay * 28, "Panels and inverter installed.", true),
                ServiceHistoryItem("Survey Completed", currentTime - oneDay * 45, "Site feasibility and shading analysis completed.", true)
            ),
            insights = listOf(
                Insight("Your system generated 8% more energy than yesterday.", "positive"),
                Insight("Your panels are operating efficiently.", "positive"),
                Insight("Estimated savings this month ₹3,450.", "positive"),
                Insight("Maintenance not required at this time.", "neutral")
            ),
            documents = listOf(
                DocumentShortcut("Proposal", "proposal"),
                DocumentShortcut("Invoice", "invoice"),
                DocumentShortcut("Warranty Certificate", "warranty"),
                DocumentShortcut("Installation Report", "report"),
                DocumentShortcut("Net Meter Approval", "approval")
            )
        )
        emit(Result.success(mockData))
    }
}
