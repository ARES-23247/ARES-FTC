package org.firstinspires.ftc.teamcode.dsl

import com.areslib.ftc.dsl.FtcMecanumAutoBase
import com.areslib.ftc.dsl.FtcAutoBuilder
import com.areslib.ftc.dsl.FtcAutoDefinition
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.ftc.photon.PhotonEnabledOpMode
import com.areslib.util.PoseStorage
import org.firstinspires.ftc.teamcode.opmodes.AresRobot

/**
 * Team adapter for ARESLib's mecanum autonomous lifecycle.
 *
 * The shared base owns path loading, alliance mirroring, task execution, abort safety,
 * and final pose persistence. This adapter supplies the season facade and preserves
 * process-local team state needed by the following TeleOp.
 */
abstract class AresAutoBase : FtcMecanumAutoBase<AresRobot>(), PhotonEnabledOpMode {

    override fun buildRobot() = AresRobot(hardwareMap, telemetry)

    /** Builds a validated autonomous definition for the season robot. */
    fun auto(block: FtcAutoBuilder.() -> Unit): FtcAutoDefinition =
        com.areslib.ftc.dsl.ftcAuto(block)

    override fun getMecanumRobot(robot: AresRobot): FtcMecanumRobot = robot.base

    override fun updateRobot(robot: AresRobot) = robot.update()

    override fun closeRobot(robot: AresRobot) {
        org.firstinspires.ftc.teamcode.opmodes.TeamStateStorage.liftHeight = robot.base.store.state.superstructure.season.liftHeight
        // PoseStorage and TeamStateStorage do not survive a Robot Controller restart.
        PoseStorage.alliance = robot.base.store.state.drive.alliance
        robot.close()
    }
}
