package com.example.feature.systemdetails.domain.repository

import com.example.feature.systemdetails.domain.model.SolarSystemDetails
import kotlinx.coroutines.flow.Flow

interface SystemDetailsRepository {
    fun getSystemDetails(): Flow<Result<SolarSystemDetails>>
}
