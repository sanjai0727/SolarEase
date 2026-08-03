import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/assessment/ui/AssessmentInputScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("fun AssessmentInputRoute(\n    customerType: String,\n    systemType: String,\n    onNavigateNext: (RecommendationResult) -> Unit,\n    viewModel: SiteDetailsViewModel = viewModel()\n) {", """fun AssessmentInputRoute(
    customerType: String,
    systemType: String,
    onNavigateNext: (RecommendationResult) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SiteDetailsViewModel = viewModel()
) {""")

content = content.replace("""        Button(
            onClick = {
                viewModel.calculateEstimate(customerType, systemType)?.let { result ->
                    onNavigateNext(result)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            enabled = uiState.isFormValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text("Calculate Estimate", style = MaterialTheme.typography.titleMedium)
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
                onClick = {
                    viewModel.calculateEstimate(customerType, systemType)?.let { result ->
                        onNavigateNext(result)
                    }
                },
                modifier = Modifier
                    .weight(0.65f)
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = uiState.isFormValid,
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
