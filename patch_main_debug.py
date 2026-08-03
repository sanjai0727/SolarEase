import sys

file_path = "/app/applet/app/src/main/java/com/example/MainActivity.kt"

with open(file_path, "r") as f:
    content = f.read()

new_content = content.replace(
    "com.example.core.session.UserSession.init(this)",
    "com.example.core.session.UserSession.init(this)\n    \n    if (BuildConfig.DEBUG) {\n        com.example.core.session.UserSession.clear(this)\n    }"
)

with open(file_path, "w") as f:
    f.write(new_content)

print("MainActivity patched for DEBUG reset")
