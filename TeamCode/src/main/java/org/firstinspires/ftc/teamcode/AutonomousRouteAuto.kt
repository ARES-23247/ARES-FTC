package org.firstinspires.ftc.teamcode

import com.areslib.pathing.DynamicPathLoader
import com.areslib.pathing.HolonomicPathFollower

/**
 * Auto-generated companion code for the path: autonomous_route.path
 * Map event callbacks to trigger subsystem commands.
 */
object AutonomousRouteAuto {
    val pathName = "autonomous_route"

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