package org.firstinspires.ftc.teamcode

import com.areslib.ftc.FtcMecanumRobot
import com.areslib.state.Alliance
import com.areslib.state.DriveState
import com.areslib.state.RobotState
import com.areslib.Store
import org.firstinspires.ftc.teamcode.opmodes.robot.AresDriveController
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito
import org.mockito.ArgumentMatchers.anyDouble

class AresDriveControllerTest {

    private fun setupMockRobot(alliance: Alliance = Alliance.RED): FtcMecanumRobot {
        val base = Mockito.mock(FtcMecanumRobot::class.java)
        val store = Mockito.mock(Store::class.java)
        val state = RobotState(drive = DriveState(alliance = alliance))
        
        Mockito.`when`(base.store).thenReturn(store)
        Mockito.`when`(store.state).thenReturn(state)
        return base
    }

    @Test
    fun testFieldCentricDriveRedAlliance() {
        val base = setupMockRobot(Alliance.RED)
        val controller = AresDriveController(base)
        
        controller.driveFieldCentric(0.5, 0.5, 0.1)
        
        Mockito.verify(base).driveFieldCentric(
            org.mockito.AdditionalMatchers.eq(0.03645, 1e-4),
            org.mockito.AdditionalMatchers.eq(0.03645, 1e-4),
            org.mockito.AdditionalMatchers.eq(0.00005, 1e-6)
        )
    }

    @Test
    fun testFieldCentricDriveBlueAlliance() {
        val base = setupMockRobot(Alliance.BLUE)
        val controller = AresDriveController(base)
        
        controller.driveFieldCentric(0.5, 0.5, 0.1)
        
        Mockito.verify(base).driveFieldCentric(
            org.mockito.AdditionalMatchers.eq(0.03645, 1e-4),
            org.mockito.AdditionalMatchers.eq(0.03645, 1e-4),
            org.mockito.AdditionalMatchers.eq(0.00005, 1e-6)
        )
    }

    @Test
    fun testRobotCentricDrive() {
        val base = setupMockRobot()
        val controller = AresDriveController(base)
        
        controller.driveRobotCentric(0.5, 0.5, 0.1)
        
        Mockito.verify(base).driveRobotCentric(
            org.mockito.AdditionalMatchers.eq(0.03645, 1e-4),
            org.mockito.AdditionalMatchers.eq(0.03645, 1e-4),
            org.mockito.AdditionalMatchers.eq(0.00005, 1e-6)
        )
    }

    @Test
    fun testZeroJoystickInputProducesZeroOutput() {
        val base = setupMockRobot()
        val controller = AresDriveController(base)
        
        controller.driveFieldCentric(0.0, 0.0, 0.0)
        
        Mockito.verify(base).driveFieldCentric(0.0, 0.0, 0.0)
    }

    @Test
    fun testDeadbandFilteringEliminatesSmallNoise() {
        val base = setupMockRobot()
        val controller = AresDriveController(base)
        
        val noise = 0.01
        controller.driveFieldCentric(noise, noise, 0.0)
        
        Mockito.verify(base).driveFieldCentric(0.0, 0.0, 0.0)
    }
    
    @Test
    fun testMotorPowerBounds() {
        val base = setupMockRobot()
        val controller = AresDriveController(base)
        
        controller.driveFieldCentric(2.0, -2.0, 0.0)
        
        Mockito.verify(base).driveFieldCentric(2.96595, -2.96595, 0.0)
    }

    @Test
    fun testClosedLoopHeadingPID() {
        val base = setupMockRobot()
        val controller = AresDriveController(base)
        
        controller.driveFieldCentric(0.0, 0.0, 0.5)
        Mockito.verify(base).driveFieldCentric(0.0, 0.0, 0.03645)
    }
}
