import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/order/ui/SiteSurveyStatusScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

# Material 3 ModalBottomSheet does have windowInsets, but maybe in a newer version. Let's use contentWindowInsets instead, or just remove it if not available.
old_sheet_call = """    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        windowInsets = BottomSheetDefaults.windowInsets
    ) {"""

new_sheet_call = """    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        windowInsets = BottomSheetDefaults.windowInsets
    ) {"""

if "windowInsets = BottomSheetDefaults.windowInsets" in content:
    content = content.replace("windowInsets = BottomSheetDefaults.windowInsets", "windowInsets = WindowInsets.systemBars")
    
if "import androidx.compose.foundation.layout.WindowInsets" not in content:
    content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.compose.foundation.layout.WindowInsets\nimport androidx.compose.foundation.layout.systemBars")

with open(file_path, "w") as f:
    f.write(content)
