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
import org.firstinspires.ftc.teamcode.dsl.AresTeleOpBuilder
import org.firstinspires.ftc.teamcode.dsl.toState
import org.firstinspires.ftc.teamcode.dsl.update
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito
import kotlin.concurrent.thread

class AresTeleOpBaseTest {

    @Test
    fun testAresTeleOpBaseLifecycle() {
        val fl = Mockito.mock(DcMotorEx::class.java)
        val fr = Mockito.mock(DcMotorEx::class.java)
        val bl = Mockito.mock(DcMotorEx::class.java)
        val br = Mockito.mock(DcMotorEx::class.java)
        val pinpoint = Mockito.mock(GoBildaPinpointDriver::class.java)
        val limelight = Mockito.mock(Limelight3A::class.java)
        val voltageSensor = Mockito.mock(VoltageSensor::class.java)
        Mockito.`when`(voltageSensor.voltage).thenReturn(12.5)

        val mockHardwareMap = Mockito.mock(HardwareMap::class.java)
        Mockito.`when`(mockHardwareMap.get(DcMotorEx::class.java, "fl")).thenReturn(fl)
        Mockito.`when`(mockHardwareMap.get(DcMotorEx::class.java, "fr")).thenReturn(fr)
        Mockito.`when`(mockHardwareMap.get(DcMotorEx::class.java, "rl")).thenReturn(bl)
        Mockito.`when`(mockHardwareMap.get(DcMotorEx::class.java, "rr")).thenReturn(br)
        Mockito.`when`(mockHardwareMap.get(GoBildaPinpointDriver::class.java, "pinpoint")).thenReturn(pinpoint)
        Mockito.`when`(mockHardwareMap.get(Limelight3A::class.java, "limelight")).thenReturn(limelight)
        
        @Suppress("UNCHECKED_CAST")
        Mockito.`when`(mockHardwareMap.getAll(VoltageSensor::class.java)).thenReturn(listOf(voltageSensor))

        val mockTelemetry = Mockito.mock(Telemetry::class.java)

        val opMode = object : AresTeleOpBase() {
            init {
                this.hardwareMap = mockHardwareMap
                this.telemetry = mockTelemetry
            }

            override fun define(): AresTeleOpBuilder {
                return aresTeleOp {
                    onInit { _, _ -> }
                    onLoop { _, _, _ -> }
                }
            }
        }

        // Configure gamepad inputs to trigger reset branch coverage (gamepad1.y = true)
        val gamepad = Gamepad()
        gamepad.y = true
        opMode.gamepad1 = gamepad
        opMode.gamepad2 = Gamepad()

        // Get access to LinearOpMode private volatile field for lifecycle control
        val linearOpModeClass = LinearOpMode::class.java
        val userMonitoredForStartField = linearOpModeClass.getDeclaredField("userMonitoredForStart")
        userMonitoredForStartField.isAccessible = true

        userMonitoredForStartField.set(opMode, false)

        val t = thread {
            opMode.runOpMode()
        }

        try {
            // 1. Let init run for a bit
            Thread.sleep(150)
            
            // 2. Transition to active (setting userMonitoredForStart to true starts the opMode)
            userMonitoredForStartField.set(opMode, true)
            Thread.sleep(150)
        } finally {
            // 3. Force loop to exit by setting userMonitoredForStart back to false
            userMonitoredForStartField.set(opMode, false)
            t.interrupt()
            t.join(2000)
            
            // 4. Force stop the default NT4 Server using reflection to prevent JVM leakage
            try {
                val instClass = Class.forName("org.frcforftc.networktables.NetworkTablesInstance")
                val getDefaultInstanceMethod = instClass.getMethod("getDefaultInstance")
                val inst = getDefaultInstanceMethod.invoke(null)
                val closeServerMethod = instClass.getMethod("closeServer")
                closeServerMethod.invoke(inst)
            } catch (_: Exception) {}
        }

        assertFalse("OpMode thread should have stopped and exited cleanly", t.isAlive)
    }

    @Test
    fun testGamepadExtensionCoverage() {
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

        val state2 = com.areslib.telemetry.GamepadState()
        state2.update(gamepad)
        assertEquals(0.1f, state2.leftStickX, 1e-4f)
    }
}
