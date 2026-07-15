package org.firstinspires.ftc.teamcode

import edu.wpi.first.networktables.NetworkTableInstance
import kotlin.concurrent.thread
import com.areslib.sim.DesktopSimLauncher
import com.areslib.sim.NoOpInteractionModel

fun main(args: Array<String>) {
    println("=================================================================")
    println("STARTING PROGRAMMATIC CALIBRATION ROUTINES VERIFICATION")
    println("=================================================================")

    // 1. Start simulator in background thread
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

    // 2. Setup NT4 client
    val ntInst = NetworkTableInstance.create()
    ntInst.startClient4("CalibrationVerificationClient")
    ntInst.setServer("127.0.0.1")

    // Wait for connection
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

    // Get DS topics
    val cmdPub = ntInst.getStringTopic("ARES/DriverStation/Command").publish()
    val selectPub = ntInst.getStringTopic("ARES/DriverStation/SelectedOpMode").publish()
    
    val heartbeatPub = ntInst.getIntegerTopic("ARES/Input/heartbeat").publish()
    val teleopPub = ntInst.getBooleanTopic("ARES/Input/isTeleopMode").publish()

    // Start background heartbeat publisher
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

    // 3. Command INIT & START
    selectPub.set("org.firstinspires.ftc.teamcode.opmodes.ARESMecanumTeleOp")
    cmdPub.set("INIT")
    println("Sent INIT command.")
    Thread.sleep(3000)

    cmdPub.set("START")
    println("Sent START command.")
    Thread.sleep(1500)

    // SysId / Calib Topics
    val calCmdPub = ntInst.getStringTopic("SysId/Command").publish()
    val calStatusSub = ntInst.getStringTopic("SysId/Status").subscribe("NONE")
    val calDataSub = ntInst.getDoubleArrayTopic("SysId/Data").subscribe(doubleArrayOf())

    fun runCalibrationTest(command: String, expectedStatus: String) {
        println("\n--- Testing: $command (Expecting Status: $expectedStatus) ---")
        
        // 1. Trigger
        calCmdPub.set(command)
        Thread.sleep(500)

        // 2. Verify state transition
        val currentStatus = calStatusSub.get()
        println("Current Status: $currentStatus")
        if (currentStatus != expectedStatus) {
            running.set(false)
            System.err.println("ERROR: Expected status $expectedStatus, but got $currentStatus!")
            System.exit(1)
        }

        // 3. Wait for progress and gather data points
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

        // 4. Reset command to STOP to ensure state is clean
        calCmdPub.set("STOP")
        Thread.sleep(300)
    }

    // Run all calibrations sequentially
    try {
        runCalibrationTest("START_PINPOINT_SPIN", "PINPOINT_SPIN")
        runCalibrationTest("START_TRACK_WIDTH_SPIN", "TRACK_WIDTH_SPIN")
        runCalibrationTest("START_LINEAR_DRIVE", "LINEAR_DRIVE")
        runCalibrationTest("START_VISION_CALIBRATION", "VISION_CALIBRATION")
        
        // Run combined SysId routines for Drive Linear, Drive Angular, and Flywheel
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
