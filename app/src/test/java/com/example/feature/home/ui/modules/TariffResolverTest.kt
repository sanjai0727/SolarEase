package com.example.feature.home.ui.modules

import org.junit.Assert.assertEquals
import org.junit.Test

class TariffResolverTest {

    @Test
    fun testResolvesLegacyTnebDomesticFormat() {
        val resolved = TariffResolver.resolve("LA1A / DOMESTIC")
        assertEquals("LT-IA", resolved.tariffCode)
        assertEquals("Domestic", resolved.tariffName)
        assertEquals("LT-IA · Domestic", resolved.displayLabel)
    }

    @Test
    fun testResolvesVariationsOfDomestic() {
        val inputs = listOf(
            "LA1A",
            "LA-1A",
            "LT1A",
            "LT-1A",
            "LT-IA",
            "01 - DOMESTIC",
            "01-DOMESTIC",
            "TANGEDCO LT-1A · DOMESTIC",
            "LT-1A / DOMESTIC",
            "LA1A / Domestic"
        )

        for (input in inputs) {
            val resolved = TariffResolver.resolve(input)
            assertEquals("Failed for $input", "LT-IA", resolved.tariffCode)
            assertEquals("Failed for $input", "Domestic", resolved.tariffName)
            assertEquals("Failed for $input", "LT-IA · Domestic", resolved.displayLabel)
        }
    }

    @Test
    fun testResolvesBlankAndNull() {
        val nullResolved = TariffResolver.resolve(null)
        assertEquals("LT-IA", nullResolved.tariffCode)
        assertEquals("Domestic", nullResolved.tariffName)
        assertEquals("LT-IA · Domestic", nullResolved.displayLabel)

        val emptyResolved = TariffResolver.resolve("")
        assertEquals("LT-IA", emptyResolved.tariffCode)
        assertEquals("Domestic", emptyResolved.tariffName)
        assertEquals("LT-IA · Domestic", emptyResolved.displayLabel)
    }

    @Test
    fun testResolvesFutureTariffCategories() {
        // Commercial
        val commercial = TariffResolver.resolve("LA2A / COMMERCIAL")
        assertEquals("LT-IIA", commercial.tariffCode)
        assertEquals("Commercial", commercial.tariffName)
        assertEquals("LT-IIA · Commercial", commercial.displayLabel)

        // Industrial
        val industrial = TariffResolver.resolve("LA3A / INDUSTRIAL")
        assertEquals("LT-IIIA", industrial.tariffCode)
        assertEquals("Industrial", industrial.tariffName)
        assertEquals("LT-IIIA · Industrial", industrial.displayLabel)

        // Agriculture
        val agri = TariffResolver.resolve("LA4 / AGRICULTURE")
        assertEquals("LT-IV", agri.tariffCode)
        assertEquals("Agriculture", agri.tariffName)
        assertEquals("LT-IV · Agriculture", agri.displayLabel)
    }

    @Test
    fun testConsumerBillDataNormalizedTariffIntegration() {
        val billData = TnebConsumerBillData(
            consumerNumber = "092410051234",
            consumerName = "SANJAI K",
            tariffCode = "LA1A / DOMESTIC",
            serviceCategory = "DOMESTIC"
        )
        val normalized = billData.normalizedTariff
        assertEquals("LT-IA", normalized.tariffCode)
        assertEquals("Domestic", normalized.tariffName)
        assertEquals("LT-IA · Domestic", normalized.displayLabel)
    }

    @Test
    fun testTnebBillCalculationUnchanged() {
        // Confirm bill calculation is completely independent of tariff display normalization
        val bill160 = TNEBBillCalculator.calculateTNEBBill(160.0)
        assertEquals(0.0, bill160.totalEnergyCharge, 0.001)

        val bill300 = TNEBBillCalculator.calculateTNEBBill(300.0)
        // <= 500 units: 200 free units. Chargeable units = 100 @ 4.70 = 470.0
        assertEquals(470.0, bill300.totalEnergyCharge, 0.001)
    }
}
