package com.example.feature.systemdetails.domain.model

data class LiveStatus(
    val currentPowerOutput: Double,
    val todayGeneration: Double,
    val gridStatus: String,
    val lastUpdated: Long,
    val weatherCondition: String
)

data class PanelInformation(
    val manufacturer: String,
    val model: String,
    val wattage: Int,
    val count: Int,
    val technology: String,
    val efficiency: Double,
    val warranty: String,
    val orientation: String,
    val tiltAngle: String,
    val mountType: String
)

data class InverterInformation(
    val manufacturer: String,
    val model: String,
    val capacity: Double,
    val serialNumber: String,
    val firmwareVersion: String,
    val warranty: String,
    val status: String
)

data class InstallationInformation(
    val company: String,
    val engineerName: String,
    val installationDate: Long,
    val commissioningDate: Long,
    val gridSyncDate: Long,
    val surveyDate: Long,
    val completionDate: Long
)

data class RoofLayout(
    val roofOrientation: String,
    val areaUsed: Double,
    val areaAvailable: Double
)

data class WarrantySummary(
    val panelWarranty: String,
    val inverterWarranty: String,
    val installationWarranty: String,
    val amcStatus: String,
    val panelExpiryDate: Long,
    val inverterExpiryDate: Long,
    val installationExpiryDate: Long
)

data class SystemSpecification(
    val estimatedAnnualGeneration: Double,
    val performanceRatio: Double,
    val dcCapacity: Double,
    val acCapacity: Double,
    val expectedLifetime: Int,
    val systemVoltage: String,
    val netMeterNumber: String,
    val electricityBoard: String,
    val connectionType: String
)

data class SystemHealth(
    val overallHealth: String, // "Excellent", "Good", "Needs Attention"
    val healthPercentage: Int,
    val generationEfficiency: Double,
    val lastInspectionDate: Long,
    val dustCleaningStatus: String,
    val nextMaintenanceDate: Long
)

data class ServiceHistoryItem(
    val title: String,
    val date: Long,
    val description: String,
    val isCompleted: Boolean
)

data class Insight(
    val message: String,
    val type: String // "positive", "neutral", "warning"
)

data class DocumentShortcut(
    val title: String,
    val type: String // "proposal", "invoice", "warranty", "report", "approval"
)

data class SolarSystemDetails(
    val systemCapacity: Double,
    val systemType: String,
    val plantStatus: String, // "Online", "Offline", "Maintenance", "Fault"
    val installationDate: Long,
    val plantHealth: Int,
    val installationAddress: String,
    
    val liveStatus: LiveStatus?,
    val panelInfo: PanelInformation,
    val inverterInfo: InverterInformation,
    val installationInfo: InstallationInformation,
    val roofLayout: RoofLayout,
    val specifications: SystemSpecification,
    val warranty: WarrantySummary,
    val health: SystemHealth,
    val serviceHistory: List<ServiceHistoryItem>,
    val insights: List<Insight>,
    val documents: List<DocumentShortcut>
)
