package com.example.feature.environmental.domain.repository

import com.example.feature.environmental.domain.model.EnvironmentalDashboard
import kotlinx.coroutines.flow.Flow

interface EnvironmentalRepository {
    fun getEnvironmentalDashboard(): Flow<Result<EnvironmentalDashboard>>
}
