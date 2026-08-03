import sys

file_path = "/app/applet/app/src/main/java/com/example/navigation/NavGraph.kt"

with open(file_path, "r") as f:
    content = f.read()

new_content = content.replace(
    """                onNavigateNext = { navController.navigate(Screen.SiteSurveyStatus) }""",
    """                onNavigateNext = { navController.navigate(Screen.SiteSurveyStatus(systemType = args.systemType)) }"""
)

new_content = new_content.replace(
    """        composable<Screen.SiteSurveyStatus> {
            SiteSurveyStatusRoute(
                onNavigateNext = { navController.navigate(Screen.InstallationTracking) }
            )
        }
        
        composable<Screen.InstallationTracking> {""",
    """        composable<Screen.SiteSurveyStatus> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.SiteSurveyStatus>()
            SiteSurveyStatusRoute(
                onNavigateNext = { navController.navigate(Screen.InstallationTracking(systemType = args.systemType)) }
            )
        }
        
        composable<Screen.InstallationTracking> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.InstallationTracking>()"""
)

with open(file_path, "w") as f:
    f.write(new_content)

print("NavGraph.kt patched")
