import sys

file_path_impl = "/app/applet/app/src/main/java/com/example/feature/order/data/repository/InstallationRepositoryImpl.kt"
with open(file_path_impl, "r") as f:
    content = f.read()

hybrid_milestones = """
    private val _hybridMilestones = MutableStateFlow(
        listOf(
            Milestone(id = "1", key = "SITE_SURVEY_COMPLETED", title = "Site Survey Completed", description = "Site inspection completed and installation requirements verified.", status = MilestoneStatus.COMPLETED, completedAt = "11 Aug 2026, 02:00 PM"),
            Milestone(id = "2", key = "FEASIBILITY_APPROVED", title = "Feasibility & Load Sanction Approved", description = "Technical feasibility verified and required approvals obtained.", status = MilestoneStatus.COMPLETED, completedAt = "12 Aug 2026, 11:30 AM"),
            Milestone(id = "3", key = "MATERIALS_DISPATCHED", title = "Materials & Batteries Dispatched", description = "Solar panels, inverter, batteries, mounting structure and accessories have been dispatched.", status = MilestoneStatus.COMPLETED, completedAt = "14 Aug 2026, 09:00 AM"),
            Milestone(id = "4", key = "INSTALLATION_IN_PROGRESS", title = "Installation In Progress", description = "Installation team is currently working on-site.", status = MilestoneStatus.IN_PROGRESS, progress = 0, currentActivity = "Mounting Structure Installation", expectedCompletion = "Today, 5:30 PM"),
            Milestone(id = "5", key = "INSTALLATION_COMPLETED", title = "Installation Completed", description = "Mechanical and electrical installation has been completed successfully.", status = MilestoneStatus.PENDING),
            Milestone(id = "6", key = "TESTING_COMMISSIONING", title = "Testing & Battery Commissioning", description = "Battery charging, inverter configuration, electrical testing and hybrid system commissioning are currently being performed.", status = MilestoneStatus.PENDING),
            Milestone(id = "7", key = "EB_INSPECTION", title = "EB / DISCOM Physical Inspection", description = "Electricity Board officials inspect the installation before approving grid connectivity.", status = MilestoneStatus.PENDING),
            Milestone(id = "8", key = "NET_METER_INSTALLED", title = "Net Meter Installed & Approved", description = "Net meter has been installed and approved by the Electricity Board.", status = MilestoneStatus.PENDING),
            Milestone(id = "9", key = "GRID_SYNC", title = "Grid Synchronization", description = "Hybrid solar system has been synchronized with the electricity grid.", status = MilestoneStatus.PENDING),
            Milestone(id = "10", key = "SYSTEM_LIVE", title = "System Live & Customer Handover", description = "Hybrid solar system is operational and handed over to the customer.", status = MilestoneStatus.PENDING),
            Milestone(id = "11", key = "PROJECT_COMPLETED", title = "Project Completed", description = "Installation completed successfully. The Hybrid solar system is fully operational.", status = MilestoneStatus.PENDING)
        )
    )
"""

content = content.replace(
    "    private val _milestones = MutableStateFlow(",
    hybrid_milestones + "\n    private val _milestones = MutableStateFlow("
)

content = content.replace(
    "override fun getOnGridMilestones(): Flow<List<Milestone>> {",
    "override fun getHybridMilestones(): Flow<List<Milestone>> {\n        return _hybridMilestones.asStateFlow()\n    }\n\n    override fun getOnGridMilestones(): Flow<List<Milestone>> {"
)

content = content.replace(
    "override suspend fun updateMilestoneProgress(key: String, progress: Int, activity: String?, isOffGrid: Boolean) {",
    "override suspend fun updateMilestoneProgress(key: String, progress: Int, activity: String?, systemType: String) {"
)

content = content.replace(
    "var currentList = if (isOffGrid) _offGridMilestones.value.toMutableList() else _milestones.value.toMutableList()",
    """var currentList = when {
            systemType.equals("OFF-GRID", true) -> _offGridMilestones.value.toMutableList()
            systemType.equals("HYBRID", true) -> _hybridMilestones.value.toMutableList()
            else -> _milestones.value.toMutableList()
        }"""
)

content = content.replace(
    "if (isOffGrid) _offGridMilestones.value = currentList else _milestones.value = currentList",
    """when {
                systemType.equals("OFF-GRID", true) -> _offGridMilestones.value = currentList
                systemType.equals("HYBRID", true) -> _hybridMilestones.value = currentList
                else -> _milestones.value = currentList
            }"""
)

content = content.replace(
    "override suspend fun completeMilestone(key: String, isOffGrid: Boolean) {",
    "override suspend fun completeMilestone(key: String, systemType: String) {"
)

with open(file_path_impl, "w") as f:
    f.write(content)

print("Impl patched")
