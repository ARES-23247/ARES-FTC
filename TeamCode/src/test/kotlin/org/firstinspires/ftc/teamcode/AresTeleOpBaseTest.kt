package org.firstinspires.ftc.teamcode

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.VoltageSensor
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.hardware.limelightvision.Limelight3A
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.hardware.Gamepad
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.teamcode.dsl.AresTeleOpBase
import com.areslib.ftc.dsl.FtcTeleOpBuilder
import com.areslib.ftc.toState
import com.areslib.ftc.update
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito
import kotlin.concurrent.thread

import com.areslib.telemetry.GamepadState
import com.areslib.telemetry.RobotStatusTracker
/**
 * Documentation for AresTeleOpBaseTest
 */

class AresTeleOpBaseTest {
    /**
     * Documentation for killFlag
     */

    @Volatile
    var killFlag = false
    /**
     * Documentation for testAresTeleOpBaseLifecycle
     */

    @Test
    fun testAresTeleOpBaseLifecycle() {
        com.areslib.telemetry.RobotStatusTracker.isEnabled = false
        /**
         * Documentation for fl
         */
        val fl = Mockito.mock(DcMotorEx::class.java)
        /**
         * Documentation for fr
         */
        val fr = Mockito.mock(DcMotorEx::class.java)
        /**
         * Documentation for bl
         */
        val bl = Mockito.mock(DcMotorEx::class.java)
        /**
         * Documentation for br
         */
        val br = Mockito.mock(DcMotorEx::class.java)
        /**
         * Documentation for pinpoint
         */
        val pinpoint = Mockito.mock(GoBildaPinpointDriver::class.java)
        /**
         * Documentation for limelight
         */
        val limelight = Mockito.mock(Limelight3A::class.java)
        /**
         * Documentation for voltageSensor
         */
        val voltageSensor = Mockito.mock(VoltageSensor::class.java)
        Mockito.`when`(voltageSensor.voltage).thenReturn(12.5)
        /**
         * Documentation for mockHardwareMap
         */

        val mockHardwareMap = Mockito.mock(HardwareMap::class.java)
        Mockito.`when`(mockHardwareMap.get(DcMotorEx::class.java, "fl")).thenReturn(fl)
        Mockito.`when`(mockHardwareMap.get(DcMotorEx::class.java, "fr")).thenReturn(fr)
        Mockito.`when`(mockHardwareMap.get(DcMotorEx::class.java, "rl")).thenReturn(bl)
        Mockito.`when`(mockHardwareMap.get(DcMotorEx::class.java, "rr")).thenReturn(br)
        Mockito.`when`(mockHardwareMap.get(GoBildaPinpointDriver::class.java, "pinpoint")).thenReturn(pinpoint)
        Mockito.`when`(mockHardwareMap.get(Limelight3A::class.java, "limelight")).thenReturn(limelight)
        
        @Suppress("UNCHECKED_CAST")
        Mockito.`when`(mockHardwareMap.getAll(VoltageSensor::class.java)).thenReturn(listOf(voltageSensor))
        /**
         * Documentation for mockTelemetry
         */

        val mockTelemetry = Mockito.mock(Telemetry::class.java)
        /**
         * Documentation for opMode
         */

        val opMode = object : AresTeleOpBase() {
            init {
                this.hardwareMap = mockHardwareMap
                this.telemetry = mockTelemetry
            }

            override fun define(): FtcTeleOpBuilder<org.firstinspires.ftc.teamcode.opmodes.AresRobot> {
                return aresTeleOp {
                    onInit { _, _ -> }
                    onLoop { _, _, _ -> }
                }
            }

            override fun updateRobot(robot: org.firstinspires.ftc.teamcode.opmodes.AresRobot, g1: GamepadState, g2: GamepadState) {
                if (killFlag) throw RuntimeException("Kill test thread")
                super.updateRobot(robot, g1, g2)
            }
        }

        // Configure gamepad inputs to trigger reset branch coverage (gamepad1.y = true)
        /**
         * Documentation for gamepad
         */
        val gamepad = Gamepad()
        gamepad.y = true
        opMode.gamepad1 = gamepad
        opMode.gamepad2 = Gamepad()

        // Get access to LinearOpMode private volatile field for lifecycle control
        /**
         * Documentation for linearOpModeClass
         */
        val linearOpModeClass = LinearOpMode::class.java
        /**
         * Documentation for userMonitoredForStartField
         */
        val userMonitoredForStartField = linearOpModeClass.getDeclaredField("userMonitoredForStart")
        userMonitoredForStartField.isAccessible = true

        userMonitoredForStartField.set(opMode, false)

        // 1. Start thread and let it enter opModeInInit()
        /**
         * Documentation for t
         */
        val t = thread {
            try {
                opMode.runOpMode()
            } catch (e: RuntimeException) {
                if (e.message != "Kill test thread") throw e
            }
        }
        Thread.sleep(500)

        try {
            // verify telemetry and state during init
            assertFalse(RobotStatusTracker.isEnabled)

            // 2. Transition to active (setting isStarted to true)
            try {
                /**
                 * Documentation for isStartedField
                 */
                val isStartedField = linearOpModeClass.superclass.getDeclaredField("isStarted")
                isStartedField.isAccessible = true
                isStartedField.set(opMode, true)
            } catch (e: Exception) {}
            Thread.sleep(150)
        } finally {
            // 3. Force loop to exit by throwing
            killFlag = true
            t.interrupt()
            t.join(5000)
            
            // 4. Force stop the default NT4 Server using reflection to prevent JVM leakage
            try {
                /**
                 * Documentation for instClass
                 */
                val instClass = Class.forName("org.frcforftc.networktables.NetworkTablesInstance")
                /**
                 * Documentation for getDefaultInstanceMethod
                 */
                val getDefaultInstanceMethod = instClass.getMethod("getDefaultInstance")
                /**
                 * Documentation for inst
                 */
                val inst = getDefaultInstanceMethod.invoke(null)
                /**
                 * Documentation for closeServerMethod
                 */
                val closeServerMethod = instClass.getMethod("closeServer")
                closeServerMethod.invoke(inst)
            } catch (_: Exception) {}
        }

        if (t.isAlive) {
            System.err.println("Diagnostic: Thread t is still alive! State: ${t.state}")
            for (ste in t.stackTrace) {
                System.err.println("   at $ste")
            }
            System.err.println("LinearOpMode fields:")
            for (field in LinearOpMode::class.java.declaredFields) {
                try {
                    field.isAccessible = true
                    System.err.println("  ${field.name} (${field.type}) = ${field.get(opMode)}")
                } catch (e: Exception) {
                    System.err.println("  ${field.name} (${field.type}) = <error>")
                }
            }
        }
        assertFalse("OpMode thread should have stopped and exited cleanly", t.isAlive)
    }
    /**
     * Documentation for testGamepadExtensionCoverage
     */

    @Test
    fun testGamepadExtensionCoverage() {
        /**
         * Documentation for gamepad
         */
        val gamepad = Gamepad()
        gamepad.left_stick_x = 0.1f
        gamepad.left_stick_y = 0.2f
        gamepad.right_stick_x = 0.3f
        gamepad.right_stick_y = 0.4f
        gamepad.left_trigger = 0.5f
        gamepad.right_trigger = 0.6f
        gamepad.a = true
        gamepad.b = false
        gamepad.x = true
        gamepad.y = false
        gamepad.dpad_up = true
        gamepad.dpad_down = false
        gamepad.dpad_left = true
        gamepad.dpad_right = false
        gamepad.left_bumper = true
        gamepad.right_bumper = false
        gamepad.left_stick_button = true
        gamepad.right_stick_button = false
        gamepad.start = true
        gamepad.back = false
        /**
         * Documentation for state
         */

        val state = gamepad.toState()
        assertEquals(0.1f, state.leftStickX, 1e-4f)
        assertEquals(0.2f, state.leftStickY, 1e-4f)
        assertEquals(0.3f, state.rightStickX, 1e-4f)
        assertEquals(0.4f, state.rightStickY, 1e-4f)
        assertEquals(0.5f, state.leftTrigger, 1e-4f)
        assertEquals(0.6f, state.rightTrigger, 1e-4f)
        assertTrue(state.a)
        assertFalse(state.b)
        assertTrue(state.x)
        assertFalse(state.y)
        assertTrue(state.dpadUp)
        assertFalse(state.dpadDown)
        assertTrue(state.dpadLeft)
        assertFalse(state.dpadRight)
        assertTrue(state.leftBumper)
        assertFalse(state.rightBumper)
        assertTrue(state.leftStickButton)
        assertFalse(state.rightStickButton)
        assertTrue(state.start)
        assertFalse(state.back)
        /**
         * Documentation for state2
         */

        val state2 = com.areslib.telemetry.GamepadState()
        state2.update(gamepad)
        assertEquals(0.1f, state2.leftStickX, 1e-4f)
    }
}

