import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/order/ui/SiteSurveyStatusScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

# 1. Add imports if they don't exist
imports = """import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.BottomSheetDefaults
import java.util.Calendar
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
"""
if "import androidx.compose.material3.ExperimentalMaterial3Api" not in content:
    content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\n" + imports)

# 2. Add state variables inside SiteSurveyStatusRoute
state_str = """    var isChecklistExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }"""

new_state_str = """    var isChecklistExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    var surveyDate by remember { mutableStateOf("Tomorrow") }
    var surveyTime by remember { mutableStateOf("10:00 AM - 11:30 AM") }
    
    var showRescheduleSheet by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
"""
content = content.replace(state_str, new_state_str)

# 3. Replace the text elements displaying the date and time
old_date = """Text(text = "Date: Tomorrow", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)"""
new_date = """Text(text = "Date: $surveyDate", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)"""
content = content.replace(old_date, new_date)

old_time = """Text(text = "Time: 10:00 AM - 11:30 AM", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)"""
new_time = """Text(text = "Time: $surveyTime", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)"""
content = content.replace(old_time, new_time)

# 4. Handle reschedule button click
old_reschedule = """OutlinedButton(
                        onClick = { /* Reschedule */ },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Reschedule")
                    }"""
new_reschedule = """OutlinedButton(
                        onClick = { showRescheduleSheet = true },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Reschedule")
                    }"""
content = content.replace(old_reschedule, new_reschedule)


# 5. Add dialogs and bottom sheet at the end of the composable (before closing brace)
closing_brace_index = content.rfind("}")
# Actually it's better to insert right before the last closing brace of the Column
# Wait, SiteSurveyStatusRoute has a main Column that is not the top level, let's just insert at the end of the Route composable
route_end = content.rfind("}")

dialogs_code = """
    if (showRescheduleSheet) {
        RescheduleBottomSheet(
            onDismiss = { showRescheduleSheet = false },
            onConfirm = { newDate, newTime ->
                surveyDate = newDate
                surveyTime = newTime
                showRescheduleSheet = false
                showSuccessDialog = true
            }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Survey Rescheduled Successfully") },
            text = { 
                Column {
                    Text("Your site survey has been successfully rescheduled.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("New Date: $surveyDate", fontWeight = FontWeight.Bold)
                    Text("New Time: $surveyTime", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
"""

content = content[:route_end] + dialogs_code + content[route_end:]

# 6. Add RescheduleBottomSheet component at the bottom of the file
bottom_sheet_code = """

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val timeSlots = listOf(
        "09:00 AM - 10:30 AM",
        "10:30 AM - 12:00 PM",
        "01:00 PM - 02:30 PM",
        "03:00 PM - 04:30 PM"
    )
    
    var selectedTimeSlot by remember { mutableStateOf<String?>(null) }
    
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val todayStart = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                return utcTimeMillis > todayStart
            }
        }
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Reschedule Site Survey",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Select your preferred survey date and time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            DatePicker(
                state = datePickerState,
                title = null,
                headline = null,
                showModeToggle = false,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Select Time Slot",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            timeSlots.forEach { slot ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { selectedTimeSlot = slot }
                        .background(
                            if (selectedTimeSlot == slot) MaterialTheme.colorScheme.primaryContainer 
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedTimeSlot == slot,
                        onClick = { selectedTimeSlot = slot }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = slot,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedTimeSlot == slot) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selectedTimeSlot == slot) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Your selected date and time will be used to reschedule your site survey.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        if (datePickerState.selectedDateMillis != null && selectedTimeSlot != null) {
                            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            val formattedDate = formatter.format(Date(datePickerState.selectedDateMillis!!))
                            onConfirm(formattedDate, selectedTimeSlot!!)
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    enabled = datePickerState.selectedDateMillis != null && selectedTimeSlot != null
                ) {
                    Text("Confirm Reschedule")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
"""

content += bottom_sheet_code

with open(file_path, "w") as f:
    f.write(content)
