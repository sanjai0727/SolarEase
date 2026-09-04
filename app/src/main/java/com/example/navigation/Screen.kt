package com.example.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable
    data object AuthGraph : Screen()

    @Serializable
    data object AuthMain : Screen()

    @Serializable
    data object AuthPhone : Screen()
    
    @Serializable
    data class AuthOtp(val fullName: String, val phoneNumber: String) : Screen()
    

    
    // New Routes
    @Serializable
    data object Home : Screen()
    
    @Serializable
    data object BillCalculator : Screen()
    
    @Serializable
    data object ElectricityChatbot : Screen()
    
    @Serializable
    data object SolarEaseLens : Screen()
    
    @Serializable
    data object LensMain : Screen()
    
    @Serializable
    data object LensCamera : Screen()

    @Serializable
    data object LensDetails : Screen()

    @Serializable
    data object LensReview : Screen()

    @Serializable
    data object LensResult : Screen()
    
    @Serializable
    data object TnebBillStatus : Screen()
    
    @Serializable
    data object SolarPanel : Screen()

    @Serializable
    data object CustomerDashboard : Screen()
    
    @Serializable
    data object MySolarSystem : Screen()
    
    @Serializable
    data object Performance : Screen()
    
    @Serializable
    data object Financial : Screen()
    
    @Serializable
    data object Environmental : Screen()

    @Serializable
    data object Profile : Screen()
}
