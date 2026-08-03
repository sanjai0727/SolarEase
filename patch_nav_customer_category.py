import sys

file_path = "/app/applet/app/src/main/java/com/example/navigation/NavGraph.kt"

with open(file_path, "r") as f:
    content = f.read()

replacement = """        composable<Screen.CustomerCategory> {
            CustomerCategoryRoute(
                onNavigateNext = { customerType -> navController.navigate(Screen.SystemType(customerType)) }
            )
        }"""

old = """        composable<Screen.CustomerCategory> {
            CustomerCategoryRoute(
                onNavigateNext = { customerType -> navController.navigate(Screen.SystemType(customerType)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }"""

content = content.replace(old, replacement)

with open(file_path, "w") as f:
    f.write(content)
