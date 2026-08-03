import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/auth/ui/AuthPhoneScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

replacement1 = """    val isPhoneNumberValid = phoneNumber.length == 10 && phoneNumber.firstOrNull()?.let { it in listOf('6', '7', '8', '9') } == true"""
content = content.replace("    val isPhoneNumberValid = phoneNumber.isNotBlank()", replacement1)

replacement2 = """        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { newValue -> 
                val filtered = newValue.filter { it.isDigit() }.take(10)
                phoneNumber = filtered 
            },
            label = { Text("Mobile Number *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            isError = uiState is AuthUiState.Error || (phoneNumber.isNotEmpty() && !isPhoneNumberValid),
            supportingText = {
                if (uiState is AuthUiState.Error) {
                    Text((uiState as AuthUiState.Error).message)
                } else if (phoneNumber.isNotEmpty() && !isPhoneNumberValid) {
                    Text("Enter a valid 10-digit mobile number.", color = MaterialTheme.colorScheme.error)
                }
            }
        )"""

old2 = """        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Mobile Number *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            isError = uiState is AuthUiState.Error,
            supportingText = {
                if (uiState is AuthUiState.Error) {
                    Text((uiState as AuthUiState.Error).message)
                }
            }
        )"""

content = content.replace(old2, replacement2)

with open(file_path, "w") as f:
    f.write(content)
