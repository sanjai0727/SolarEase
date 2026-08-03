import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/assessment/ui/CostEstimationScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

content = content.replace("""    mountType: String,
    systemType: String,
    customerType: String,""", """    mountType: String,
    systemType: String,
    panelType: String,
    customerType: String,""")

content = content.replace("""            mountType = mountType,
            systemType = systemType,
            customerType = customerType
        )
    }""", """            mountType = mountType,
            systemType = systemType,
            panelType = panelType,
            customerType = customerType
        )
    }""")

with open(file_path, "w") as f:
    f.write(content)
