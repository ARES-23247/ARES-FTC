package org.firstinspires.ftc.teamcode.dsl

import com.areslib.math.coordinate.AllianceMirroring
import com.areslib.math.coordinate.FieldSymmetry
import com.areslib.math.geometry.Pose2d
import com.areslib.math.geometry.Rotation2d
import com.areslib.routine.AutonomousCatalogEntry
import com.areslib.routine.RoutineAlliance
import com.areslib.routine.RoutinePose
import com.areslib.state.Alliance

/**
 * Small deterministic state machine behind the FTC Driver Station INIT selector.
 *
 * Only enabled generated entries are exposed. D-pad left/right cycles the routine and X toggles
 * alliance on rising edges, so the SDK's repeated INIT frames cannot skip choices. A derived
 * validation OpMode may lock either value while the competition OpMode leaves both editable.
 */
internal class FtcAutonomousSelector(
    entries: List<AutonomousCatalogEntry>,
    defaultEntryId: String?,
    initialAlliance: Alliance,
    private val lockedEntryId: String? = null,
    private val lockedAlliance: Alliance? = null,
) {
    val entries: List<AutonomousCatalogEntry> = entries
        .asSequence()
        .filter(AutonomousCatalogEntry::enabled)
        .sortedWith(compareBy<AutonomousCatalogEntry> { it.sortOrder }.thenBy { it.entryId })
        .toList()

    private var index: Int = selectInitialIndex(lockedEntryId ?: defaultEntryId)
    private var previousLeft = false
    private var previousRight = false
    private var previousAllianceToggle = false

    var alliance: Alliance = lockedAlliance ?: initialAlliance
        private set

    val selected: AutonomousCatalogEntry?
        get() = entries.getOrNull(index)

    /** Selects one generated entry by stable ID; invalid or locked requests leave the fallback. */
    fun selectEntry(entryId: String): Boolean {
        if (lockedEntryId != null) return false
        val requestedIndex = entries.indexOfFirst { it.entryId == entryId }
        if (requestedIndex < 0 || requestedIndex == index) return false
        index = requestedIndex
        return true
    }

    /** Applies one cached gamepad sample and returns true when selection metadata changed. */
    fun update(left: Boolean, right: Boolean, toggleAlliance: Boolean): Boolean {
        var changed = false
        if (lockedEntryId == null && entries.size > 1) {
            if (left && !previousLeft) {
                index = (index - 1 + entries.size) % entries.size
                changed = true
            }
            if (right && !previousRight) {
                index = (index + 1) % entries.size
                changed = true
            }
        }
        if (lockedAlliance == null && toggleAlliance && !previousAllianceToggle) {
            alliance = if (alliance == Alliance.RED) Alliance.BLUE else Alliance.RED
            changed = true
        }
        previousLeft = left
        previousRight = right
        previousAllianceToggle = toggleAlliance
        return changed
    }

    private fun selectInitialIndex(requestedId: String?): Int {
        if (entries.isEmpty()) return -1
        val requestedIndex = entries.indexOfFirst { it.entryId == requestedId }
        return if (requestedIndex >= 0) requestedIndex else 0
    }
}

/**
 * Resolves an authored pose for the selected alliance.
 *
 * Mirroring is relative to the catalog's authored alliance, rather than the legacy assumption
 * that one hard-coded alliance is always canonical. FTC uses the established center-origin,
 * rotational (180-degree) field symmetry and CCW-positive headings.
 */
internal fun resolveFtcAutonomousPose(
    entry: AutonomousCatalogEntry,
    selectedAlliance: Alliance,
    pose: RoutinePose = entry.startingPose,
): Pose2d {
    val authoredAlliance = when (entry.authoredAlliance) {
        RoutineAlliance.RED -> Alliance.RED
        RoutineAlliance.BLUE -> Alliance.BLUE
    }
    val authoredPose = Pose2d(
        pose.xMeters,
        pose.yMeters,
        Rotation2d(pose.headingRadians),
    )
    if (!entry.mirrorForOppositeAlliance || selectedAlliance == authoredAlliance) {
        return authoredPose
    }
    // AllianceMirroring's RED branch is the involutive geometry operation itself. Catalog
    // metadata decides *when* to apply it, avoiding dependence on that helper's base alliance.
    return AllianceMirroring.mirror(
        authoredPose,
        Alliance.RED,
        FieldSymmetry.ROTATIONAL,
    )
}
