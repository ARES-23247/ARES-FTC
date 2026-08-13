package org.firstinspires.ftc.teamcode.config

import com.qualcomm.robotcore.hardware.DcMotorSimple
import org.firstinspires.ftc.teamcode.generated.drivebase.GeneratedAresDrivebaseConfig
import org.firstinspires.ftc.teamcode.generated.drivebase.GeneratedAresTuningConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalDrivebaseConfigTest {
    @Test
    fun `generated profile initializes hardware and Redux from one baseline`() {
        val tuning = CanonicalDrivebaseConfig.initialTuningState()

        assertEquals(GeneratedAresDrivebaseConfig.TRACK_WIDTH_METERS, tuning.drive.trackWidthMeters, 0.0)
        assertEquals(GeneratedAresDrivebaseConfig.WHEEL_BASE_METERS, tuning.drive.wheelBaseMeters, 0.0)
        assertEquals(GeneratedAresTuningConfig.Parameters.DRIVE_TICKSPERMETER, tuning.drive.ftc.ticksPerMeter, 0.0)
        assertEquals(GeneratedAresTuningConfig.Parameters.DRIVE_FEEDFORWARDKV, tuning.drive.driveFeedforward.kV, 0.0)
        assertEquals(DcMotorSimple.Direction.FORWARD, CanonicalDrivebaseConfig.frontLeftDirection)
        assertEquals(DcMotorSimple.Direction.REVERSE, CanonicalDrivebaseConfig.frontRightDirection)
        assertFalse(GeneratedAresTuningConfig.Parameters.DRIVE_CLOSEDLOOPVELOCITY)
        assertTrue(GeneratedAresTuningConfig.Parameters.LOCALIZATION_PINPOINTCCWPOSITIVE)
    }

    @Test
    fun `generated safety and simulation contract stays explicit`() {
        assertTrue(GeneratedAresDrivebaseConfig.CURRENT_VALIDITY_REQUIRED)
        assertEquals("BRAKE", GeneratedAresDrivebaseConfig.ENABLED_NEUTRAL_MODE)
        assertEquals("FORCE_NEUTRAL_BRAKE", GeneratedAresDrivebaseConfig.DISABLED_POLICY)
        assertEquals(2000.0, GeneratedAresTuningConfig.Parameters.DRIVE_TICKSPERMETER, 0.0)
    }

    @Test
    fun `runtime updates reject constructor only and unknown parameters`() {
        assertTrue(CanonicalDrivebaseConfig.supportsRuntimeParameter("ftc.drive.heading.kp"))
        assertFalse(CanonicalDrivebaseConfig.supportsRuntimeParameter("ftc.drive.closed-loop-velocity"))
        assertFalse(CanonicalDrivebaseConfig.supportsRuntimeParameter("future.unmapped.parameter"))
    }
}
