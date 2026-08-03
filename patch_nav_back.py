import sys

file_path = "/app/applet/app/src/main/java/com/example/navigation/NavGraph.kt"
with open(file_path, "r") as f:
    content = f.read()

# Replace for CustomerCategoryRoute
content = content.replace("""            CustomerCategoryRoute(
                onNavigateNext = { customerType -> navController.navigate(Screen.SystemType(customerType)) }
            )""", """            CustomerCategoryRoute(
                onNavigateNext = { customerType -> navController.navigate(Screen.SystemType(customerType)) },
                onNavigateBack = { navController.popBackStack() }
            )""")

# Replace for SystemTypeRoute
content = content.replace("""            SystemTypeRoute(
                onNavigateNext = { systemType -> navController.navigate(Screen.AssessmentInput(args.customerType, systemType)) }
            )""", """            SystemTypeRoute(
                onNavigateNext = { systemType -> navController.navigate(Screen.AssessmentInput(args.customerType, systemType)) },
                onNavigateBack = { navController.popBackStack() }
            )""")

# Replace for AssessmentInputRoute
content = content.replace("""                    ))
                }
            )""", """                    ))
                },
                onNavigateBack = { navController.popBackStack() }
            )""")

# Replace for RecommendationRoute
content = content.replace("""                    ))
                }
            )""", """                    ))
                },
                onNavigateBack = { navController.popBackStack() }
            )""")
# Wait, this might replace multiple if they match. AssessmentInputRoute and RecommendationRoute both end similarly. Let's do it safer.

with open(file_path, "w") as f:
    f.write(content)
