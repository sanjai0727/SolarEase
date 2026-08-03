import sys

files = [
    "/app/applet/app/src/main/java/com/example/feature/assessment/ui/CustomerCategoryScreen.kt",
    "/app/applet/app/src/main/java/com/example/feature/assessment/ui/SystemTypeScreen.kt"
]

for file_path in files:
    with open(file_path, "r") as f:
        content = f.read()

    # Add import if missing
    if "import androidx.compose.runtime.saveable.rememberSaveable" not in content:
        content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.compose.runtime.saveable.rememberSaveable")

    content = content.replace("by remember {", "by rememberSaveable {")

    with open(file_path, "w") as f:
        f.write(content)
