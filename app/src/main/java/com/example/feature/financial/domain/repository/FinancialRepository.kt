package com.example.feature.financial.domain.repository

import com.example.feature.financial.domain.model.FinancialDashboard
import kotlinx.coroutines.flow.Flow

interface FinancialRepository {
    fun getFinancialDashboard(): Flow<Result<FinancialDashboard>>
}
