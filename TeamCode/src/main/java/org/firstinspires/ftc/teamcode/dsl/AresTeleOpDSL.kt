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
        robot.update(g1, g2)
    }

    override fun closeRobot(robot: AresRobot) {
        robot.close()
    }

    /**
     * Entrypoint for the DSL configuration block, typed to AresRobot.
     */
    fun aresTeleOp(block: FtcTeleOpBuilder<AresRobot>.() -> Unit): FtcTeleOpBuilder<AresRobot> {
        /**
         * Documentation for builder
         */
        val builder = FtcTeleOpBuilder<AresRobot>()
        builder.block()
        return builder
    }
}
