import sys

file_path = "/app/applet/app/src/main/java/com/example/MainActivity.kt"

with open(file_path, "r") as f:
    content = f.read()

new_content = content.replace(
    "SolarEaseNavGraph(navController = navController)",
    "val startDest = if (com.example.core.session.UserSession.currentUser?.onboardingCompleted == true) com.example.navigation.Screen.CustomerDashboard else com.example.navigation.Screen.AuthPhone\n            SolarEaseNavGraph(navController = navController, startDestination = startDest)"
)

with open(file_path, "w") as f:
    f.write(new_content)

print("MainActivity patched")
