package org.firstinspires.ftc.teamcode.opmodes

import com.areslib.pathing.DynamicPathLoader
import com.areslib.pathing.HolonomicPathFollower

/**
 * Auto-generated companion code for the path: TestPath.path
 * Map event callbacks to trigger subsystem commands.
 */
object TestPathAuto {
    /**
     * Documentation for pathName
     */
    val pathName = "TestPath"
    /**
     * Documentation for buildPathFollower
     */

    fun buildPathFollower(
        follower: HolonomicPathFollower,
        eventMap: Map<String, () -> Unit>
    ) {
        /**
         * Documentation for path
         */
        val path = DynamicPathLoader.loadPath(pathName)
        follower.startPath(path)
        follower.onEventTriggered = { eventName ->
            println("[Auto] Path event triggered: $eventName")
            eventMap[eventName]?.invoke()
        }
    }
}
