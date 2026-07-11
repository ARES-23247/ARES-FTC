package org.firstinspires.ftc.teamcode.opmodes

import com.areslib.ftc.FtcMecanumRobot
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry
import com.areslib.hardware.HardwareRegistry
import com.areslib.action.RobotAction

/**
 * Team-specific wrapper around the core FtcMecanumRobot.
 * Adds specialized mechanisms like intake and shooter.
 */
class AresRobot(
    val hardwareMap: HardwareMap,
    val localTelemetry: Telemetry? = null
) {
    val base = FtcMecanumRobot(
        hardwareMap = hardwareMap,
        blName = "rl",
        brName = "rr",
        localTelemetry = localTelemetry
    )
    
    val intakeIO = org.firstinspires.ftc.teamcode.hardware.FtcIntakeIO(hardwareMap)
    val flywheelIO = org.firstinspires.ftc.teamcode.hardware.FtcFlywheelIO(hardwareMap)

    init {
        HardwareRegistry.registerDevice("Superstructure/Intake", intakeIO)
        HardwareRegistry.registerDevice("Superstructure/Flywheel", flywheelIO)
        HardwareRegistry.registerCloseable(intakeIO)
        HardwareRegistry.registerCloseable(flywheelIO)
    }

    fun update() {
        val timestamp = com.areslib.util.RobotClock.currentTimeMillis()
        
        // 1. Update drivebase sensors, EKF, and kinematics
        base.update()

        // 2. Read flywheel sensor RPM and update Redux store
        val currentRPM = flywheelIO.velocityRpm
        base.store.dispatch(RobotAction.UpdateFlywheelRPM(currentRPM, timestamp))

        // 3. Command actuators based on latest Redux target state
        val state = base.store.state

        if (state.superstructure.intakeActive) {
            intakeIO.setRollerVoltage(12.0)
        } else {
            intakeIO.setRollerVoltage(0.0)
        }

        if (state.superstructure.flywheelActive) {
            flywheelIO.setVelocityRpm(state.superstructure.flywheelTargetRPM)
        } else {
            flywheelIO.setVelocityRpm(0.0)
        }
    }

    fun driveFieldCentric(x: Double, y: Double, rotation: Double) {
        base.driveFieldCentric(x, y, rotation)
    }

    fun driveRobotCentric(x: Double, y: Double, rotation: Double) {
        base.driveRobotCentric(x, y, rotation)
    }

    fun close() {
        base.close()
        intakeIO.close()
        flywheelIO.close()
    }
}
