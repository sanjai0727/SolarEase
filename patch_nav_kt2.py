import sys

file_path = "/app/applet/app/src/main/java/com/example/navigation/NavGraph.kt"

with open(file_path, "r") as f:
    content = f.read()

new_content = content.replace(
    """        composable<Screen.InstallationTracking> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.InstallationTracking>()
            InstallationTrackingRoute(
                onNavigateNext = {""",
    """        composable<Screen.InstallationTracking> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.InstallationTracking>()
            InstallationTrackingRoute(
                systemType = args.systemType,
                onNavigateNext = {"""
)

with open(file_path, "w") as f:
    f.write(new_content)

print("NavGraph.kt patched again")
