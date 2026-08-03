import sys

file_path = "/app/applet/app/src/main/java/com/example/navigation/NavGraph.kt"
with open(file_path, "r") as f:
    content = f.read()

replacement = """                onNavigateToProfile = {
                    navController.navigate(Screen.AuthProfile)
                },"""

old = """                onNavigateToProfile = {
                    navController.navigate(Screen.AuthProfile) {
                        popUpTo(Screen.AuthPhone) { inclusive = true }
                    }
                },"""

content = content.replace(old, replacement)

with open(file_path, "w") as f:
    f.write(content)
