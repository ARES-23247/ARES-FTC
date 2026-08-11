package org.firstinspires.ftc.teamcode.dsl

import com.areslib.ftc.dsl.FtcTeleOpBase
import com.areslib.ftc.dsl.FtcTeleOpBuilder
import com.areslib.ftc.photon.PhotonEnabledOpMode
import com.areslib.telemetry.GamepadState
import com.areslib.telemetry.RobotStatusTracker
import org.firstinspires.ftc.teamcode.generated.GeneratedAresProject
import org.firstinspires.ftc.teamcode.opmodes.AresRobot

/**
 * Bridges ARESLib's declarative FTC lifecycle to the season [AresRobot] facade.
 * The shared base snapshots gamepads, invokes callbacks, runs [AresRobot.update], and
 * guarantees [AresRobot.close] on exit.
 */
abstract class AresTeleOpBase : FtcTeleOpBase<AresRobot>(), PhotonEnabledOpMode {
    override fun buildRobot() = AresRobot(hardwareMap, telemetry)

    override fun getBaseRobot(robot: AresRobot) = robot.base

    override fun updateRobot(robot: AresRobot, g1: GamepadState, g2: GamepadState) = robot.update(g1, g2)

    override fun closeRobot(robot: AresRobot) = robot.close()

    /** Builds a validated definition whose receiver exposes the concrete season facade. */
    fun teleOp(block: FtcTeleOpBuilder<AresRobot>.() -> Unit): FtcTeleOpBuilder<AresRobot> =
        com.areslib.ftc.dsl.teleOp(block)
}

/**
 * Exclusive generated-controls TeleOp authority.
 *
 * Unlike [AresTeleOpBase], this base has no handwritten `controls` or `everyLoop` callbacks that
 * can race a generated binding for the same subsystem. A concrete season OpMode supplies exactly
 * one checked-in scheme ID after Analytics has generated it. Until a scheme exists, teams keep
 * using the handwritten base above; there is deliberately no placeholder mapping with guessed
 * Flydigi indexes.
 */
abstract class AresGeneratedTeleOpBase : FtcTeleOpBase<AresRobot>(), PhotonEnabledOpMode {
    protected abstract val generatedControlSchemeId: String

    private var generatedRuntime: FtcGeneratedProjectRuntime? = null
    private var generatedControls: FtcGeneratedControllerRunner? = null

    final override fun define(): FtcTeleOpBuilder<AresRobot> = com.areslib.ftc.dsl.teleOp {
        setup {
            robot.addTelemetry("Controls", "Generated scheme: $generatedControlSchemeId")
        }
        everyLoop {
            // Generated bindings are the sole control authority and run in updateRobot().
        }
    }

    final override fun buildRobot(): AresRobot {
        RobotStatusTracker.activeOpMode = "TeleOp INIT"
        require(generatedControlSchemeId in GeneratedAresProject.knownControlSchemeIds) {
            "Generated control scheme '$generatedControlSchemeId' does not exist; regenerate the ARES project"
        }
        val robot = AresRobot(hardwareMap, telemetry)
        val runtime = FtcGeneratedProjectRuntime(robot)
        generatedRuntime = runtime
        generatedControls = FtcGeneratedControllerRunner(
            gamepad1 = gamepad1,
            gamepad2 = gamepad2,
            runtimes = runtime.createControls(generatedControlSchemeId),
        )
        return robot
    }

    final override fun getBaseRobot(robot: AresRobot) = robot.base

    final override fun updateRobot(robot: AresRobot, g1: GamepadState, g2: GamepadState) {
        if (RobotStatusTracker.activeOpMode == "TeleOp") {
            generatedControls?.update(g1, g2)
            generatedRuntime?.updateTasks()
        }
        robot.update(g1, g2)
    }

    final override fun closeRobot(robot: AresRobot) {
        generatedControls?.cancel()
        generatedRuntime?.cancelAll("FTC generated-controls TeleOp stopped")
        generatedControls = null
        generatedRuntime = null
        robot.close()
    }

    /** Host-activity bridge for a driver controller's Android F1-F12 key-down event. */
    fun onDriverKeyDown(keyCode: Int): Boolean =
        generatedControls?.onKeyDown("driver", keyCode) ?: false

    /** Host-activity bridge for a driver controller's Android F1-F12 key-up event. */
    fun onDriverKeyUp(keyCode: Int): Boolean =
        generatedControls?.onKeyUp("driver", keyCode) ?: false

    /** Host-activity bridge for an operator controller's Android F1-F12 key-down event. */
    fun onOperatorKeyDown(keyCode: Int): Boolean =
        generatedControls?.onKeyDown("operator", keyCode) ?: false

    /** Host-activity bridge for an operator controller's Android F1-F12 key-up event. */
    fun onOperatorKeyUp(keyCode: Int): Boolean =
        generatedControls?.onKeyUp("operator", keyCode) ?: false
}
