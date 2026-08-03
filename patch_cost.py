import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/assessment/ui/CostEstimationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("fun CostEstimationRoute(\n    monthlyConsumption: Double,\n    availableArea: Double,\n    mountType: String,\n    systemType: String,\n    customerType: String,\n    onNavigateNext: (PaymentOptionResult) -> Unit,\n    viewModel: CostEstimationViewModel = viewModel()\n) {", """fun CostEstimationRoute(
    monthlyConsumption: Double,
    availableArea: Double,
    mountType: String,
    systemType: String,
    customerType: String,
    onNavigateNext: (PaymentOptionResult) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CostEstimationViewModel = viewModel()
) {""")

content = content.replace("""        Button(
            onClick = { uiState.result?.let { onNavigateNext(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text("Proceed to Payment Option", style = MaterialTheme.typography.titleMedium)
        }""", """        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .weight(0.35f)
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text("Back", style = MaterialTheme.typography.titleMedium)
            }
            
            Button(
                onClick = { uiState.result?.let { onNavigateNext(it) } },
                modifier = Modifier
                    .weight(0.65f)
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("Continue", style = MaterialTheme.typography.titleMedium)
            }
        }""")

with open(file_path, "w") as f:
    f.write(content)
