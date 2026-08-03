import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/assessment/ui/PanelSelectionScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

if "import androidx.compose.runtime.saveable.rememberSaveable" not in content:
    content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.compose.runtime.saveable.rememberSaveable")

content = content.replace("by remember {", "by rememberSaveable {")

with open(file_path, "w") as f:
    f.write(content)
