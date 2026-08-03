import sys

file_path = "/app/applet/app/src/main/java/com/example/navigation/NavGraph.kt"
with open(file_path, "r") as f:
    content = f.read()

replacement = """        composable<Screen.EbServiceNumber> {
            EbServiceNumberRoute(
                onNavigateNext = { 
                    navController.navigate(Screen.CustomerCategory) {
                        popUpTo(0)
                    }
                }
            )
        }"""

content = content.replace("""        composable<Screen.EbServiceNumber> {
            EbServiceNumberRoute(
                onNavigateNext = { navController.navigate(Screen.CustomerCategory) }
            )
        }""", replacement)

with open(file_path, "w") as f:
    f.write(content)
