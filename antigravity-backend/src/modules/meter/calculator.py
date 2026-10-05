from typing import List, Dict, Any

def calculate_tneb_bill(units: float) -> Dict[str, Any]:
    """
    Computes TNEB LT-1A Domestic tariff matching the SolarEase mobile application logic (TNEBBillCalculator.kt).
    Bi-monthly domestic consumption calculation.
    """
    units = max(0.0, float(units))
    free_units = 200.0 if units <= 500.0 else 100.0
    chargeable_units = max(0.0, units - free_units)

    breakdown: List[Dict[str, Any]] = []
    remaining = chargeable_units
    total_energy_charge = 0.0

    if units <= 500.0:
        slab1_cap = 200.0
        slab1_units = min(remaining, slab1_cap)
        if slab1_units > 0.0:
            from_unit = free_units + 1
            to_unit = free_units + slab1_units
            amount = slab1_units * 4.70
            breakdown.append({
                "from_unit": from_unit,
                "to_unit": to_unit,
                "units_in_slab": slab1_units,
                "rate_per_unit": 4.70,
                "amount": round(amount, 2)
            })
            total_energy_charge += amount
            remaining -= slab1_units

        slab2_cap = 100.0
        slab2_units = min(remaining, slab2_cap)
        if slab2_units > 0.0:
            from_unit = free_units + slab1_cap + 1
            to_unit = free_units + slab1_cap + slab2_units
            amount = slab2_units * 6.30
            breakdown.append({
                "from_unit": from_unit,
                "to_unit": to_unit,
                "units_in_slab": slab2_units,
                "rate_per_unit": 6.30,
                "amount": round(amount, 2)
            })
            total_energy_charge += amount
            remaining -= slab2_units
    else:
        slabs = [
            (300.0, 4.70),
            (100.0, 6.30),
            (100.0, 8.40),
            (200.0, 9.45),
            (200.0, 10.50),
            (float("inf"), 11.55),
        ]
        current_unit_start = free_units + 1
        for cap, rate in slabs:
            if remaining <= 0.0:
                break
            units_in_slab = min(remaining, cap)
            amount = units_in_slab * rate
            from_unit = current_unit_start
            to_unit = current_unit_start + units_in_slab - 1
            breakdown.append({
                "from_unit": from_unit,
                "to_unit": to_unit,
                "units_in_slab": units_in_slab,
                "rate_per_unit": rate,
                "amount": round(amount, 2)
            })
            total_energy_charge += amount
            remaining -= units_in_slab
            current_unit_start += units_in_slab

    total_energy_charge = round(total_energy_charge, 2)

    return {
        "units": units,
        "free_units": free_units,
        "chargeable_units": chargeable_units,
        "energy_charges": total_energy_charge,
        "fixed_charges": 0.0,
        "electricity_tax": 0.0,
        "total_estimated_bill": total_energy_charge,
        "breakdown": breakdown
    }
