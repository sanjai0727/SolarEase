import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/assessment/ui/CostEstimationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("""fun CostEstimationRoute(
    monthlyConsumption: Double,
    availableArea: Double,
    mountType: String,
    systemType: String,
    customerType: String,
    onNavigateNext: (com.example.feature.assessment.util.SolarEstimateResult) -> Unit,
    viewModel: EstimateViewModel = viewModel()
) {""", """fun CostEstimationRoute(
    monthlyConsumption: Double,
    availableArea: Double,
    mountType: String,
    systemType: String,
    customerType: String,
    onNavigateNext: (com.example.feature.assessment.util.SolarEstimateResult) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EstimateViewModel = viewModel()
) {""")

with open(file_path, "w") as f:
    f.write(content)
