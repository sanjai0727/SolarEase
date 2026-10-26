from typing import List, Dict, Any
from decimal import Decimal, ROUND_HALF_UP

TARIFF_VERSION_INFO = {
    "authority": "TNERC (Tamil Nadu Electricity Regulatory Commission)",
    "order_ref": "Tariff Order No. 5 of 2024",
    "effective_date": "2024-07-01",
    "version": "2024-2025.1",
}

def round_currency(val: Decimal) -> float:
    return float(val.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP))

def calculate_tneb_bill(units: float, tariff_code: str = "LT-1A") -> Dict[str, Any]:
    """
    Computes TNEB bi-monthly electricity bill using Decimal-safe math.
    Supports LT-1A Domestic and general commercial/non-domestic tariffs.
    """
    units_dec = max(Decimal("0.0"), Decimal(str(round(units, 2))))
    
    if tariff_code and "LT-2" in tariff_code.upper():
        # Commercial / Non-domestic tariff: No free units, flat slab rates
        energy_charges = units_dec * Decimal("8.50")
        total = round_currency(energy_charges)
        return {
            "units": float(units_dec),
            "free_units": 0.0,
            "chargeable_units": float(units_dec),
            "energy_charges": total,
            "fixed_charges": 70.0,
            "electricity_tax": 0.0,
            "total_estimated_bill": round_currency(Decimal(str(total)) + Decimal("70.0")),
            "breakdown": [{
                "from_unit": 1,
                "to_unit": float(units_dec),
                "units_in_slab": float(units_dec),
                "rate_per_unit": 8.50,
                "amount": total
            }],
            "tariff_code": tariff_code,
            "tariff_metadata": TARIFF_VERSION_INFO
        }

    # LT-1A Domestic calculation
    free_units = Decimal("200.0") if units_dec <= Decimal("500.0") else Decimal("100.0")
    chargeable_units = max(Decimal("0.0"), units_dec - free_units)

    breakdown: List[Dict[str, Any]] = []
    remaining = chargeable_units
    total_energy_charge = Decimal("0.0")

    if units_dec <= Decimal("500.0"):
        slab1_cap = Decimal("200.0")
        slab1_units = min(remaining, slab1_cap)
        if slab1_units > Decimal("0.0"):
            rate = Decimal("4.70")
            amount = slab1_units * rate
            breakdown.append({
                "from_unit": float(free_units + Decimal("1")),
                "to_unit": float(free_units + slab1_units),
                "units_in_slab": float(slab1_units),
                "rate_per_unit": float(rate),
                "amount": round_currency(amount)
            })
            total_energy_charge += amount
            remaining -= slab1_units

        slab2_cap = Decimal("100.0")
        slab2_units = min(remaining, slab2_cap)
        if slab2_units > Decimal("0.0"):
            rate = Decimal("6.30")
            amount = slab2_units * rate
            breakdown.append({
                "from_unit": float(free_units + slab1_cap + Decimal("1")),
                "to_unit": float(free_units + slab1_cap + slab2_units),
                "units_in_slab": float(slab2_units),
                "rate_per_unit": float(rate),
                "amount": round_currency(amount)
            })
            total_energy_charge += amount
            remaining -= slab2_units
    else:
        slabs = [
            (Decimal("300.0"), Decimal("4.70")),
            (Decimal("100.0"), Decimal("6.30")),
            (Decimal("100.0"), Decimal("8.40")),
            (Decimal("200.0"), Decimal("9.45")),
            (Decimal("200.0"), Decimal("10.50")),
            (Decimal("999999999.0"), Decimal("11.55")),
        ]
        current_unit_start = free_units + Decimal("1")
        for cap, rate in slabs:
            if remaining <= Decimal("0.0"):
                break
            units_in_slab = min(remaining, cap)
            amount = units_in_slab * rate
            breakdown.append({
                "from_unit": float(current_unit_start),
                "to_unit": float(current_unit_start + units_in_slab - Decimal("1")),
                "units_in_slab": float(units_in_slab),
                "rate_per_unit": float(rate),
                "amount": round_currency(amount)
            })
            total_energy_charge += amount
            remaining -= units_in_slab
            current_unit_start += units_in_slab

    final_energy = round_currency(total_energy_charge)

    return {
        "units": float(units_dec),
        "free_units": float(free_units),
        "chargeable_units": float(chargeable_units),
        "energy_charges": final_energy,
        "fixed_charges": 0.0,
        "electricity_tax": 0.0,
        "total_estimated_bill": final_energy,
        "breakdown": breakdown,
        "tariff_code": tariff_code,
        "tariff_metadata": TARIFF_VERSION_INFO
    }
