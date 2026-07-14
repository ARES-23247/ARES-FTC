package org.firstinspires.ftc.teamcode.dsl

import com.areslib.ftc.dsl.FtcTeleOpBase
import com.areslib.ftc.dsl.FtcTeleOpBuilder
import com.areslib.telemetry.GamepadState
import org.firstinspires.ftc.teamcode.opmodes.AresRobot

/**
 * Team-specific base class for declarative student OpModes.
 * Bridges the generic library DSL with the concrete AresRobot wrapper.
 */
abstract class AresTeleOpBase : FtcTeleOpBase<AresRobot>() {
    
    override fun buildRobot(): AresRobot {
        return AresRobot(hardwareMap, telemetry)
    }

    override fun updateRobot(robot: AresRobot, g1: GamepadState, g2: GamepadState) {
        // First update the core drivebase/EKF with gamepad inputs
        robot.base.update(g1, g2)
        // Then update the physical intake/shooter hardware based on the state
        robot.update()
    }

    override fun closeRobot(robot: AresRobot) {
        robot.close()
    }

    /**
     * Entrypoint for the DSL configuration block, typed to AresRobot.
     */
    fun aresTeleOp(block: FtcTeleOpBuilder<AresRobot>.() -> Unit): FtcTeleOpBuilder<AresRobot> {
        val builder = FtcTeleOpBuilder<AresRobot>()
        builder.block()
        return builder
    }
}
