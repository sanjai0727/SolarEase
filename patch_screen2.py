import sys

file_path = "/app/applet/app/src/main/java/com/example/navigation/Screen.kt"

with open(file_path, "r") as f:
    content = f.read()

replacement = """    @Serializable
    data class OrderSummary(
        val recommendedCapacityKW: Double,
        val systemType: String,
        val mountType: String,
        val panelType: String,
        val paymentMethod: String,
        val grossTotal: Double,
        val subsidyAmount: Double,
        val netPayable: Double
    ) : Screen()"""

old = """    @Serializable
    data class OrderSummary(
        val recommendedCapacityKW: Double,
        val systemType: String,
        val mountType: String,
        val paymentMethod: String,
        val grossTotal: Double,
        val subsidyAmount: Double,
        val netPayable: Double
    ) : Screen()"""

content = content.replace(old, replacement)

with open(file_path, "w") as f:
    f.write(content)
