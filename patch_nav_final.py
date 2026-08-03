import sys

file_path = "/app/applet/app/src/main/java/com/example/navigation/NavGraph.kt"
with open(file_path, "r") as f:
    content = f.read()

replacement = """        composable<Screen.AuthProfile> {
            AuthProfileRoute(
                onRegistrationComplete = {
                    navController.navigate(Screen.EbServiceNumber)
                }
            )
        }"""

content = content.replace("""        composable<Screen.AuthProfile> {
            AuthProfileRoute(
                onRegistrationComplete = {
                    navController.navigate(Screen.EbServiceNumber) {
                        popUpTo(0)
                    }
                }
            )
        }""", replacement)

with open(file_path, "w") as f:
    f.write(content)
