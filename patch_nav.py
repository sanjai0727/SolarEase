import sys

file_path = "/app/applet/app/src/main/java/com/example/navigation/NavGraph.kt"

with open(file_path, "r") as f:
    content = f.read()

new_content = content.replace(
    "@Composable\nfun SolarEaseNavGraph(navController: NavHostController) {\n    NavHost(\n        navController = navController,\n        startDestination = Screen.AuthPhone\n    ) {",
    "@Composable\nfun SolarEaseNavGraph(navController: NavHostController, startDestination: Any = Screen.AuthPhone) {\n    NavHost(\n        navController = navController,\n        startDestination = startDestination\n    ) {"
)

with open(file_path, "w") as f:
    f.write(new_content)

print("NavGraph patched")
