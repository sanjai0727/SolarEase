import sys

file_path = "/app/applet/app/src/main/java/com/example/navigation/NavGraph.kt"

with open(file_path, "r") as f:
    content = f.read()

instantiation_new = """                    navController.navigate(Screen.OrderSummary(
                        recommendedCapacityKW = estimateResult.recommendedCapacityKW,
                        systemType = estimateResult.systemType,
                        mountType = estimateResult.mountType,
                        panelType = estimateResult.panelType,
                        paymentMethod = paymentMethod,
                        grossTotal = estimateResult.grossTotal,
                        subsidyAmount = estimateResult.subsidyAmount,
                        netPayable = estimateResult.netPayable
                    ))"""

instantiation_old = """                    navController.navigate(Screen.OrderSummary(
                        recommendedCapacityKW = estimateResult.recommendedCapacityKW,
                        systemType = estimateResult.systemType,
                        mountType = estimateResult.mountType,
                        paymentMethod = paymentMethod,
                        grossTotal = estimateResult.grossTotal,
                        subsidyAmount = estimateResult.subsidyAmount,
                        netPayable = estimateResult.netPayable
                    ))"""

content = content.replace(instantiation_old, instantiation_new)

route_new = """        composable<Screen.OrderSummary> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.OrderSummary>()
            OrderSummaryRoute(
                recommendedCapacityKW = args.recommendedCapacityKW,
                systemType = args.systemType,
                mountType = args.mountType,
                panelType = args.panelType,
                paymentMethod = args.paymentMethod,
                grossTotal = args.grossTotal,
                subsidyAmount = args.subsidyAmount,
                netPayable = args.netPayable,
                onNavigateNext = { navController.navigate(Screen.SiteSurveyStatus) }
            )
        }"""

route_old = """        composable<Screen.OrderSummary> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.OrderSummary>()
            OrderSummaryRoute(
                recommendedCapacityKW = args.recommendedCapacityKW,
                systemType = args.systemType,
                mountType = args.mountType,
                paymentMethod = args.paymentMethod,
                grossTotal = args.grossTotal,
                subsidyAmount = args.subsidyAmount,
                netPayable = args.netPayable,
                onNavigateNext = { navController.navigate(Screen.SiteSurveyStatus) }
            )
        }"""

content = content.replace(route_old, route_new)

with open(file_path, "w") as f:
    f.write(content)
