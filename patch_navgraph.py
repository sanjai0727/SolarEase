import sys

file_path = "/app/applet/app/src/main/java/com/example/navigation/NavGraph.kt"

with open(file_path, "r") as f:
    content = f.read()

# For PanelSelection
panel_selection_nav = """                        mountType = args.mountType,
                        systemType = args.systemType,
                        panelType = panelType,
                        customerType = args.customerType
                    ))"""

content = content.replace("""                        mountType = args.mountType,
                        systemType = args.systemType,
                        customerType = args.customerType
                    ))""", panel_selection_nav)

content = content.replace("""            PanelSelectionRoute(
                onNavigateNext = { 
                    navController.navigate(Screen.CostEstimation(""", """            PanelSelectionRoute(
                onNavigateNext = { panelType ->
                    navController.navigate(Screen.CostEstimation(""")


# For CostEstimation
cost_est = """            CostEstimationRoute(
                monthlyConsumption = args.monthlyConsumption,
                availableArea = args.availableArea,
                mountType = args.mountType,
                systemType = args.systemType,
                panelType = args.panelType,
                customerType = args.customerType,"""

content = content.replace("""            CostEstimationRoute(
                monthlyConsumption = args.monthlyConsumption,
                availableArea = args.availableArea,
                mountType = args.mountType,
                systemType = args.systemType,
                customerType = args.customerType,""", cost_est)

with open(file_path, "w") as f:
    f.write(content)
