import sys

file_path = "/app/applet/app/src/main/java/com/example/navigation/NavGraph.kt"

with open(file_path, "r") as f:
    content = f.read()

# Add panelType to CostEstimation onNavigateNext 
cost_est_nav = """                        mountType = result.mountType,
                        panelType = result.panelType,
                        customerType = result.customerType,"""

content = content.replace("""                        mountType = result.mountType,
                        customerType = result.customerType,""", cost_est_nav)

# Add panelType to SolarEstimateResult instantiation
payment_opt = """                mountType = args.mountType,
                panelType = args.panelType,
                customerType = args.customerType,"""

content = content.replace("""                mountType = args.mountType,
                customerType = args.customerType,""", payment_opt)

with open(file_path, "w") as f:
    f.write(content)
