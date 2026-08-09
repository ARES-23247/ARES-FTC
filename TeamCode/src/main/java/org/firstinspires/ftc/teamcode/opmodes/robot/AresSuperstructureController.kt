package org.firstinspires.ftc.teamcode.opmodes.robot

import com.areslib.action.RobotAction
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.state.Alliance
import org.firstinspires.ftc.teamcode.dsl.*

import com.areslib.util.RobotClock

class AresSuperstructureController(private val base: FtcMecanumRobot) {
    private var lastIntakeToggleTimeMs = 0L
    private var lastShooterToggleTimeMs = 0L
    private var lastAllianceToggleTimeMs = 0L

    // TODO: Create ToggleIntake and ToggleFlywheel actions in ARESLib-Kotlin RobotAction
    /**
     * Documentation for toggleIntake
     */
    fun toggleIntake() {
        if (RobotClock.currentTimeMillis() - lastIntakeToggleTimeMs < 200) return
        lastIntakeToggleTimeMs = RobotClock.currentTimeMillis()
        /**
         * Documentation for season
         */
        val season = base.store.state.superstructure.season
        val MIN_CLEARANCE_HEIGHT = 0.1
        if (season.liftHeight < MIN_CLEARANCE_HEIGHT && !season.intakeActive) return

        base.store.dispatch(RobotAction.UpdateSubsystemState(
            state = season.copy(intakeActive = !season.intakeActive)
        ))
    }
    /**
     * Documentation for toggleShooter
     */

    fun toggleShooter() {
        if (RobotClock.currentTimeMillis() - lastShooterToggleTimeMs < 200) return
        lastShooterToggleTimeMs = RobotClock.currentTimeMillis()
        /**
         * Documentation for season
         */
        val season = base.store.state.superstructure.season
        if (season.intakeActive) return
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
        if (RobotClock.currentTimeMillis() - lastAllianceToggleTimeMs < 200) return
        lastAllianceToggleTimeMs = RobotClock.currentTimeMillis()
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
