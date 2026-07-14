package org.firstinspires.ftc.teamcode.opmodes

import com.areslib.ftc.FtcMecanumRobot
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry
import com.areslib.hardware.HardwareRegistry
import com.areslib.action.RobotAction
import org.firstinspires.ftc.teamcode.dsl.*

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
        rlName = "rl",
        rrName = "rr",
        localTelemetry = localTelemetry
    )
    
    val intakeIO = org.firstinspires.ftc.teamcode.hardware.FtcIntakeIO(hardwareMap)
    val flywheelIO = org.firstinspires.ftc.teamcode.hardware.FtcFlywheelIO(hardwareMap)

    init {
        HardwareRegistry.registerDevice("Superstructure/Intake", intakeIO)
        HardwareRegistry.registerDevice("Superstructure/Flywheel", flywheelIO)
        HardwareRegistry.registerCloseable(intakeIO)
        HardwareRegistry.registerCloseable(flywheelIO)
        
        // Initialize subsystem states in the Redux store
        base.store.dispatch(RobotAction.UpdateSubsystemState(IntakeState()))
        base.store.dispatch(RobotAction.UpdateSubsystemState(FlywheelState()))
    }

    fun update() {
        val timestamp = com.areslib.util.RobotClock.currentTimeMillis()

        // 1. Update drivebase sensors, EKF, and kinematics
        base.update()

        // 2. Read flywheel sensor RPM and update Redux store
        val currentRPM = flywheelIO.velocityRpm
        val state = base.store.state
        base.store.dispatch(RobotAction.UpdateSubsystemState(
            FlywheelState(
                flywheelActive = state.superstructure.flywheelActive,
                flywheelTargetRPM = state.superstructure.flywheelTargetRPM,
                currentRPM = currentRPM
            )
        ))

        // 3. Command actuators based on latest Redux target state
        val updatedState = base.store.state

        if (updatedState.superstructure.intakeActive) {
            intakeIO.setRollerVoltage(12.0)
        } else {
            intakeIO.setRollerVoltage(0.0)
        }

        if (updatedState.superstructure.flywheelActive) {
            flywheelIO.setVelocityRpm(updatedState.superstructure.flywheelTargetRPM)
        } else {
            flywheelIO.setVelocityRpm(0.0)
        }
    }



    // Action Composition Helpers
    fun setIntakeActive(active: Boolean) {
        val currentState = base.store.state.superstructure
        base.store.dispatch(RobotAction.UpdateSubsystemState(
            IntakeState(intakeActive = active)
        ))
    }

    fun setFlywheelActive(active: Boolean, targetRPM: Double = 3000.0) {
        val currentState = base.store.state.superstructure
        base.store.dispatch(RobotAction.UpdateSubsystemState(
            FlywheelState(
                flywheelActive = active,
                flywheelTargetRPM = targetRPM,
                currentRPM = currentState.currentRPM // Preserve current reading
            )
        ))
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

