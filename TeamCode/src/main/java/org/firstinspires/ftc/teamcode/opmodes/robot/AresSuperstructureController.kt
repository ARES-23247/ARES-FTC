package org.firstinspires.ftc.teamcode.opmodes.robot

import com.areslib.action.RobotAction
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.state.Alliance
import org.firstinspires.ftc.teamcode.dsl.*

class AresSuperstructureController(private val base: FtcMecanumRobot) {
    /**
     * Documentation for toggleIntake
     */
    fun toggleIntake() {
        /**
         * Documentation for season
         */
        val season = base.store.state.superstructure.season
        base.store.dispatch(RobotAction.UpdateSubsystemState(
            state = season.copy(intakeActive = !season.intakeActive)
        ))
    }
    /**
     * Documentation for toggleShooter
     */

    fun toggleShooter() {
        /**
         * Documentation for season
         */
        val season = base.store.state.superstructure.season
        /**
         * Documentation for currentTarget
         */
        val currentTarget = if (!season.flywheelActive) base.store.state.tuning.flywheelTargetRpmPreset else 0.0
        base.store.dispatch(RobotAction.UpdateSubsystemState(
            state = season.copy(
                flywheelActive = !season.flywheelActive,
                flywheelTargetRPM = currentTarget
            )
        ))
    }
    /**
     * Documentation for toggleAlliance
     */

    fun toggleAlliance() {
        /**
         * Documentation for currentAlliance
         */
        val currentAlliance = base.store.state.drive.alliance
        /**
         * Documentation for newAlliance
         */
        val newAlliance = when (currentAlliance) {
            Alliance.RED -> Alliance.BLUE
            Alliance.BLUE -> Alliance.RED
        }
        base.store.dispatch(RobotAction.SetAlliance(newAlliance))
    }
}
