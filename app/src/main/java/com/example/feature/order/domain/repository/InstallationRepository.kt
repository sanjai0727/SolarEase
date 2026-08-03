package com.example.feature.order.domain.repository

import com.example.feature.order.domain.model.Milestone
import kotlinx.coroutines.flow.Flow

interface InstallationRepository {
    fun getOnGridMilestones(): Flow<List<Milestone>>
    fun getOffGridMilestones(): Flow<List<Milestone>>
    fun getHybridMilestones(): Flow<List<Milestone>>
    suspend fun updateMilestoneProgress(key: String, progress: Int, activity: String?, systemType: String = "ON-GRID")
    suspend fun completeMilestone(key: String, systemType: String = "ON-GRID")
}
