package com.example.feature.order.domain.model

data class Order(
    val orderId: String,
    val paymentId: String,
    val paymentMethod: String,
    val paymentStatus: String,
    val orderStatus: String,
    val surveyStatus: String,
    val installationStatus: String,
    val createdAt: Long,
    val updatedAt: Long
)
