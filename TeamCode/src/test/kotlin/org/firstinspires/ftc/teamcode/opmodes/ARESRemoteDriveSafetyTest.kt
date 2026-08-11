package org.firstinspires.ftc.teamcode.opmodes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ARESRemoteDriveSafetyTest {

    @Test
    fun retainedNonzeroAtomicFrameCannotArmStartup() {
        val gate = RemoteDriveFrameGate(timeoutMs = 1_000L)

        assertFalse(gate.observe(frame(session = 41, sequence = 9, vx = 1.5), 5_000L, 4.0, 8.0))
        assertFalse(gate.motionAuthorized)
        assertEquals(0.0, gate.vx, 0.0)
        assertFalse(gate.observe(frame(session = 41, sequence = 9, vx = 1.5), 5_100L, 4.0, 8.0))

        assertTrue(gate.observe(frame(session = 41, sequence = 10), 5_101L, 4.0, 8.0))
        assertFalse("The neutral handshake frame itself must never authorize motion", gate.motionAuthorized)
        assertTrue(gate.observe(frame(session = 41, sequence = 11, vx = 1.5), 5_102L, 4.0, 8.0))
        assertTrue(gate.motionAuthorized)
        assertEquals(1.5, gate.vx, 0.0)
    }

    @Test
    fun heartbeatFirstReconnectCannotReplayRetainedAxes() {
        val gate = RemoteDriveFrameGate(timeoutMs = 1_000L)

        assertTrue(gate.observe(frame(session = 7, sequence = 1), 1_000L, 4.0, 8.0))
        assertTrue(gate.observe(frame(session = 7, sequence = 2, vx = 2.0), 1_010L, 4.0, 8.0))
        assertTrue(gate.motionAuthorized)

        // The legacy heartbeat may already be changing after reconnect, but the atomic topic still
        // contains the old nonzero frame. Re-reading it at lease expiry must hard-zero and disarm.
        assertFalse(gate.observe(frame(session = 7, sequence = 2, vx = 2.0), 2_010L, 4.0, 8.0))
        assertFalse(gate.motionAuthorized)
        assertEquals(0.0, gate.vx, 0.0)
        assertFalse(gate.observe(frame(session = 7, sequence = 2, vx = 2.0), 2_011L, 4.0, 8.0))

        // A reconnecting publisher also cannot begin a new session with a nonzero command.
        assertFalse(gate.observe(frame(session = 8, sequence = 0, vx = 3.0), 2_012L, 4.0, 8.0))
        assertTrue(gate.observe(frame(session = 8, sequence = 1), 2_013L, 4.0, 8.0))
        assertFalse(gate.motionAuthorized)
        assertTrue(gate.observe(frame(session = 8, sequence = 2, vx = 0.75), 2_014L, 4.0, 8.0))
        assertEquals(0.75, gate.vx, 0.0)
    }

    @Test
    fun readFailureAfterArmingHardZerosAndRequiresAnotherNeutralHandshake() {
        val gate = RemoteDriveFrameGate(timeoutMs = 1_000L)

        assertTrue(gate.observe(frame(session = 12, sequence = 1), 100L, 4.0, 8.0))
        assertTrue(gate.observe(frame(session = 12, sequence = 2, omega = 2.5), 110L, 4.0, 8.0))
        assertTrue(gate.motionAuthorized)

        assertFalse(gate.observe(null, 120L, 4.0, 8.0))
        assertFalse(gate.motionAuthorized)
        assertEquals(0.0, gate.omega, 0.0)

        assertFalse(gate.observe(frame(session = 12, sequence = 3, omega = 2.5), 130L, 4.0, 8.0))
        assertTrue(gate.observe(frame(session = 12, sequence = 4), 140L, 4.0, 8.0))
        assertFalse(gate.motionAuthorized)
        assertTrue(gate.observe(frame(session = 12, sequence = 5, omega = 1.0), 150L, 4.0, 8.0))
    }

    @Test
    fun replayedOutOfOrderAndMalformedFramesFailClosed() {
        val gate = RemoteDriveFrameGate(timeoutMs = 1_000L)

        assertTrue(gate.observe(frame(session = 3, sequence = 10), 1_000L, 4.0, 8.0))
        assertTrue(gate.observe(frame(session = 3, sequence = 11, vy = -1.0), 1_010L, 4.0, 8.0))
        assertTrue(gate.motionAuthorized)

        // Repeats may hold the last coherent command only for the existing receiver-side lease.
        assertTrue(gate.observe(frame(session = 3, sequence = 11, vy = -1.0), 1_500L, 4.0, 8.0))
        assertEquals(-1.0, gate.vy, 0.0)
        assertFalse(gate.observe(frame(session = 3, sequence = 10, vy = 2.0), 1_501L, 4.0, 8.0))
        assertEquals(0.0, gate.vy, 0.0)

        assertFalse(gate.observe(doubleArrayOf(1.0, 3.0), 1_502L, 4.0, 8.0))
        assertFalse(gate.observe(frame(session = 3, sequence = 12, vx = Double.NaN), 1_503L, 4.0, 8.0))
        assertFalse(gate.observe(frame(session = 3, sequence = 13, vx = 4.1), 1_504L, 4.0, 8.0))
    }

    @Test
    fun clockRollbackAndInvalidProtocolMetadataDisarm() {
        val gate = RemoteDriveFrameGate(timeoutMs = 1_000L)

        assertTrue(gate.observe(frame(session = 1, sequence = 1), 1_000L, 4.0, 8.0))
        assertTrue(gate.observe(frame(session = 1, sequence = 2, vx = 1.0), 1_100L, 4.0, 8.0))
        assertFalse(gate.observe(frame(session = 1, sequence = 2, vx = 1.0), 1_050L, 4.0, 8.0))
        assertFalse(gate.motionAuthorized)

        val wrongVersion = frame(session = 2, sequence = 1).also { it[0] = 2.0 }
        assertFalse(gate.observe(wrongVersion, 1_051L, 4.0, 8.0))
        val fractionalSequence = frame(session = 2, sequence = 1).also { it[2] = 1.5 }
        assertFalse(gate.observe(fractionalSequence, 1_052L, 4.0, 8.0))
    }

    private fun frame(
        session: Long,
        sequence: Long,
        vx: Double = 0.0,
        vy: Double = 0.0,
        omega: Double = 0.0
    ): DoubleArray = doubleArrayOf(
        1.0,
        session.toDouble(),
        sequence.toDouble(),
        1_780_000_000_000.0 + sequence,
        vx,
        vy,
        omega
    )
}
