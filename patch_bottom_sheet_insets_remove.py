import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/order/ui/SiteSurveyStatusScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

# Just remove the windowInsets parameter since this version of Compose doesn't support it on ModalBottomSheet
content = content.replace("        windowInsets = WindowInsets.systemBars\n    ) {", "    ) {")

with open(file_path, "w") as f:
    f.write(content)
