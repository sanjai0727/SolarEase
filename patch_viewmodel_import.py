import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/assessment/ui/SiteDetailsViewModel.kt"

with open(file_path, "r") as f:
    content = f.read()

content = content.replace("import androidx.lifecycle.SavedStateHandle\n\nclass SiteDetailsViewModel", "class SiteDetailsViewModel")
content = content.replace("import androidx.lifecycle.ViewModel", "import androidx.lifecycle.ViewModel\nimport androidx.lifecycle.SavedStateHandle")

with open(file_path, "w") as f:
    f.write(content)
