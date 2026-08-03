import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/assessment/ui/RecommendationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("fun RecommendationRoute(\n    result: RecommendationResult,\n    onNavigateNext: () -> Unit\n) {", """fun RecommendationRoute(
    result: RecommendationResult,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {""")

content = content.replace("""        Button(
            onClick = onNavigateNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            enabled = result.isFeasible,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text("Select Panels", style = MaterialTheme.typography.titleMedium)
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
                onClick = onNavigateNext,
                modifier = Modifier
                    .weight(0.65f)
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = result.isFeasible,
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
