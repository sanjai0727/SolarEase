package com.example.feature.lens

import com.example.feature.lens.utils.ApplianceDef
import com.example.feature.lens.utils.ApplianceMaster
import com.example.feature.lens.utils.UsageMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ApplianceCalculatorTest {

    @Test
    fun laptop_uses_average_operating_wattage() {
        val laptop = ApplianceDef(
            name = "Laptop",
            ratedWattage = 60f,
            activeOperatingWattage = 30f,
            usageMode = UsageMode.DAILY_HOURS,
            typicalDailyHours = 6f,
            monthlyDays = 30,
            dutyCycle = 1.0f
        )

        val result = ApplianceMaster.calculateApplianceResult(laptop, quantity = 1, tariffPerKwh = 7.0)

        assertEquals(0.06, result.connectedLoadKw, 0.0001)
        assertEquals(5.4, result.monthlyEnergyKwh, 0.0001)
        assertEquals(37.8, result.simplifiedBaselineCost, 0.0001)
    }

    @Test
    fun refrigerator_applies_duty_cycle() {
        val refrigerator = ApplianceDef(
            name = "Refrigerator",
            ratedWattage = 150f,
            activeOperatingWattage = 150f,
            usageMode = UsageMode.CONTINUOUS_THERMOSTATIC,
            typicalDailyHours = 24f,
            monthlyDays = 30,
            dutyCycle = 0.30f
        )

        val result = ApplianceMaster.calculateApplianceResult(refrigerator, quantity = 1, tariffPerKwh = 7.0)

        assertEquals(0.15, result.connectedLoadKw, 0.0001)
        assertEquals(32.4, result.monthlyEnergyKwh, 0.0001)
        assertEquals(226.8, result.simplifiedBaselineCost, 0.0001)
    }

    @Test
    fun washing_machine_cycle_calculation() {
        val washingMachine = ApplianceDef(
            name = "Washing Machine",
            ratedWattage = 500f,
            activeOperatingWattage = 500f,
            usageMode = UsageMode.CYCLES_PER_MONTH,
            typicalDailyHours = 1f,
            minutesPerUse = 60f,
            usesPerMonth = 12,
            monthlyDays = 12,
            dutyCycle = 1.0f
        )

        val result = ApplianceMaster.calculateApplianceResult(washingMachine, quantity = 1, tariffPerKwh = 7.0)

        assertEquals(0.50, result.connectedLoadKw, 0.0001)
        assertEquals(6.0, result.monthlyEnergyKwh, 0.0001)
        assertEquals(42.0, result.simplifiedBaselineCost, 0.0001)
    }

    @Test
    fun microwave_minutes_per_use_calculation() {
        val microwave = ApplianceDef(
            name = "Microwave",
            ratedWattage = 1000f,
            activeOperatingWattage = 1000f,
            usageMode = UsageMode.MINUTES_PER_USE,
            typicalDailyHours = 0.25f,
            minutesPerUse = 15f,
            usesPerMonth = 15,
            monthlyDays = 15,
            dutyCycle = 1.0f
        )

        val result = ApplianceMaster.calculateApplianceResult(microwave, quantity = 1, tariffPerKwh = 7.0)

        // 1000W * (15/60 hours) * 15 uses / 1000 = 3.75 kWh
        assertEquals(1.0, result.connectedLoadKw, 0.0001)
        assertEquals(3.75, result.monthlyEnergyKwh, 0.0001)
        assertEquals(26.25, result.simplifiedBaselineCost, 0.0001)
    }

    @Test
    fun quantity_greater_than_one_scales_linearly() {
        val laptop = ApplianceDef(
            name = "Laptop",
            ratedWattage = 60f,
            activeOperatingWattage = 30f,
            usageMode = UsageMode.DAILY_HOURS,
            typicalDailyHours = 6f,
            monthlyDays = 30,
            dutyCycle = 1.0f
        )

        val result = ApplianceMaster.calculateApplianceResult(laptop, quantity = 2, tariffPerKwh = 7.0)

        assertEquals(0.12, result.connectedLoadKw, 0.0001)
        assertEquals(10.8, result.monthlyEnergyKwh, 0.0001)
        assertEquals(75.6, result.simplifiedBaselineCost, 0.0001)
    }

    @Test
    fun zero_quantity_yields_zero_metrics() {
        val laptop = ApplianceDef(
            name = "Laptop",
            ratedWattage = 60f,
            activeOperatingWattage = 30f,
            typicalDailyHours = 6f,
            monthlyDays = 30
        )

        val result = ApplianceMaster.calculateApplianceResult(laptop, quantity = 0)

        assertEquals(0.0, result.connectedLoadKw, 0.0001)
        assertEquals(0.0, result.monthlyEnergyKwh, 0.0001)
        assertEquals(0.0, result.simplifiedBaselineCost, 0.0001)
    }

    @Test
    fun duty_cycle_zero_yields_zero_energy() {
        val ac = ApplianceDef(
            name = "Air Conditioner",
            ratedWattage = 1500f,
            activeOperatingWattage = 1500f,
            typicalDailyHours = 6f,
            monthlyDays = 30,
            dutyCycle = 0.0f
        )

        val result = ApplianceMaster.calculateApplianceResult(ac, quantity = 1)

        assertEquals(1.5, result.connectedLoadKw, 0.0001)
        assertEquals(0.0, result.monthlyEnergyKwh, 0.0001)
    }

    @Test
    fun duty_cycle_one_yields_continuous_energy() {
        val fan = ApplianceDef(
            name = "Ceiling Fan",
            ratedWattage = 75f,
            activeOperatingWattage = 75f,
            typicalDailyHours = 10f,
            monthlyDays = 30,
            dutyCycle = 1.0f
        )

        val result = ApplianceMaster.calculateApplianceResult(fan, quantity = 1)

        // 75 * 10 * 30 / 1000 = 22.5 kWh
        assertEquals(0.075, result.connectedLoadKw, 0.0001)
        assertEquals(22.5, result.monthlyEnergyKwh, 0.0001)
    }

    @Test
    fun negative_wattage_throws_exception() {
        assertThrows(IllegalArgumentException::class.java) {
            ApplianceDef(
                name = "Invalid",
                ratedWattage = -10f,
                activeOperatingWattage = 50f
            )
        }
    }

    @Test
    fun negative_active_wattage_throws_exception() {
        assertThrows(IllegalArgumentException::class.java) {
            ApplianceDef(
                name = "Invalid",
                ratedWattage = 50f,
                activeOperatingWattage = -5f
            )
        }
    }

    @Test
    fun duty_cycle_greater_than_one_throws_exception() {
        assertThrows(IllegalArgumentException::class.java) {
            ApplianceDef(
                name = "Invalid",
                ratedWattage = 50f,
                activeOperatingWattage = 50f,
                dutyCycle = 1.2f
            )
        }
    }

    @Test
    fun monthly_days_out_of_range_throws_exception() {
        assertThrows(IllegalArgumentException::class.java) {
            ApplianceDef(
                name = "Invalid",
                ratedWattage = 50f,
                activeOperatingWattage = 50f,
                monthlyDays = 35
            )
        }
    }

    @Test
    fun calculateMonthlyEnergyKwh_validates_inputs() {
        assertThrows(IllegalArgumentException::class.java) {
            ApplianceMaster.calculateMonthlyEnergyKwh(
                activeOperatingWattage = -10f,
                dailyHours = 5.0,
                monthlyDays = 30,
                quantity = 1
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            ApplianceMaster.calculateMonthlyEnergyKwh(
                activeOperatingWattage = 50f,
                dailyHours = 5.0,
                monthlyDays = 30,
                quantity = -1
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            ApplianceMaster.calculateMonthlyEnergyKwh(
                activeOperatingWattage = 50f,
                dailyHours = 5.0,
                monthlyDays = 30,
                quantity = 1,
                dutyCycle = 1.5f
            )
        }
    }

    @Test
    fun custom_appliance_calculation_and_validation() {
        val customAppliance = ApplianceDef(
            name = "Custom 3D Printer",
            ratedWattage = 250f,
            activeOperatingWattage = 120f,
            usageMode = UsageMode.DAILY_HOURS,
            typicalDailyHours = 4f,
            monthlyDays = 20,
            dutyCycle = 1.0f
        )

        val result = ApplianceMaster.calculateApplianceResult(customAppliance, quantity = 1, tariffPerKwh = 7.0)
        assertEquals(0.25, result.connectedLoadKw, 0.0001)
        // 120W * 4h * 20d * 1.0 / 1000 = 9.6 kWh
        assertEquals(9.6, result.monthlyEnergyKwh, 0.0001)
        assertEquals(67.2, result.simplifiedBaselineCost, 0.0001)
    }

    @Test
    fun room_aggregation_calculates_total_load_and_energy() {
        val bedroomAppliances = listOf(
            ApplianceMaster.getDefaults("Ceiling Fan")!! to 1,
            ApplianceMaster.getDefaults("LED Tube Light")!! to 2,
            ApplianceMaster.getDefaults("Laptop")!! to 1
        )

        // Ceiling Fan: 75W rated, 75W active, 12h, 30d, 1.0 duty -> 0.075 kW, 27.0 kWh
        // LED Tube Light (x2): 36W rated, 36W active, 6h, 30d, 1.0 duty -> 0.072 kW, 12.96 kWh
        // Laptop: 60W rated, 30W active, 6h, 30d, 1.0 duty -> 0.060 kW, 5.4 kWh

        val totalLoadKw = bedroomAppliances.sumOf { (appliance, qty) ->
            ApplianceMaster.calculateConnectedLoadKw(appliance, qty)
        }
        val totalMonthlyKwh = bedroomAppliances.sumOf { (appliance, qty) ->
            ApplianceMaster.calculateMonthlyEnergyKwh(appliance, qty)
        }

        assertEquals(0.075 + 0.072 + 0.060, totalLoadKw, 0.0001)
        assertEquals(27.0 + 12.96 + 5.4, totalMonthlyKwh, 0.0001)
    }

    @Test
    fun empty_room_aggregates_to_zero() {
        val emptyAppliances = emptyList<Pair<ApplianceDef, Int>>()

        val totalLoadKw = emptyAppliances.sumOf { (appliance, qty) ->
            ApplianceMaster.calculateConnectedLoadKw(appliance, qty)
        }
        val totalMonthlyKwh = emptyAppliances.sumOf { (appliance, qty) ->
            ApplianceMaster.calculateMonthlyEnergyKwh(appliance, qty)
        }

        assertEquals(0.0, totalLoadKw, 0.0001)
        assertEquals(0.0, totalMonthlyKwh, 0.0001)
    }

    @Test
    fun multiple_rooms_aggregate_connected_load_and_energy() {
        val bedroomAppliances = listOf(
            ApplianceMaster.getDefaults("Laptop")!! to 1 // 0.06 kW, 5.4 kWh
        )
        val kitchenAppliances = listOf(
            ApplianceMaster.getDefaults("Refrigerator (Single Door)")!! to 1 // 0.15 kW, 32.4 kWh
        )

        val allRooms = listOf(bedroomAppliances, kitchenAppliances)

        val wholeHouseConnectedLoadKw = allRooms.sumOf { room ->
            room.sumOf { (appliance, qty) -> ApplianceMaster.calculateConnectedLoadKw(appliance, qty) }
        }
        val wholeHouseMonthlyEnergyKwh = allRooms.sumOf { room ->
            room.sumOf { (appliance, qty) -> ApplianceMaster.calculateMonthlyEnergyKwh(appliance, qty) }
        }

        assertEquals(0.21, wholeHouseConnectedLoadKw, 0.0001)
        assertEquals(37.8, wholeHouseMonthlyEnergyKwh, 0.0001)
    }

    @Test
    fun intermediate_calculations_preserve_double_precision_rounding_only_at_ui_display() {
        val appliance = ApplianceDef(
            name = "Small Device",
            ratedWattage = 7.5f,
            activeOperatingWattage = 3.333f,
            usageMode = UsageMode.DAILY_HOURS,
            typicalDailyHours = 3.5f,
            monthlyDays = 30,
            dutyCycle = 0.77f
        )

        val unroundedMonthlyEnergy = ApplianceMaster.calculateMonthlyEnergyKwh(appliance, quantity = 3)
        // 3.333 * 3.5 * 30 * 3 * 0.77 / 1000.0 = 0.8085189
        val uiFormattedString = String.format(java.util.Locale.US, "%.1f", unroundedMonthlyEnergy)

        assertEquals(0.8085189, unroundedMonthlyEnergy, 0.001)
        assertEquals("0.8", uiFormattedString)
    }
}
