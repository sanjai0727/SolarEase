import sys
import os

file_path = "/app/applet/app/src/main/java/com/example/ui/theme/Shape.kt"
if not os.path.exists(file_path):
    print(f"{file_path} not found")
    sys.exit(1)

with open(file_path, "r") as f:
    content = f.read()

old_content = """val Shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(50) // Fully rounded
)"""

new_content = """val Shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(20.dp)
)"""

content = content.replace(old_content, new_content)

with open(file_path, "w") as f:
    f.write(content)

print("Shape.kt patched")
