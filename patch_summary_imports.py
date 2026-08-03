import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/order/ui/OrderSummaryScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

content = content.replace("import java.util.Locale", "import java.util.Locale\\nimport androidx.compose.ui.platform.LocalContext\\nimport androidx.compose.ui.window.Dialog\\nimport androidx.compose.ui.window.DialogProperties\\nimport com.example.feature.order.util.ReceiptPdfGenerator")

route_old = """@Composable
fun OrderSummaryRoute(
    recommendedCapacityKW: Double,
    systemType: String,
    mountType: String,
    paymentMethod: String,
    grossTotal: Double,
    subsidyAmount: Double,
    netPayable: Double,
    onNavigateNext: () -> Unit,"""

route_new = """@Composable
fun OrderSummaryRoute(
    recommendedCapacityKW: Double,
    systemType: String,
    mountType: String,
    panelType: String,
    paymentMethod: String,
    grossTotal: Double,
    subsidyAmount: Double,
    netPayable: Double,
    onNavigateNext: () -> Unit,"""

content = content.replace(route_old, route_new)

call_old = """    LaunchedEffect(Unit) {
        viewModel.setOrderDetails(
            recommendedCapacityKW = recommendedCapacityKW,
            systemType = systemType,
            mountType = mountType,
            paymentMethod = paymentMethod,"""

call_new = """    LaunchedEffect(Unit) {
        viewModel.setOrderDetails(
            recommendedCapacityKW = recommendedCapacityKW,
            systemType = systemType,
            mountType = mountType,
            panelType = panelType,
            paymentMethod = paymentMethod,"""

content = content.replace(call_old, call_new)

with open(file_path, "w") as f:
    f.write(content)
