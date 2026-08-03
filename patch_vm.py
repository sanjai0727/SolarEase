import sys
file_path = "/app/applet/app/src/main/java/com/example/feature/order/ui/InstallationTrackingViewModel.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace(
    """                    repository.updateMilestoneProgress("INSTALLATION_IN_PROGRESS", currentProgress, currentActivity)
                } else {
                    repository.completeMilestone("INSTALLATION_IN_PROGRESS")""",
    """                    repository.updateMilestoneProgress("INSTALLATION_IN_PROGRESS", currentProgress, currentActivity, isOffGrid)
                } else {
                    repository.completeMilestone("INSTALLATION_IN_PROGRESS", isOffGrid)"""
)

content = content.replace(
    """            for (key in remainingKeys) {
                delay(1500)
                repository.completeMilestone(key)
            }""",
    """            for (key in remainingKeys) {
                delay(1500)
                repository.completeMilestone(key, isOffGrid)
            }"""
)

with open(file_path, "w") as f:
    f.write(content)

print("ViewModel patched")
