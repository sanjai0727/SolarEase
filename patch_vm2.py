import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/order/ui/InstallationTrackingViewModel.kt"

with open(file_path, "r") as f:
    content = f.read()

new_content = """package com.example.feature.order.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.order.data.repository.InstallationRepositoryImpl
import com.example.feature.order.domain.model.Milestone
import com.example.feature.order.domain.model.MilestoneStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InstallationTrackingViewModel : ViewModel() {
    private val repository = InstallationRepositoryImpl()
    
    val milestones: StateFlow<List<Milestone>> = repository.getOnGridMilestones()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        simulateProgress()
    }

    private fun simulateProgress() {
        viewModelScope.launch {
            delay(1500)
            
            val activities = listOf(
                "Mounting Structure Installation",
                "Solar Panel Installation",
                "Inverter Installation",
                "Electrical Wiring"
            )
            
            var currentProgress = 0
            var activityIndex = 0
            
            while (currentProgress < 100) {
                delay(1000)
                currentProgress += 30
                if (currentProgress > 100) {
                    currentProgress = 100
                }
                
                activityIndex = (currentProgress / 25).coerceIn(0, activities.size - 1)
                val currentActivity = activities[activityIndex]
                
                if (currentProgress < 100) {
                    repository.updateMilestoneProgress("INSTALLATION_IN_PROGRESS", currentProgress, currentActivity)
                } else {
                    repository.completeMilestone("INSTALLATION_IN_PROGRESS")
                    break
                }
            }
            
            // Simulate the remaining steps
            val remainingKeys = listOf(
                "INSTALLATION_COMPLETED",
                "TESTING_WCR",
                "EB_INSPECTION",
                "NET_METER_INSTALLED",
                "GRID_SYNC",
                "SYSTEM_LIVE"
            )
            
            for (key in remainingKeys) {
                delay(1500)
                repository.completeMilestone(key)
            }
        }
    }
}
"""

with open(file_path, "w") as f:
    f.write(new_content)

print("ViewModel patched to simulate all")
