# Solar Lens Appliance Default Data

> Source: `Appliance-DefaultWattageW-TypicalUsageHoursDay-Mon.csv`
>
> Use this data as the current source of truth for appliance defaults and estimated monthly calculations.

## Appliance Default Data

| Appliance | Default Wattage (W) | Typical Usage Hours/Day | Monthly Usage Days | Monthly kWh (est.) | Monthly Units | Est. Monthly Cost @ ₹6/unit |
|---|---:|---|---:|---:|---:|---:|
| LED Tube Light | 36 | 6 | 30 | 6.48 | 6.48 | ₹39 |
| Standard Bulb (LED 9W) | 9 | 6 | 30 | 1.62 | 1.62 | ₹10 |
| Ceiling Fan | 75 | 12 | 30 | 27.00 | 27.00 | ₹162 |
| Table Fan | 50 | 8 | 30 | 12.00 | 12.00 | ₹72 |
| Television (LED 43") | 100 | 5 | 30 | 15.00 | 15.00 | ₹90 |
| Refrigerator (single-door) | 150 | 24* | 30 | 108.00 | 108.00 | ₹648 |
| Washing Machine | 500 | 1 | 12 | 6.00 | 6.00 | ₹36 |
| Air Conditioner (1.5T, non-inverter) | 1500 | 6 | 30 | 270.00 | 270.00 | ₹1,620 |
| Microwave | 1000 | 0.25 (15 min) | 15 | 3.75 | 3.75 | ₹23 |
| Water Heater / Geyser (15–25L) | 2000 | 1 | 20 | 40.00 | 40.00 | ₹240 |
| Laptop | 60 | 6 | 30 | 10.80 | 10.80 | ₹65 |
| Air Cooler | 200 | 8 | 30 | 48.00 | 48.00 | ₹288 |
| Induction Cooktop | 1800 | 1 | 20 | 36.00 | 36.00 | ₹216 |
| Iron Box | 1000 | 0.5 | 12 | 6.00 | 6.00 | ₹36 |
| Mixer Grinder | 600 | 0.25 (15 min) | 15 | 2.25 | 2.25 | ₹14 |
| Electric Kettle | 1500 | 0.1 (6 min) | 15 | 2.25 | 2.25 | ₹14 |
| Water Pump (0.5–1 HP) | 500 | 1 | 15 | 7.50 | 7.50 | ₹45 |
| Printer | 50 | 0.5 | 10 | 0.25 | 0.25 | ₹2 |
| Dishwasher | 1500 | 1 | 10 | 15.00 | 15.00 | ₹90 |

## Calculation Formula

```text
Monthly kWh = (Wattage × Typical Usage Hours/Day × Monthly Usage Days × Quantity) / 1000
```

```text
Estimated Monthly Cost = Monthly kWh × ₹6
```

## Implementation Notes

- The first four values for each appliance should be treated as editable user inputs:
  - Appliance
  - Wattage
  - Typical Usage Hours/Day
  - Monthly Usage Days
- Quantity is handled separately by the user.
- AI detection should only provide the initial appliance suggestion.
- The final user-confirmed values should be used as the source of truth for analytics.
- `LED Tube Light` and `Standard Bulb (LED 9W)` are separate appliance types.
- The `24*` value for the refrigerator is preserved exactly from the source CSV.
