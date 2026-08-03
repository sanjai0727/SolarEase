package com.example.feature.dashboard.domain.model

data class ActivityItem(
    val title: String,
    val timestamp: Long
)

data class NotificationItem(
    val message: String,
    val timestamp: Long
)

data class CustomerDashboard(
    val customerName: String,
    val systemStatus: String, // "Online", "Offline", "Maintenance", "Fault"
    val capacity: Double,
    val systemType: String,
    val installationDate: String,
    val todayGeneration: Double,
    val todaySavings: Double,
    val todayCo2: Double,
    val lifetimeGeneration: Double,
    val lifetimeSavings: Double,
    val lifetimeCo2: Double,
    val plantHealth: Int,
    val recentActivities: List<ActivityItem>,
    val notifications: List<NotificationItem>
)
