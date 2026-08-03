package com.example.feature.order.domain.model

data class InstallationStage(
    val title: String,
    val description: String,
    val status: String,
    val timestamp: Long,
    val remarks: String,
    val trackingId: String?,
    val estimatedCompletion: Long
)
