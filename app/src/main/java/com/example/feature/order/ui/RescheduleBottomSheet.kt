package com.example.feature.order.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestDatePicker() {
    val datePickerState = rememberDatePickerState()
    DatePicker(state = datePickerState)
}
