import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/auth/ui/AuthProfileScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

replacement = """            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅ Geotag Site Photo Captured", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    // Future implementation: Add "View Photo" and "Retake Photo" actions here
                }
            }"""

old = """            } else {
                Text("Photo Captured ✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }"""

content = content.replace(old, replacement)

with open(file_path, "w") as f:
    f.write(content)
