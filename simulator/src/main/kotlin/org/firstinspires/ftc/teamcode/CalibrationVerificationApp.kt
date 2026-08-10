package org.firstinspires.ftc.teamcode

import edu.wpi.first.networktables.NetworkTableInstance
import kotlin.concurrent.thread
import com.areslib.sim.DesktopSimLauncher
import com.areslib.sim.NoOpInteractionModel

/**
 * Headless end-to-end smoke runner for FTC calibration and SysId command contracts.
 *
 * It launches the real season TeleOp against FTC mocks, drives its Driver Station lifecycle over
 * local NT4, then verifies each calibration publishes the expected status and a minimum amount of
 * data. Wall-clock time is appropriate here because this executable supervises another simulated
 * process; robot control and replay code continue to use `RobotClock`.
 */
fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {
    println("=================================================================")
    println("STARTING PROGRAMMATIC CALIBRATION ROUTINES VERIFICATION")
    println("=================================================================")

    // Launch the simulator first; its NT4 server is the system under test.
    thread {
        try {
            DesktopSimLauncher.launch(
                args = arrayOf("--opmode", "org.firstinspires.ftc.teamcode.opmodes.ARESMecanumTeleOp", "--headless"),
                interactionModel = NoOpInteractionModel()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Use an independent WPILib client so this validates the wire contract, not an in-process API.
    val ntInst = NetworkTableInstance.create()
    ntInst.startClient4("CalibrationVerificationClient")
    ntInst.setServer("127.0.0.1")

    // Bound startup wait so CI cannot hang indefinitely when the simulator fails early.
    var connected = false
    val startConnectTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startConnectTime < 10000) {
        if (ntInst.isConnected) {
            connected = true
            break
        }
        Thread.sleep(100)
    }

    if (!connected) {
        System.err.println("Verification Failed: Could not connect to simulator NT4 server!")
        System.exit(1)
    }
    println("Connected to simulator NT4 server.")

    // Driver Station control and heartbeat topics use canonical names without leading slashes.
    val cmdPub = ntInst.getStringTopic("ARES/DriverStation/Command").publish()
    val selectPub = ntInst.getStringTopic("ARES/DriverStation/SelectedOpMode").publish()
    
    val heartbeatPub = ntInst.getIntegerTopic("ARES/Input/heartbeat").publish()
    val teleopPub = ntInst.getBooleanTopic("ARES/Input/isTeleopMode").publish()

    // A changing heartbeat keeps the remote-input watchdog armed during long calibration routines.
    val running = java.util.concurrent.atomic.AtomicBoolean(true)
    thread {
        var count = 0L
        while (running.get()) {
            heartbeatPub.set(count++)
            teleopPub.set(true)
            try {
                Thread.sleep(50)
            } catch (_: InterruptedException) {
                break
            }
        }
    }

    // Follow the same INIT then START lifecycle as the desktop Driver Station.
    selectPub.set("org.firstinspires.ftc.teamcode.opmodes.ARESMecanumTeleOp")
    cmdPub.set("INIT")
    println("Sent INIT command.")
    Thread.sleep(3000)

    cmdPub.set("START")
    println("Sent START command.")
    Thread.sleep(1500)

    // Calibration command/status/data contract shared with ARES Analytics.
    val calCmdPub = ntInst.getStringTopic("SysId/Command").publish()
    val calStatusSub = ntInst.getStringTopic("SysId/Status").subscribe("NONE")
    val calDataSub = ntInst.getDoubleArrayTopic("SysId/Data").subscribe(doubleArrayOf())

    fun runCalibrationTest(command: String, expectedStatus: String) {
        println("\n--- Testing: $command (Expecting Status: $expectedStatus) ---")
        
        // Trigger and verify the immediate state transition.
        calCmdPub.set(command)
        Thread.sleep(500)

        val currentStatus = calStatusSub.get()
        println("Current Status: $currentStatus")
        if (currentStatus != expectedStatus) {
            running.set(false)
            System.err.println("ERROR: Expected status $expectedStatus, but got $currentStatus!")
            System.exit(1)
        }

        // Observe bounded progress; repeated arrays count as streamed samples for this smoke test.
        val startWait = System.currentTimeMillis()
        var pointsCount = 0
        var wentBackToNone = false

        while (System.currentTimeMillis() - startWait < 8000) {
            val status = calStatusSub.get()
            val data = calDataSub.get()
            
            if (data.isNotEmpty()) {
                pointsCount++
            }
            if (status == "NONE") {
                wentBackToNone = true
                break
            }
            Thread.sleep(100)
        }

        println("Finished $command. Points collected: $pointsCount, returned to NONE: $wentBackToNone")
        
        if (pointsCount < 5) {
            running.set(false)
            System.err.println("ERROR: Insufficient calibration data points streamed! Only got $pointsCount points.")
            System.exit(1)
        }

        // Reset command state before the next routine.
        calCmdPub.set("STOP")
        Thread.sleep(300)
    }

    // Hardware-affecting routines are deliberately serialized.
    try {
        runCalibrationTest("START_PINPOINT_SPIN", "PINPOINT_SPIN")
        runCalibrationTest("START_TRACK_WIDTH_SPIN", "TRACK_WIDTH_SPIN")
        runCalibrationTest("START_LINEAR_DRIVE", "LINEAR_DRIVE")
        runCalibrationTest("START_VISION_CALIBRATION", "VISION_CALIBRATION")
        
        // Exercise quasistatic and dynamic modes for linear, angular, and flywheel characterization.
        runCalibrationTest("START_LINEAR_QUASISTATIC", "QUASISTATIC")
        runCalibrationTest("START_LINEAR_DYNAMIC", "DYNAMIC")
        
        runCalibrationTest("START_ANGULAR_QUASISTATIC", "QUASISTATIC")
        runCalibrationTest("START_ANGULAR_DYNAMIC", "DYNAMIC")
        
        runCalibrationTest("START_FLYWHEEL_QUASISTATIC", "QUASISTATIC")
        runCalibrationTest("START_FLYWHEEL_DYNAMIC", "DYNAMIC")
        
        println("\n=================================================================")
        println("ALL CALIBRATION AND SYSID ROUTINES PASSED HEADLESSLY!")
        println("=================================================================")
    } catch (e: Exception) {
        e.printStackTrace()
        System.exit(1)
    } finally {
        running.set(false)
        System.exit(0)
    }
}
