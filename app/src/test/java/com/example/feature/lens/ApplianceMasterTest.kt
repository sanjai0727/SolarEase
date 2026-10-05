package com.example.feature.lens

import com.example.feature.lens.utils.ApplianceMaster
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplianceMasterTest {

    @Test
    fun `standard appliances list contains required core appliances`() {
        val names = ApplianceMaster.appliances.map { it.name }
        assertTrue("Should contain Ceiling Fan", names.contains("Ceiling Fan"))
        assertTrue("Should contain Air Conditioner (1.5T)", names.contains("Air Conditioner (1.5T)"))
        assertTrue("Should contain Refrigerator (Single Door)", names.contains("Refrigerator (Single Door)"))
        assertTrue("Should contain Washing Machine", names.contains("Washing Machine"))
        assertTrue("Should contain Television (LED 43\")", names.contains("Television (LED 43\")"))
        assertTrue("Should contain Other / Custom Appliance", names.contains("Other / Custom Appliance"))
    }

    @Test
    fun `getDefaults returns exact match when name matches standard appliance`() {
        val fan = ApplianceMaster.getDefaults("Ceiling Fan")
        assertNotNull(fan)
        assertEquals(65f, fan?.defaultWattage)
        assertEquals(12f, fan?.typicalUsageHoursPerDay)
        assertEquals(30, fan?.monthlyUsageDays)
    }

    @Test
    fun `getDefaults maps partial AI labels to standard appliance`() {
        // e.g. Gemini detects "ceiling fan" or "refrigerator"
        val fan = ApplianceMaster.getDefaults("three-blade ceiling fan")
        assertNotNull(fan)
        assertEquals("Ceiling Fan", fan?.name)

        val fridge = ApplianceMaster.getDefaults("refrigerator")
        assertNotNull(fridge)
        assertEquals("Refrigerator (Single Door)", fridge?.name)
    }

    @Test
    fun `getDefaultWattage returns fallback wattage for unknown name`() {
        val wattage = ApplianceMaster.getDefaultWattage("Totally Unknown Device")
        assertEquals(100f, wattage)
    }

    @Test
    fun `custom appliance wattage is preserved and not overwritten`() {
        val customApplianceDef = ApplianceMaster.getDefaults("Other / Custom Appliance")
        assertNotNull(customApplianceDef)
        assertEquals(0f, customApplianceDef?.defaultWattage)
    }

    @Test
    fun `calculateMonthlyKwh calculates correct energy consumption`() {
        // Ceiling Fan: 75W, 12 hrs/day, 30 days, qty 1
        // (75 * 12 * 30 * 1) / 1000 = 27.0 kWh
        val kwh = ApplianceMaster.calculateMonthlyKwh(
            powerWatts = 75f,
            dailyUsageHours = 12f,
            monthlyUsageDays = 30,
            quantity = 1
        )
        assertEquals(27.0f, kwh, 0.001f)

        // Multiple appliances: qty 2 -> 54.0 kWh
        val kwh2 = ApplianceMaster.calculateMonthlyKwh(
            powerWatts = 75f,
            dailyUsageHours = 12f,
            monthlyUsageDays = 30,
            quantity = 2
        )
        assertEquals(54.0f, kwh2, 0.001f)
    }

    @Test
    fun `calculateMonthlyCost calculates correct cost using standard tariff`() {
        // 27 kWh * ₹7.0 = ₹189.0
        val cost = ApplianceMaster.calculateMonthlyCost(27.0f)
        assertEquals(189.0f, cost, 0.01f)
    }
}
