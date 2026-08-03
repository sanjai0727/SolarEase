import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/assessment/ui/RecommendationScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

replacement = """        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Review Your Recommendation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Please review your recommended solar system.\\n\\nIf you would like to change your monthly consumption, available area, or other site details, tap Back.\\n\\nIf everything looks correct, tap Continue to proceed to Panel Selection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))"""

if "Spacer(modifier = Modifier.weight(1f))" in content:
    content = content.replace("Spacer(modifier = Modifier.weight(1f))", replacement, 1)

with open(file_path, "w") as f:
    f.write(content)
