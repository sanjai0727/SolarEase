package com.example.feature.order.domain.model

enum class MilestoneStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}

data class Milestone(
    val id: String,
    val key: String,
    val title: String,
    val description: String,
    val status: MilestoneStatus,
    val progress: Int? = null,
    val completedAt: String? = null,
    val expectedCompletion: String? = null,
    val currentActivity: String? = null
)
