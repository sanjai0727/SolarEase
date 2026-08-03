import sys

file_path = "/app/applet/app/src/main/java/com/example/navigation/NavGraph.kt"
with open(file_path, "r") as f:
    content = f.read()

replacement = """        composable<Screen.EbServiceNumber> {
            EbServiceNumberRoute(
                onNavigateNext = { navController.navigate(Screen.RegistrationSuccess) }
            )
        }
        
        composable<Screen.RegistrationSuccess> {
            com.example.feature.auth.ui.RegistrationSuccessRoute(
                onNavigateNext = { 
                    navController.navigate(Screen.CustomerCategory) {
                        popUpTo(Screen.RegistrationSuccess) { inclusive = true }
                    } 
                }
            )
        }"""

content = content.replace("""        composable<Screen.EbServiceNumber> {
            EbServiceNumberRoute(
                onNavigateNext = { navController.navigate(Screen.RegistrationSuccess) }
            )
        }""", replacement)

with open(file_path, "w") as f:
    f.write(content)
