import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/assessment/ui/PanelSelectionScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

content = content.replace("""@Composable
fun PanelSelectionRoute(
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {""", """@Composable
fun PanelSelectionRoute(
    onNavigateNext: (String) -> Unit,
    onNavigateBack: () -> Unit
) {""")

content = content.replace("""        Button(
            onClick = onNavigateNext,""", """        Button(
            onClick = { selectedPanel?.let { onNavigateNext(it) } },""")

with open(file_path, "w") as f:
    f.write(content)
