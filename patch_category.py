import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/assessment/ui/CustomerCategoryScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("fun CustomerCategoryRoute(\n    onNavigateNext: (String) -> Unit\n) {", """fun CustomerCategoryRoute(
    onNavigateNext: (String) -> Unit,
    onNavigateBack: () -> Unit
) {""")

content = content.replace("""        Button(
            onClick = { selectedCategory?.let { onNavigateNext(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            enabled = selectedCategory != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
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
                onClick = { selectedCategory?.let { onNavigateNext(it) } },
                modifier = Modifier
                    .weight(0.65f)
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = selectedCategory != null,
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
