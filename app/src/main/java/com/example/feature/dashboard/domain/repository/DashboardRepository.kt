package com.example.feature.dashboard.domain.repository

import com.example.feature.dashboard.domain.model.CustomerDashboard
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getDashboardData(): Flow<Result<CustomerDashboard>>
}
