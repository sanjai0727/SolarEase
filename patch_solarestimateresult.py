import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/assessment/util/SolarEstimateResult.kt"

with open(file_path, "r") as f:
    content = f.read()

replacement = """    val systemType: String,
    val mountType: String,
    val panelType: String,
    val customerType: String,"""

old = """    val systemType: String,
    val mountType: String,
    val customerType: String,"""

content = content.replace(old, replacement)

with open(file_path, "w") as f:
    f.write(content)
