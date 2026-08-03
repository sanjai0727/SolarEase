import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/order/ui/SiteSurveyStatusScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

# Make sure imports for LocalConfiguration and RoundedCornerShape exist
if "import androidx.compose.ui.platform.LocalConfiguration" not in content:
    content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.compose.ui.platform.LocalConfiguration\nimport androidx.compose.foundation.shape.RoundedCornerShape")

old_sheet_call = """    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {"""

new_sheet_call = """    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        windowInsets = BottomSheetDefaults.windowInsets
    ) {"""

content = content.replace(old_sheet_call, new_sheet_call)


old_column = """        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {"""

new_column = """        val configuration = LocalConfiguration.current
        val maxH = configuration.screenHeightDp.dp * 0.85f

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxH)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {"""

content = content.replace(old_column, new_column)

# Now find the bottom action buttons and move them outside the scrollable column.
# The end of the scrollable content is just before:
#             Row(
#                 modifier = Modifier.fillMaxWidth(),
#                 horizontalArrangement = Arrangement.spacedBy(16.dp)
#             ) {
#                 OutlinedButton(

# So we can just add a closing brace `}` before the Row, and then the Row will be inside the outer Column.

old_row = """            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {"""

new_row = """            Spacer(modifier = Modifier.height(24.dp))
            } // Close scrollable Column
            
            // Fixed bottom actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {"""

content = content.replace(old_row, new_row)


# And the end of the file has two closing braces for the bottom sheet and column, we added one more opening brace so we don't need to change the bottom braces. Wait:
# Previous structure:
# ModalBottomSheet {
#   Column {
#     ...
#     Row { buttons }
#     Spacer 16dp
#   }
# }
#
# New structure:
# ModalBottomSheet {
#   Column { // Outer
#     Column { // Inner
#       ...
#     } // Close inner
#     Row { buttons }
#     Spacer 16dp
#   } // Close outer
# }

with open(file_path, "w") as f:
    f.write(content)
