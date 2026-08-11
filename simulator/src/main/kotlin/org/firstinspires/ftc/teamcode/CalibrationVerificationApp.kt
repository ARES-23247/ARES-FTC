package org.firstinspires.ftc.teamcode

import edu.wpi.first.networktables.NetworkTableInstance
import kotlin.concurrent.thread
import com.areslib.sim.DesktopSimLauncher
import com.areslib.sim.NoOpInteractionModel

/**
 * Headless end-to-end smoke runner for FTC calibration and SysId command contracts.
 *
 * It launches the calibration contract OpMode against FTC mocks, drives its Driver Station lifecycle
 * over local NT4, then verifies each calibration publishes the expected status and a minimum amount
 * of data. Wall-clock time is appropriate here because this executable supervises another simulated
 * process; robot control and replay code continue to use `RobotClock`.
 */
fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {
    println("=================================================================")
    println("STARTING PROGRAMMATIC CALIBRATION ROUTINES VERIFICATION")
    println("=================================================================")

    // Launch the simulator first; its NT4 server is the system under test.
    thread(isDaemon = true, name = "ARES-Calibration-Simulator") {
        try {
            DesktopSimLauncher.launch(
                args = arrayOf("--opmode", "org.firstinspires.ftc.teamcode.CalibrationTestOpMode", "--headless"),
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
        ntInst.close()
        error("Verification Failed: Could not connect to simulator NT4 server!")
    }
    println("Connected to simulator NT4 server.")

    // Driver Station control and heartbeat topics use canonical names without leading slashes.
    val cmdPub = ntInst.getStringTopic("ARES/DriverStation/Command").publish()
    val selectPub = ntInst.getStringTopic("ARES/DriverStation/SelectedOpMode").publish()
    
    val heartbeatPub = ntInst.getIntegerTopic("ARES/Input/heartbeat").publish()
    val teleopPub = ntInst.getBooleanTopic("ARES/Input/isTeleopMode").publish()

    // A changing heartbeat keeps the remote-input watchdog armed during long calibration routines.
    val running = java.util.concurrent.atomic.AtomicBoolean(true)
    val heartbeatThread = thread(isDaemon = true, name = "ARES-Calibration-Heartbeat") {
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
    selectPub.set("org.firstinspires.ftc.teamcode.CalibrationTestOpMode")
    cmdPub.set("INIT")
    println("Sent INIT command.")
    Thread.sleep(3000)

    cmdPub.set("START")
    println("Sent START command.")
    Thread.sleep(1500)

    // Calibration command/status/data contract shared with ARES Analytics.
    val calCmdPub = ntInst.getStringTopic("SysId/Command").publish()
    // WPILib's client-facing topic namespace includes the root slash used in server announcements.
    // Robot and dashboard code still normalize stored/published keys to no leading slash.
    val calStatusSub = ntInst.getStringTopic(
        com.areslib.telemetry.TelemetryTopicNormalizer.toWireTopic("SysId/Status")
    ).subscribe("NONE")
    val calDataSub = ntInst.getDoubleArrayTopic(
        com.areslib.telemetry.TelemetryTopicNormalizer.toWireTopic("SysId/Data")
    ).subscribe(doubleArrayOf())

    fun runCalibrationTest(command: String, expectedStatus: String) {
        println("\n--- Testing: $command (Expecting Status: $expectedStatus) ---")
        
        // Trigger and allow for NT4 topic announcement plus the robot's next control loop.
        calCmdPub.set(command)
        ntInst.flush()
        val statusDeadline = System.currentTimeMillis() + 2500L
        var currentStatus = calStatusSub.get()
        while (currentStatus != expectedStatus && System.currentTimeMillis() < statusDeadline) {
            Thread.sleep(50L)
            currentStatus = calStatusSub.get()
        }
        println("Current Status: $currentStatus")
        if (currentStatus != expectedStatus) {
            val serverStatus = com.areslib.networktables.NT4Server.getString("SysId/Status", "MISSING")
            error("Expected status $expectedStatus, but client got $currentStatus (server has $serverStatus)")
        }

        // Observe bounded progress; repeated arrays count as streamed samples for this smoke test.
        val startWait = System.currentTimeMillis()
        var pointsCount = 0
        var wentBackToNone = false
        var lastDataChange = calDataSub.lastChange

        while (System.currentTimeMillis() - startWait < 8000) {
            val status = calStatusSub.get()
            val data = calDataSub.get()
            
            val dataChange = calDataSub.lastChange
            if (data.isNotEmpty() && dataChange != 0L && dataChange != lastDataChange) {
                pointsCount++
                lastDataChange = dataChange
            }
            if (status == "NONE") {
                wentBackToNone = true
                break
            }
            Thread.sleep(100)
        }

        println("Finished $command. Points collected: $pointsCount, returned to NONE: $wentBackToNone")
        
        val failureMessage = when {
            !wentBackToNone ->
                "Calibration routine $command did not return to NONE before the completion deadline"
            pointsCount < 5 ->
                "Insufficient fresh calibration samples: received $pointsCount"
            else -> null
        }

        // Reset command state before the next routine, including on a recorded verification failure.
        calCmdPub.set("STOP")
        ntInst.flush()
        Thread.sleep(300)
        if (failureMessage != null) error(failureMessage)
    }

    // Hardware-affecting routines are deliberately serialized.
    var succeeded = false
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
        succeeded = true
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        running.set(false)
        heartbeatThread.interrupt()
        try {
            heartbeatThread.join(1000L)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        try { cmdPub.set("STOP") } catch (_: Exception) {}
        calCmdPub.close()
        calStatusSub.close()
        calDataSub.close()
        heartbeatPub.close()
        teleopPub.close()
        cmdPub.close()
        selectPub.close()
        ntInst.close()
    }
    kotlin.system.exitProcess(if (succeeded) 0 else 1)
}
