package com.example.core.network.dto

data class ChatQueryRequest(
    val query: String,
    val mode: String,
    val language: String
)

data class ChatResponse(
    val answer: String,
    val source_question_id: String?
)
