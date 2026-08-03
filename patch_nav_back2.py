import sys

file_path = "/app/applet/app/src/main/java/com/example/navigation/NavGraph.kt"
with open(file_path, "r") as f:
    content = f.read()

# Replace for PanelSelectionRoute
content = content.replace("""                    ))
                }
            )
        }
        
        composable<Screen.CostEstimation>""", """                    ))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<Screen.CostEstimation>""")

# Replace for CostEstimationRoute
content = content.replace("""                        estimatedPaybackYears = result.estimatedPaybackYears
                    ))
                }
            )""", """                        estimatedPaybackYears = result.estimatedPaybackYears
                    ))
                },
                onNavigateBack = { navController.popBackStack() }
            )""")

with open(file_path, "w") as f:
    f.write(content)
