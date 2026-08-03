import sys

file_path = "/app/applet/app/src/main/java/com/example/navigation/NavGraph.kt"
with open(file_path, "r") as f:
    content = f.read()

replacement = """    NavHost(
        navController = navController,
        startDestination = Screen.AuthPhone
    ) {"""

old = """    NavHost(
        navController = navController,
        startDestination = if (com.example.core.session.UserSession.currentUser?.registrationCompleted == true) {
            Screen.CustomerCategory
        } else {
            Screen.AuthPhone
        }
    ) {"""

content = content.replace(old, replacement)

with open(file_path, "w") as f:
    f.write(content)
