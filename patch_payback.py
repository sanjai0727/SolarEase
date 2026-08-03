import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/assessment/ui/CostEstimationScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

replacement = """            if (result.annualSavings > 0) {
                Text(text = "Estimated Payback: ~${result.estimatedPaybackYears} Years", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(text = "Estimated Payback: N/A", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }"""

old = """            if (result.estimatedPaybackYears > 0) {
                Text(text = "Estimated Payback: ~${result.estimatedPaybackYears} Years", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }"""

content = content.replace(old, replacement)

with open(file_path, "w") as f:
    f.write(content)
