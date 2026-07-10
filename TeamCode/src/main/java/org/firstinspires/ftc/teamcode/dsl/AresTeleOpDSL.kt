package org.firstinspires.ftc.teamcode.dsl

import com.areslib.ftc.FtcMecanumRobot
import com.areslib.telemetry.AresGamepad
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.hardware.Gamepad
import com.areslib.telemetry.GamepadState
import org.firstinspires.ftc.robotcore.external.Telemetry

/**
 * DSL Builder class for constructing an Ares TeleOp.
 */
class AresTeleOpBuilder {
    internal var onInitBlock: ((FtcMecanumRobot, Telemetry) -> Unit)? = null
    internal var onLoopBlock: ((FtcMecanumRobot, AresGamepad, Telemetry) -> Unit)? = null

    fun onInit(block: (robot: FtcMecanumRobot, telemetry: Telemetry) -> Unit) {
        onInitBlock = block
    }

    fun onLoop(block: (robot: FtcMecanumRobot, driver: AresGamepad, telemetry: Telemetry) -> Unit) {
        onLoopBlock = block
    }
}

/**
 * Base class for declarative student OpModes.
 * Manages lifecycle loops, proxy starting, log uploading, Redux loops, and telemetry flushing.
 */
abstract class AresTeleOpBase : LinearOpMode() {
    abstract fun define(): AresTeleOpBuilder

    /**
     * Entrypoint for the DSL configuration block.
     */
    fun aresTeleOp(block: AresTeleOpBuilder.() -> Unit): AresTeleOpBuilder {
        val builder = AresTeleOpBuilder()
        builder.block()
        return builder
    }
    
    override fun runOpMode() {
        val builder = define()
        
        // Configure the EKF with the tag positions of the selected field layout
        com.areslib.math.PoseEstimator.activeTags = com.areslib.math.FieldLayouts.getTagsForLayout(com.areslib.math.FieldLayout.SQUARE_STANDARD)

        val robot = FtcMecanumRobot(
            hardwareMap = hardwareMap,
            flName = "fl",
            frName = "fr",
            blName = "rl", // based on the original OpMode, it's 'rl' and 'rr'
            brName = "rr",
            pinpointName = "pinpoint",
            limelightName = "limelight",
            localTelemetry = telemetry,
            flDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD,
            blDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD,
            frDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE,
            brDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE
        )

        // Calibrate static friction feedforward (kS) to overcome physical drivetrain deadband
        robot.mecanumIO.kS = 0.05

        // Dispatch alliance choice to core Redux store
        robot.store.dispatch(com.areslib.action.RobotAction.SetAlliance(com.areslib.state.Alliance.RED))

        // Set initial pose/heading offset at center (0,0)
        robot.pinpointIO?.initialize(
            com.areslib.math.Pose2d(0.0, 0.0, com.areslib.math.Rotation2d(0.0)),
            resetHardware = true
        )

        val driver = AresGamepad()
        driver.leftStick.label("Field-centric Translation (X/Y)")
        driver.rightStickX.label("Robot Rotation")
        driver.b.label("Auto-Align to Tag 1")
        driver.y.label("Reset Field Centric Pose")
        driver.x.label("Drive to TestWaypoint")

        try {
            while (opModeInInit()) {
                robot.update(null, null)
                builder.onInitBlock?.invoke(robot, telemetry)
                telemetry.update()
                sleep(20)
            }
            if (isStopRequested) return

            // Mark initialization complete to enable active-play vision outlier filtering
            robot.visionTracker.isInInit = false

            // NOTE: FtcMecanumRobot base init auto-stops the proxy for us now!
            com.areslib.ftc.telemetry.LimelightProxyAutoStart.stop()
            
            while (opModeIsActive()) {
                val g1State = gamepad1.toState()
                val g2State = gamepad2.toState()
                
                driver.update(g1State)
                
                // Allow the user DSL loop to dispatch inputs
                builder.onLoopBlock?.invoke(robot, driver, telemetry)
                
                // Reset pose if Triangle (Y) is pressed (from original boilerplate)
                if (gamepad1.y) {
                    val now = com.areslib.util.RobotClock.currentTimeMillis()
                    robot.pinpointIO?.initialize(
                        com.areslib.math.Pose2d(0.0, 0.0, com.areslib.math.Rotation2d(0.0))
                    )
                    robot.store.dispatch(
                        com.areslib.action.RobotAction.PoseUpdate(
                            xMeters = 0.0,
                            yMeters = 0.0,
                            headingRadians = 0.0,
                            timestampMs = now,
                            isReset = true
                        )
                    )
                }
                
                // Compute kinematics, push to motors, upload NT4 frames
                robot.update(g1State, g2State)
            }
        } finally {
            // Note: robot.close() handles proxy restart and local archiving automatically!
            robot.close()
        }
    }
}

/**
 * Converts an FTC SDK Gamepad into a platform-agnostic GamepadState
 * for the ARESLib logging pipeline.
 */
fun Gamepad.toState() = GamepadState(
    leftStickX = left_stick_x,
    leftStickY = left_stick_y,
    rightStickX = right_stick_x,
    rightStickY = right_stick_y,
    leftTrigger = left_trigger,
    rightTrigger = right_trigger,
    a = a,
    b = b,
    x = x,
    y = y,
    dpadUp = dpad_up,
    dpadDown = dpad_down,
    dpadLeft = dpad_left,
    dpadRight = dpad_right,
    leftBumper = left_bumper,
    rightBumper = right_bumper,
    leftStickButton = left_stick_button,
    rightStickButton = right_stick_button,
    start = start,
    back = back
)
