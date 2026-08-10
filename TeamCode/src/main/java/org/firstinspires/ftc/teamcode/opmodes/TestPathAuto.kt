package org.firstinspires.ftc.teamcode.opmodes

import com.areslib.pathing.DynamicPathLoader
import com.areslib.pathing.HolonomicPathFollower

/**
 * Compatibility helper for loading `TestPath.path` and routing its named markers.
 * Competition autonomous normally uses `AutoBuilder`; this remains for focused path tests and
 * tools that need a direct [HolonomicPathFollower].
 */
object TestPathAuto {
    const val pathName = "TestPath"

    /** Starts the path and invokes a callback only when the marker name is registered. */
    fun buildPathFollower(
        follower: HolonomicPathFollower,
        eventMap: Map<String, () -> Unit>
    ) {
        val path = DynamicPathLoader.loadPath(pathName)
        follower.startPath(path)
        follower.onEventTriggered = { eventName ->
            println("[Auto] Path event triggered: $eventName")
            eventMap[eventName]?.invoke()
        }
    }
}
