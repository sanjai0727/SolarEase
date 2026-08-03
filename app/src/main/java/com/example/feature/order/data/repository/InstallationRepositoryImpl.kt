package com.example.feature.order.data.repository

import com.example.feature.order.domain.model.Milestone
import com.example.feature.order.domain.model.MilestoneStatus
import com.example.feature.order.domain.repository.InstallationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InstallationRepositoryImpl : InstallationRepository {
    private val _offGridMilestones = MutableStateFlow(
        listOf(
            Milestone(id = "1", key = "SITE_SURVEY_COMPLETED", title = "Site Survey Completed", description = "Site inspection completed and installation requirements verified.", status = MilestoneStatus.COMPLETED, completedAt = "11 Aug 2026, 02:00 PM"),
            Milestone(id = "2", key = "SITE_FEASIBILITY", title = "Site Feasibility & Load Audit", description = "Power requirements and battery sizing have been verified.", status = MilestoneStatus.COMPLETED, completedAt = "12 Aug 2026, 11:30 AM"),
            Milestone(id = "3", key = "MATERIALS_DISPATCHED", title = "Materials & Batteries Dispatched", description = "Solar panels, inverter, batteries, mounting structure and accessories have been dispatched.", status = MilestoneStatus.COMPLETED, completedAt = "14 Aug 2026, 09:00 AM"),
            Milestone(id = "4", key = "INSTALLATION_IN_PROGRESS", title = "Installation In Progress", description = "Installation team is currently working on-site.", status = MilestoneStatus.IN_PROGRESS, progress = 0, currentActivity = "Mounting Structure Installation", expectedCompletion = "Today, 5:30 PM"),
            Milestone(id = "5", key = "INSTALLATION_COMPLETED", title = "Installation Completed", description = "Mechanical and electrical installation has been completed successfully.", status = MilestoneStatus.PENDING),
            Milestone(id = "6", key = "TESTING_COMMISSIONING", title = "Testing & Battery Commissioning", description = "Battery charging, inverter configuration, electrical testing and system commissioning are currently being performed.", status = MilestoneStatus.PENDING),
            Milestone(id = "7", key = "BATTERY_VERIFICATION", title = "Battery Backup Verification", description = "Battery backup performance, charging, discharge and protection systems have been successfully verified.", status = MilestoneStatus.PENDING),
            Milestone(id = "8", key = "SYSTEM_LIVE", title = "System Live & Customer Handover", description = "The Off-Grid solar system has been successfully commissioned and handed over to the customer.", status = MilestoneStatus.PENDING),
            Milestone(id = "9", key = "PROJECT_COMPLETED", title = "Project Completed", description = "Installation completed successfully. The Off-Grid solar system is fully operational.", status = MilestoneStatus.PENDING)
        )
    )

    private val _hybridMilestones = MutableStateFlow(
        listOf(
            Milestone(id = "1", key = "SITE_SURVEY_COMPLETED", title = "Site Survey Completed", description = "Site inspection completed and installation requirements verified.", status = MilestoneStatus.COMPLETED, completedAt = "11 Aug 2026, 02:00 PM"),
            Milestone(id = "2", key = "FEASIBILITY_APPROVED", title = "Feasibility & Load Sanction Approved", description = "Technical feasibility verified and required approvals obtained.", status = MilestoneStatus.COMPLETED, completedAt = "12 Aug 2026, 11:30 AM"),
            Milestone(id = "3", key = "MATERIALS_DISPATCHED", title = "Materials & Batteries Dispatched", description = "Solar panels, inverter, batteries, mounting structure and accessories have been dispatched.", status = MilestoneStatus.COMPLETED, completedAt = "14 Aug 2026, 09:00 AM"),
            Milestone(id = "4", key = "INSTALLATION_IN_PROGRESS", title = "Installation In Progress", description = "Installation team is currently working on-site.", status = MilestoneStatus.IN_PROGRESS, progress = 0, currentActivity = "Mounting Structure Installation", expectedCompletion = "Today, 5:30 PM"),
            Milestone(id = "5", key = "INSTALLATION_COMPLETED", title = "Installation Completed", description = "Mechanical and electrical installation has been completed successfully.", status = MilestoneStatus.PENDING),
            Milestone(id = "6", key = "TESTING_COMMISSIONING", title = "Testing & Battery Commissioning", description = "Battery charging, inverter configuration, electrical testing and hybrid system commissioning are currently being performed.", status = MilestoneStatus.PENDING),
            Milestone(id = "7", key = "EB_INSPECTION", title = "EB / DISCOM Physical Inspection", description = "Electricity Board officials inspect the installation before approving grid connectivity.", status = MilestoneStatus.PENDING),
            Milestone(id = "8", key = "NET_METER_INSTALLED", title = "Net Meter Installed & Approved", description = "Net meter has been installed and approved by the Electricity Board.", status = MilestoneStatus.PENDING),
            Milestone(id = "9", key = "GRID_SYNC", title = "Grid Synchronization", description = "Hybrid solar system has been synchronized with the electricity grid.", status = MilestoneStatus.PENDING),
            Milestone(id = "10", key = "SYSTEM_LIVE", title = "System Live & Customer Handover", description = "Hybrid solar system is operational and handed over to the customer.", status = MilestoneStatus.PENDING),
            Milestone(id = "11", key = "PROJECT_COMPLETED", title = "Project Completed", description = "Installation completed successfully. The Hybrid solar system is fully operational.", status = MilestoneStatus.PENDING)
        )
    )

    private val _milestones = MutableStateFlow(
        listOf(
            Milestone(id = "1", key = "SITE_SURVEY_COMPLETED", title = "Site Survey Completed", description = "Site inspection completed and technical information collected.", status = MilestoneStatus.COMPLETED, completedAt = "11 Aug 2026, 02:00 PM"),
            Milestone(id = "2", key = "FEASIBILITY_APPROVED", title = "Feasibility & Load Sanction Approved", description = "Technical feasibility verified and required approvals obtained.", status = MilestoneStatus.COMPLETED, completedAt = "12 Aug 2026, 11:30 AM"),
            Milestone(id = "3", key = "MATERIALS_DISPATCHED", title = "Materials Dispatched", description = "Solar panels, inverter and installation materials have been dispatched.", status = MilestoneStatus.COMPLETED, completedAt = "14 Aug 2026, 09:00 AM"),
            Milestone(id = "4", key = "INSTALLATION_IN_PROGRESS", title = "Installation In Progress", description = "Installation team is currently working on-site.", status = MilestoneStatus.IN_PROGRESS, progress = 0, currentActivity = "Mounting Structure Installation", expectedCompletion = "Today, 5:30 PM"),
            Milestone(id = "5", key = "INSTALLATION_COMPLETED", title = "Installation Completed", description = "Mechanical and electrical installation has been completed successfully.", status = MilestoneStatus.PENDING),
            Milestone(id = "6", key = "TESTING_WCR", title = "Testing & Internal Commissioning (WCR)", description = "Electrical testing, inverter configuration and internal commissioning are in progress.", status = MilestoneStatus.PENDING),
            Milestone(id = "7", key = "EB_INSPECTION", title = "EB / DISCOM Physical Inspection", description = "Electricity Board officials inspect the installation before grid approval.", status = MilestoneStatus.PENDING),
            Milestone(id = "8", key = "NET_METER_INSTALLED", title = "Net Meter Installed & Approved", description = "Net meter has been installed and approved by the Electricity Board.", status = MilestoneStatus.PENDING),
            Milestone(id = "9", key = "GRID_SYNC", title = "Grid Synchronization", description = "Solar system has been synchronized with the electricity grid.", status = MilestoneStatus.PENDING),
            Milestone(id = "10", key = "SYSTEM_LIVE", title = "Project Completed & System Live", description = "Installation completed successfully. The solar system is now generating electricity and is fully operational.", status = MilestoneStatus.PENDING)
        )
    )

    override fun getOffGridMilestones(): Flow<List<Milestone>> {
        return _offGridMilestones.asStateFlow()
    }

    override fun getHybridMilestones(): Flow<List<Milestone>> {
        return _hybridMilestones.asStateFlow()
    }

    override fun getOnGridMilestones(): Flow<List<Milestone>> {
        return _milestones.asStateFlow()
    }

    override suspend fun updateMilestoneProgress(key: String, progress: Int, activity: String?, systemType: String) {
        var currentList = when {
            systemType.equals("OFF-GRID", true) -> _offGridMilestones.value.toMutableList()
            systemType.equals("HYBRID", true) -> _hybridMilestones.value.toMutableList()
            else -> _milestones.value.toMutableList()
        }
        val index = currentList.indexOfFirst { it.key == key }
        if (index != -1) {
            val milestone = currentList[index]
            currentList[index] = milestone.copy(
                progress = progress,
                currentActivity = activity ?: milestone.currentActivity
            )
            when {
                systemType.equals("OFF-GRID", true) -> _offGridMilestones.value = currentList
                systemType.equals("HYBRID", true) -> _hybridMilestones.value = currentList
                else -> _milestones.value = currentList
            }
        }
    }

    override suspend fun completeMilestone(key: String, systemType: String) {
        var currentList = when {
            systemType.equals("OFF-GRID", true) -> _offGridMilestones.value.toMutableList()
            systemType.equals("HYBRID", true) -> _hybridMilestones.value.toMutableList()
            else -> _milestones.value.toMutableList()
        }
        val index = currentList.indexOfFirst { it.key == key }
        if (index != -1) {
            val milestone = currentList[index]
            val timestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            currentList[index] = milestone.copy(
                status = MilestoneStatus.COMPLETED,
                progress = 100,
                completedAt = timestamp
            )
            
            if (index + 1 < currentList.size) {
                currentList[index + 1] = currentList[index + 1].copy(
                    status = MilestoneStatus.IN_PROGRESS
                )
            }
            when {
                systemType.equals("OFF-GRID", true) -> _offGridMilestones.value = currentList
                systemType.equals("HYBRID", true) -> _hybridMilestones.value = currentList
                else -> _milestones.value = currentList
            }
        }
    }
}
