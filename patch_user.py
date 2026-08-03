import sys

file_path = "/app/applet/app/src/main/java/com/example/core/session/UserSession.kt"

with open(file_path, "r") as f:
    content = f.read()

new_content = content.replace(
    "val registrationCompleted: Boolean = false",
    "val registrationCompleted: Boolean = false,\n    val onboardingCompleted: Boolean = false,\n    val installationCompleted: Boolean = false"
)

with open(file_path, "w") as f:
    f.write(new_content)

print("UserSession patched")
