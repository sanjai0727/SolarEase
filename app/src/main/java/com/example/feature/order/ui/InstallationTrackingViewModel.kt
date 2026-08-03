package com.example.feature.order.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.order.data.repository.InstallationRepositoryImpl
import com.example.feature.order.domain.model.Milestone
import com.example.feature.order.domain.model.MilestoneStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InstallationTrackingViewModel : ViewModel() {
    private val repository = InstallationRepositoryImpl()
    
    private val _milestones = MutableStateFlow<List<Milestone>>(emptyList())
    val milestones: StateFlow<List<Milestone>> = _milestones.asStateFlow()
    
    private var systemTypeStr = "ON-GRID"
    private var isOffGrid = false
    private var isHybrid = false
    private var workflowInitialized = false

    fun initWorkflow(systemType: String) {
        if (workflowInitialized) return
        workflowInitialized = true
        systemTypeStr = systemType
        isOffGrid = systemType.equals("OFF-GRID", ignoreCase = true)
        isHybrid = systemType.equals("HYBRID", ignoreCase = true)
        
        viewModelScope.launch {
            val flow = when {
                isOffGrid -> repository.getOffGridMilestones()
                isHybrid -> repository.getHybridMilestones()
                else -> repository.getOnGridMilestones()
            }
            
            flow.collect { list ->
                _milestones.value = list
            }
        }
        
        simulateProgress()
    }
    
    private fun simulateProgress() {
        viewModelScope.launch {
            delay(1500)
            
            val activities = if (isHybrid) {
                listOf(
                    "Mounting Structure Installation",
                    "Solar Panel Installation",
                    "Battery Installation",
                    "Hybrid Inverter Installation",
                    "Electrical Wiring"
                )
            } else if (isOffGrid) {
                listOf(
                    "Mounting Structure Installation",
                    "Solar Panel Installation",
                    "Battery Installation",
                    "Inverter Installation",
                    "Electrical Wiring"
                )
            } else {
                listOf(
                    "Mounting Structure Installation",
                    "Solar Panel Installation",
                    "Inverter Installation",
                    "Electrical Wiring"
                )
            }
            
            var currentProgress = 0
            var activityIndex = 0
            
            while (currentProgress < 100) {
                delay(1000)
                currentProgress += 30
                if (currentProgress > 100) {
                    currentProgress = 100
                }
                
                activityIndex = (currentProgress / 20).coerceIn(0, activities.size - 1)
                val currentActivity = activities[activityIndex]
                
                if (currentProgress < 100) {
                    repository.updateMilestoneProgress("INSTALLATION_IN_PROGRESS", currentProgress, currentActivity, systemTypeStr)
                } else {
                    repository.completeMilestone("INSTALLATION_IN_PROGRESS", systemTypeStr)
                    break
                }
            }
            
            val remainingKeys = when {
                isHybrid -> listOf(
                    "INSTALLATION_COMPLETED",
                    "TESTING_COMMISSIONING",
                    "EB_INSPECTION",
                    "NET_METER_INSTALLED",
                    "GRID_SYNC",
                    "SYSTEM_LIVE",
                    "PROJECT_COMPLETED"
                )
                isOffGrid -> listOf(
                    "INSTALLATION_COMPLETED",
                    "TESTING_COMMISSIONING",
                    "BATTERY_VERIFICATION",
                    "SYSTEM_LIVE",
                    "PROJECT_COMPLETED"
                )
                else -> listOf(
                    "INSTALLATION_COMPLETED",
                    "TESTING_WCR",
                    "EB_INSPECTION",
                    "NET_METER_INSTALLED",
                    "GRID_SYNC",
                    "SYSTEM_LIVE"
                )
            }
            
            for (key in remainingKeys) {
                delay(1500)
                repository.completeMilestone(key, systemTypeStr)
            }
        }
    }
}
