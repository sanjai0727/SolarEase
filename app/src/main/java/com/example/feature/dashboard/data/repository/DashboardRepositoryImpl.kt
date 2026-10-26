package com.example.feature.dashboard.data.repository

import com.example.feature.dashboard.domain.model.ActivityItem
import com.example.feature.dashboard.domain.model.CustomerDashboard
import com.example.feature.dashboard.domain.model.NotificationItem
import com.example.feature.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DashboardRepositoryImpl : DashboardRepository {
    override fun getDashboardData(): Flow<Result<CustomerDashboard>> = flow {
        // Simulate network delay
        delay(1500)
        
        val mockData = CustomerDashboard(
            username = com.example.core.session.UserSession.currentUser?.username ?: "Valued Customer",
            systemStatus = "Online",
            capacity = 2.5,
            systemType = "On Grid",
            installationDate = "24 July 2026",
            todayGeneration = 18.5,
            todaySavings = 142.0,
            todayCo2 = 6.4,
            lifetimeGeneration = 12450.0,
            lifetimeSavings = 124600.0,
            lifetimeCo2 = 8500.0,
            plantHealth = 98,
            recentActivities = listOf(
                ActivityItem("Installation Completed", System.currentTimeMillis() - 86400000 * 2),
                ActivityItem("Grid Sync Successful", System.currentTimeMillis() - 86400000 * 1),
                ActivityItem("Warranty Activated", System.currentTimeMillis())
            ).sortedByDescending { it.timestamp },
            notifications = listOf(
                NotificationItem("Solar generated 18.5 kWh today.", System.currentTimeMillis() - 3600000),
                NotificationItem("AMC reminder due next month.", System.currentTimeMillis() - 86400000)
            )
        )
        
        emit(Result.success(mockData))
    }
}
