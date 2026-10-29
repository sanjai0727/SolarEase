package com.example.feature.home.ui.modules

import org.junit.Assert.assertEquals
import org.junit.Test

class TNEBBillCalculatorTest {

    @Test
    fun testZeroUnits() {
        val result = TNEBBillCalculator.calculateTNEBBill(0.0)
        assertEquals(200.0, result.freeUnits, 0.001)
        assertEquals(0.0, result.chargeableUnits, 0.001)
        assertEquals(0.0, result.totalEnergyCharge, 0.001)
        assertEquals(0, result.breakdown.size)
    }

    @Test
    fun testExample976Units() {
        val result = TNEBBillCalculator.calculateTNEBBill(976.0)
        assertEquals(100.0, result.freeUnits, 0.001)
        assertEquals(876.0, result.chargeableUnits, 0.001)
        assertEquals(6618.0, result.totalEnergyCharge, 0.001)
        assertEquals(5, result.breakdown.size)

        // 101-400 (300 units @ 4.70)
        assertEquals(300.0, result.breakdown[0].unitsInSlab, 0.001)
        assertEquals(4.70, result.breakdown[0].ratePerUnit, 0.001)
        assertEquals(1410.0, result.breakdown[0].amount, 0.001)

        // 401-500 (100 units @ 6.30)
        assertEquals(100.0, result.breakdown[1].unitsInSlab, 0.001)
        assertEquals(6.30, result.breakdown[1].ratePerUnit, 0.001)
        assertEquals(630.0, result.breakdown[1].amount, 0.001)

        // 501-600 (100 units @ 8.40)
        assertEquals(100.0, result.breakdown[2].unitsInSlab, 0.001)
        assertEquals(8.40, result.breakdown[2].ratePerUnit, 0.001)
        assertEquals(840.0, result.breakdown[2].amount, 0.001)

        // 601-800 (200 units @ 9.45)
        assertEquals(200.0, result.breakdown[3].unitsInSlab, 0.001)
        assertEquals(9.45, result.breakdown[3].ratePerUnit, 0.001)
        assertEquals(1890.0, result.breakdown[3].amount, 0.001)

        // 801-976 (176 units @ 10.50)
        assertEquals(176.0, result.breakdown[4].unitsInSlab, 0.001)
        assertEquals(10.50, result.breakdown[4].ratePerUnit, 0.001)
        assertEquals(1848.0, result.breakdown[4].amount, 0.001)
    }

    @Test
    fun testAllRequiredInputs() {
        val testInputs = listOf(
            0.0, 1.0, 100.0, 199.5, 200.0, 200.5, 250.0,
            400.0, 400.5, 500.0, 500.5, 600.0, 800.0,
            800.5, 976.0, 976.5, 1000.0, 1000.5, 1200.0
        )

        for (input in testInputs) {
            val result = TNEBBillCalculator.calculateTNEBBill(input)
            
            // Basic sanity checks for all inputs
            assertEquals(input, result.totalUnits, 0.001)
            
            if (input <= 500.0) {
                assertEquals(200.0, result.freeUnits, 0.001)
            } else {
                assertEquals(100.0, result.freeUnits, 0.001)
            }

            val expectedChargeable = maxOf(0.0, input - result.freeUnits)
            assertEquals(expectedChargeable, result.chargeableUnits, 0.001)

            val sumBreakdown = result.breakdown.sumOf { it.amount }
            assertEquals(result.totalEnergyCharge, sumBreakdown, 0.001)
        }
    }
}

