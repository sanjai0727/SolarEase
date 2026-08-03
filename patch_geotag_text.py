import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/assessment/ui/AssessmentInputScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

replacement = """        // Geotag Site Photo Status
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = !uiState.isPhotoCaptured) { viewModel.updatePhotoCaptured(true) },
            color = if (uiState.isPhotoCaptured) successColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (uiState.isPhotoCaptured) {
                    Text(
                        text = "✅ Geotag Site Photo Captured",
                        color = successColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture Photo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Geotag Site Photo Required",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }"""

old = """        // Geotag Site Photo Status
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = !uiState.isPhotoCaptured) { viewModel.updatePhotoCaptured(true) },
            color = if (uiState.isPhotoCaptured) successColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.isPhotoCaptured) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                        contentDescription = if (uiState.isPhotoCaptured) "Photo Captured" else "Capture Photo",
                        tint = if (uiState.isPhotoCaptured) successColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (uiState.isPhotoCaptured) "Geotag Site Photo Captured" else "Geotag Site Photo Required",
                        color = if (uiState.isPhotoCaptured) successColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (uiState.isPhotoCaptured) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }"""

content = content.replace(old, replacement)

with open(file_path, "w") as f:
    f.write(content)
