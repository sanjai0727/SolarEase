import sys
file_path_impl = "/app/applet/app/src/main/java/com/example/feature/order/data/repository/InstallationRepositoryImpl.kt"
with open(file_path_impl, "r") as f:
    content = f.read()

content = content.replace(
    """    override suspend fun completeMilestone(key: String, isOffGrid: Boolean) {
        var isOffGrid = false
        var currentList = _milestones.value.toMutableList()
        var index = currentList.indexOfFirst { it.key == key }
        if (index == -1) {
            currentList = _offGridMilestones.value.toMutableList()
            index = currentList.indexOfFirst { it.key == key }
            isOffGrid = true
        }""",
    """    override suspend fun completeMilestone(key: String, isOffGrid: Boolean) {
        var currentList = if (isOffGrid) _offGridMilestones.value.toMutableList() else _milestones.value.toMutableList()
        val index = currentList.indexOfFirst { it.key == key }"""
)

with open(file_path_impl, "w") as f:
    f.write(content)

print("Repo fixed")
