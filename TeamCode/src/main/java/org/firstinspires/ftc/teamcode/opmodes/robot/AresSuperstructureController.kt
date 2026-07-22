package org.firstinspires.ftc.teamcode.opmodes.robot

import com.areslib.action.RobotAction
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.state.Alliance
import org.firstinspires.ftc.teamcode.dsl.*

class AresSuperstructureController(private val base: FtcMecanumRobot) {
    fun toggleIntake() {
        val season = base.store.state.superstructure.season
        base.store.dispatch(RobotAction.UpdateSubsystemState(
            state = season.copy(intakeActive = !season.intakeActive)
        ))
    }

    fun toggleShooter() {
        val season = base.store.state.superstructure.season
        val currentTarget = if (!season.flywheelActive) base.store.state.tuning.flywheelTargetRpmPreset else 0.0
        base.store.dispatch(RobotAction.UpdateSubsystemState(
            state = season.copy(
                flywheelActive = !season.flywheelActive,
                flywheelTargetRPM = currentTarget
            )
        ))
    }

    fun toggleAlliance() {
        val currentAlliance = base.store.state.drive.alliance
        val newAlliance = when (currentAlliance) {
            Alliance.RED -> Alliance.BLUE
            Alliance.BLUE -> Alliance.RED
        }
        base.store.dispatch(RobotAction.SetAlliance(newAlliance))
    }
}
