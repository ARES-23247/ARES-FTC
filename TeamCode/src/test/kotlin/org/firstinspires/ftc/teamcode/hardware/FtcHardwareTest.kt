package org.firstinspires.ftc.teamcode.hardware

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito

class FtcHardwareTest {

    @Test
    fun testFtcFlywheelIO() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val hardwareMap = Mockito.mock(HardwareMap::class.java)
        Mockito.`when`(hardwareMap.get(DcMotorEx::class.java, "shooter")).thenReturn(mockMotor)

        val io = FtcFlywheelIO(hardwareMap)
        io.setVelocityRpm(3000.0)
        Mockito.verify(mockMotor).velocity = (3000.0 / 60.0) * 28.0

        io.setAppliedVoltage(6.0)
        Mockito.verify(mockMotor).power = 0.5

        Mockito.`when`(mockMotor.velocity).thenReturn(1400.0)
        Thread.sleep(150)
        assertEquals(3000.0, io.velocityRpm, 1e-6)

        assertEquals(0.0, io.currentAmps, 1e-6)
        assertEquals(0.0, io.tempCelsius, 1e-6)

        io.close()
        Mockito.verify(mockMotor, Mockito.atLeastOnce()).power = 0.0
    }

    @Test
    fun testFtcIntakeIO() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val hardwareMap = Mockito.mock(HardwareMap::class.java)
        Mockito.`when`(hardwareMap.get(DcMotorEx::class.java, "intake")).thenReturn(mockMotor)

        val io = FtcIntakeIO(hardwareMap)
        io.setRollerVoltage(6.0)
        Mockito.verify(mockMotor).power = 0.5

        io.setPivotAngle(45.0)
        io.setPivotVoltage(12.0)
        assertEquals(0.0, io.pivotAngleDegrees, 1e-6)
        assertEquals(0.0, io.pivotCurrentAmps, 1e-6)

        Mockito.`when`(mockMotor.getCurrent(org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit.AMPS)).thenReturn(2.5)
        Thread.sleep(150)
        assertEquals(2.5, io.rollerCurrentAmps, 1e-6)

        io.close()
        Mockito.verify(mockMotor, Mockito.atLeastOnce()).power = 0.0
    }
}
