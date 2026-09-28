package com.example.feature.home.ui.modules

import kotlinx.serialization.Serializable

@Serializable
data class SlabBreakdown(
    val fromUnit: Double,
    val toUnit: Double,
    val unitsInSlab: Double,
    val ratePerUnit: Double,
    val amount: Double
)

@Serializable
data class BillResult(
    val totalUnits: Double,
    val freeUnits: Double,
    val chargeableUnits: Double,
    val totalEnergyCharge: Double,
    val breakdown: List<SlabBreakdown>
)

object TNEBBillCalculator {
    fun calculateTNEBBill(units: Double): BillResult {
        val freeUnits = if (units <= 500.0) 200.0 else 100.0
        val chargeableUnits = maxOf(0.0, units - freeUnits)

        val breakdown = mutableListOf<SlabBreakdown>()
        var remaining = chargeableUnits
        var totalEnergyCharge = 0.0

        if (units <= 500.0) {
            val slab1Cap = 200.0
            val slab1Units = minOf(remaining, slab1Cap)

            if (slab1Units > 0.0) {
                val fromUnit = freeUnits + 1
                val toUnit = freeUnits + slab1Units
                val amount = slab1Units * 4.70

                breakdown.add(
                    SlabBreakdown(
                        fromUnit = fromUnit,
                        toUnit = toUnit,
                        unitsInSlab = slab1Units,
                        ratePerUnit = 4.70,
                        amount = amount
                    )
                )

                totalEnergyCharge += amount
                remaining -= slab1Units
            }

            val slab2Cap = 100.0
            val slab2Units = minOf(remaining, slab2Cap)

            if (slab2Units > 0.0) {
                val fromUnit = freeUnits + slab1Cap + 1
                val toUnit = freeUnits + slab1Cap + slab2Units
                val amount = slab2Units * 6.30

                breakdown.add(
                    SlabBreakdown(
                        fromUnit = fromUnit,
                        toUnit = toUnit,
                        unitsInSlab = slab2Units,
                        ratePerUnit = 6.30,
                        amount = amount
                    )
                )

                totalEnergyCharge += amount
                remaining -= slab2Units
            }
        } else {
            val slabs = listOf(
                Pair(300.0, 4.70),
                Pair(100.0, 6.30),
                Pair(100.0, 8.40),
                Pair(200.0, 9.45),
                Pair(200.0, 10.50),
                Pair(Double.POSITIVE_INFINITY, 11.55)
            )

            var currentUnitStart = freeUnits + 1

            for ((cap, rate) in slabs) {
                if (remaining <= 0.0) {
                    break
                }

                val unitsInSlab = minOf(remaining, cap)
                val amount = unitsInSlab * rate

                val fromUnit = currentUnitStart
                val toUnit = currentUnitStart + unitsInSlab - 1

                breakdown.add(
                    SlabBreakdown(
                        fromUnit = fromUnit,
                        toUnit = toUnit,
                        unitsInSlab = unitsInSlab,
                        ratePerUnit = rate,
                        amount = amount
                    )
                )

                totalEnergyCharge += amount
                remaining -= unitsInSlab
                currentUnitStart += unitsInSlab
            }
        }

        return BillResult(
            totalUnits = units,
            freeUnits = freeUnits,
            chargeableUnits = chargeableUnits,
            totalEnergyCharge = totalEnergyCharge,
            breakdown = breakdown
        )
    }
}
