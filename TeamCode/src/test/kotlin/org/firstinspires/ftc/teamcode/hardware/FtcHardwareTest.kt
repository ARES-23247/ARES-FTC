package org.firstinspires.ftc.teamcode.hardware

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.VoltageSensor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito

class FtcHardwareTest {

    private fun createMockHardwareMap(motorName: String, mockMotor: DcMotorEx): HardwareMap {
        val hardwareMap = Mockito.mock(HardwareMap::class.java)
        Mockito.`when`(hardwareMap.get(DcMotorEx::class.java, motorName)).thenReturn(mockMotor)

        val mockVoltageSensor = Mockito.mock(VoltageSensor::class.java)
        Mockito.`when`(mockVoltageSensor.voltage).thenReturn(12.0)

        @Suppress("UNCHECKED_CAST")
        val deviceMapping = Mockito.mock(HardwareMap.DeviceMapping::class.java) as HardwareMap.DeviceMapping<VoltageSensor>
        Mockito.`when`(deviceMapping.iterator()).thenAnswer { listOf(mockVoltageSensor).iterator() }

        val mockServo = Mockito.mock(Servo::class.java)
        @Suppress("UNCHECKED_CAST")
        val servoMapping = Mockito.mock(HardwareMap.DeviceMapping::class.java) as HardwareMap.DeviceMapping<Servo>
        Mockito.`when`(servoMapping.get(Mockito.anyString())).thenReturn(mockServo)

        runCatching {
            HardwareMap::class.java.getField("voltageSensor").set(hardwareMap, deviceMapping)
            HardwareMap::class.java.getField("servo").set(hardwareMap, servoMapping)
        }

        return hardwareMap
    }

    @Test
    fun testFtcFlywheelIO() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val hardwareMap = createMockHardwareMap("shooter", mockMotor)
        Mockito.`when`(mockMotor.velocity).thenReturn(1400.0)

        val io = FtcFlywheelIO(hardwareMap)
        io.setVelocityRpm(3000.0)
        Mockito.verify(mockMotor).velocity = (3000.0 / 60.0) * 28.0

        io.setAppliedVoltage(6.0)
        Mockito.verify(mockMotor).power = 0.5

        io.refresh()
        assertEquals(3000.0, io.velocityRpm, 1e-6)

        assertEquals(0.0, io.currentAmps, 1e-6)
        assertEquals(0.0, io.tempCelsius, 1e-6)

        io.close()
        Mockito.verify(mockMotor, Mockito.atLeastOnce()).power = 0.0
    }

    @Test
    fun testFtcIntakeIO() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val hardwareMap = createMockHardwareMap("intake", mockMotor)

        val io = FtcIntakeIO(hardwareMap)
        io.setRollerVoltage(12.0)
        Mockito.verify(mockMotor).power = 1.0

        io.close()
        Mockito.verify(mockMotor, Mockito.atLeastOnce()).power = 0.0
    }
}
