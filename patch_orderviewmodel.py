import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/order/ui/OrderConfirmationViewModel.kt"

with open(file_path, "r") as f:
    content = f.read()

state_old = """    val surveyDate: String = "",
    val customerName: String = com.example.core.session.UserSession.currentUser?.customerName ?: "Valued Customer",
    val installationAddress: String = "123 Solar Way, Green City"
)"""

state_new = """    val surveyDate: String = "",
    val customerName: String = com.example.core.session.UserSession.currentUser?.customerName ?: "Valued Customer",
    val mobileNumber: String = com.example.core.session.UserSession.currentUser?.mobileNumber ?: "N/A",
    val installationAddress: String = "123 Solar Way, Green City",
    val panelType: String = ""
)"""

content = content.replace(state_old, state_new)

func_old = """    fun setOrderDetails(
        recommendedCapacityKW: Double,
        systemType: String,
        mountType: String,
        paymentMethod: String,
        grossTotal: Double,
        subsidyAmount: Double,
        netPayable: Double
    ) {"""

func_new = """    fun setOrderDetails(
        recommendedCapacityKW: Double,
        systemType: String,
        mountType: String,
        panelType: String,
        paymentMethod: String,
        grossTotal: Double,
        subsidyAmount: Double,
        netPayable: Double
    ) {"""

content = content.replace(func_old, func_new)

update_old = """                recommendedCapacityKW = recommendedCapacityKW,
                systemType = systemType,
                mountType = mountType,
                paymentMethod = paymentMethod,"""

update_new = """                recommendedCapacityKW = recommendedCapacityKW,
                systemType = systemType,
                mountType = mountType,
                panelType = panelType,
                paymentMethod = paymentMethod,"""

content = content.replace(update_old, update_new)

with open(file_path, "w") as f:
    f.write(content)
