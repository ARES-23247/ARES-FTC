package org.firstinspires.ftc.teamcode.dsl

import com.areslib.ftc.dsl.FtcTeleOpBase
import com.areslib.ftc.dsl.FtcTeleOpBuilder
import com.areslib.telemetry.GamepadState
import org.firstinspires.ftc.teamcode.opmodes.AresRobot

/**
 * Bridges ARESLib's declarative FTC lifecycle to the season [AresRobot] facade.
 * The shared base snapshots gamepads, invokes callbacks, runs [AresRobot.update], and
 * guarantees [AresRobot.close] on exit.
 */
abstract class AresTeleOpBase : FtcTeleOpBase<AresRobot>() {
    
    override fun buildRobot() = AresRobot(hardwareMap, telemetry)

    override fun updateRobot(robot: AresRobot, g1: GamepadState, g2: GamepadState) = robot.update(g1, g2)

    override fun closeRobot(robot: AresRobot) = robot.close()

    /**
     * Builds a DSL definition whose callbacks receive the concrete season facade.
     */
    fun aresTeleOp(block: FtcTeleOpBuilder<AresRobot>.() -> Unit): FtcTeleOpBuilder<AresRobot> {
        val builder = FtcTeleOpBuilder<AresRobot>()
        builder.block()
        return builder
    }
}
