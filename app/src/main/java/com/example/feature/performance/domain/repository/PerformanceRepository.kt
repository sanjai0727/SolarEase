package com.example.feature.performance.domain.repository

import com.example.feature.performance.domain.model.PerformanceDashboard
import kotlinx.coroutines.flow.Flow

interface PerformanceRepository {
    fun getPerformanceDashboard(): Flow<Result<PerformanceDashboard>>
}
