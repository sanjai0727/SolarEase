import sys

file_path_repo = "/app/applet/app/src/main/java/com/example/feature/order/domain/repository/InstallationRepository.kt"
with open(file_path_repo, "r") as f:
    content = f.read()
content = content.replace(
    "suspend fun updateMilestoneProgress(key: String, progress: Int, activity: String?)",
    "suspend fun updateMilestoneProgress(key: String, progress: Int, activity: String?, isOffGrid: Boolean = false)"
)
content = content.replace(
    "suspend fun completeMilestone(key: String)",
    "suspend fun completeMilestone(key: String, isOffGrid: Boolean = false)"
)
with open(file_path_repo, "w") as f:
    f.write(content)

file_path_impl = "/app/applet/app/src/main/java/com/example/feature/order/data/repository/InstallationRepositoryImpl.kt"
with open(file_path_impl, "r") as f:
    content = f.read()

content = content.replace(
    "override suspend fun updateMilestoneProgress(key: String, progress: Int, activity: String?) {",
    "override suspend fun updateMilestoneProgress(key: String, progress: Int, activity: String?, isOffGrid: Boolean) {"
)
content = content.replace(
    """        var isOffGrid = false
        var currentList = _milestones.value.toMutableList()
        var index = currentList.indexOfFirst { it.key == key }
        if (index == -1) {
            currentList = _offGridMilestones.value.toMutableList()
            index = currentList.indexOfFirst { it.key == key }
            isOffGrid = true
        }""",
    """        var currentList = if (isOffGrid) _offGridMilestones.value.toMutableList() else _milestones.value.toMutableList()
        val index = currentList.indexOfFirst { it.key == key }"""
)

content = content.replace(
    "override suspend fun completeMilestone(key: String) {",
    "override suspend fun completeMilestone(key: String, isOffGrid: Boolean) {"
)

with open(file_path_impl, "w") as f:
    f.write(content)

print("Repo patched")
