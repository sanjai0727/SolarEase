import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/order/ui/InstallationTrackingScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

new_content = content.replace(
    "@Composable\nfun InstallationTrackingRoute(\n    onNavigateNext: () -> Unit,\n    viewModel: InstallationTrackingViewModel = viewModel()\n) {",
    "@Composable\nfun InstallationTrackingRoute(\n    systemType: String,\n    onNavigateNext: () -> Unit,\n    viewModel: InstallationTrackingViewModel = viewModel()\n) {\n    LaunchedEffect(systemType) {\n        viewModel.initWorkflow(systemType)\n    }"
)

with open(file_path, "w") as f:
    f.write(new_content)

print("Screen Patched")
