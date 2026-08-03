import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/assessment/ui/SiteDetailsViewModel.kt"

with open(file_path, "r") as f:
    content = f.read()

replacement = """import androidx.lifecycle.SavedStateHandle

class SiteDetailsViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SiteDetailsUiState(
            monthlyConsumption = savedStateHandle.get<String>("monthlyConsumption") ?: "",
            availableArea = savedStateHandle.get<String>("availableArea") ?: "",
            mountType = savedStateHandle.get<String>("mountType") ?: "Rooftop",
            isPhotoCaptured = savedStateHandle.get<Boolean>("isPhotoCaptured") ?: false
        )
    )
"""

content = content.replace("class SiteDetailsViewModel : ViewModel() {\n    private val _uiState = MutableStateFlow(SiteDetailsUiState())", replacement)

# Replace update functions to also update savedStateHandle
update_consumption = """    fun updateMonthlyConsumption(input: String) {
        val filtered = input.filter { it.isDigit() }.take(5)
        var error: String? = null
        if (filtered.isEmpty()) {
            error = "Please enter your monthly electricity consumption."
        } else if (!Validation.isValidConsumption(filtered)) {
            error = "Consumption must be greater than 0."
        }
        
        savedStateHandle["monthlyConsumption"] = filtered
        _uiState.update {"""
content = content.replace("""    fun updateMonthlyConsumption(input: String) {
        val filtered = input.filter { it.isDigit() }.take(5)
        var error: String? = null
        if (filtered.isEmpty()) {
            error = "Please enter your monthly electricity consumption."
        } else if (!Validation.isValidConsumption(filtered)) {
            error = "Consumption must be greater than 0."
        }
        
        _uiState.update {""", update_consumption)


update_area = """    fun updateAvailableArea(input: String) {
        val filtered = input.filter { it.isDigit() || it == '.' }
        
        // Prevent multiple decimals
        val parts = filtered.split('.')
        val sanitized = if (parts.size > 2) {
            parts[0] + "." + parts.drop(1).joinToString("")
        } else {
            filtered
        }
        
        var error: String? = null
        if (sanitized.isEmpty()) {
            error = "Please enter the available installation area."
        } else if (!Validation.isValidArea(sanitized)) {
            val value = sanitized.toDoubleOrNull() ?: 0.0
            if (value <= 0) {
                error = "Area must be greater than 0."
            } else {
                error = "Area exceeds maximum limit."
            }
        }
        
        savedStateHandle["availableArea"] = sanitized
        _uiState.update {"""
content = content.replace("""    fun updateAvailableArea(input: String) {
        val filtered = input.filter { it.isDigit() || it == '.' }
        
        // Prevent multiple decimals
        val parts = filtered.split('.')
        val sanitized = if (parts.size > 2) {
            parts[0] + "." + parts.drop(1).joinToString("")
        } else {
            filtered
        }
        
        var error: String? = null
        if (sanitized.isEmpty()) {
            error = "Please enter the available installation area."
        } else if (!Validation.isValidArea(sanitized)) {
            val value = sanitized.toDoubleOrNull() ?: 0.0
            if (value <= 0) {
                error = "Area must be greater than 0."
            } else {
                error = "Area exceeds maximum limit."
            }
        }
        
        _uiState.update {""", update_area)


update_mount = """    fun updateMountType(type: String) {
        savedStateHandle["mountType"] = type
        _uiState.update {"""
content = content.replace("""    fun updateMountType(type: String) {
        _uiState.update {""", update_mount)

update_photo = """    fun updatePhotoCaptured(captured: Boolean) {
        savedStateHandle["isPhotoCaptured"] = captured
        _uiState.update {"""
content = content.replace("""    fun updatePhotoCaptured(captured: Boolean) {
        _uiState.update {""", update_photo)


with open(file_path, "w") as f:
    f.write(content)
