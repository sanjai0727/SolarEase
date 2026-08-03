import sys

file_path = "/app/applet/app/src/main/java/com/example/navigation/Screen.kt"

with open(file_path, "r") as f:
    content = f.read()

new_content = content.replace(
    "@Serializable\n    data object SiteSurveyStatus : Screen()",
    "@Serializable\n    data class SiteSurveyStatus(val systemType: String) : Screen()"
)
new_content = new_content.replace(
    "@Serializable\n    data object InstallationTracking : Screen()",
    "@Serializable\n    data class InstallationTracking(val systemType: String) : Screen()"
)

with open(file_path, "w") as f:
    f.write(new_content)

print("Screen.kt patched")
