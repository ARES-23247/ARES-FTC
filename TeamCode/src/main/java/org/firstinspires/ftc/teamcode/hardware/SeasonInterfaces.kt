package org.firstinspires.ftc.teamcode.hardware

import com.areslib.hardware.SubsystemIO
import com.areslib.telemetry.ITelemetry

/**
 * Pure abstraction for the deployed pivot-arm intake and active rollers.
 */
interface IntakeIO : SubsystemIO {
    override fun logTelemetry(telemetry: ITelemetry, prefix: String) {
        telemetry.putNumber("$prefix/PivotAngleDegrees", pivotAngleDegrees)
        telemetry.putNumber("$prefix/PivotCurrentAmps", pivotCurrentAmps)
        telemetry.putNumber("$prefix/RollerCurrentAmps", rollerCurrentAmps)
    }

    override fun safe() {
        setPivotVoltage(0.0)
        setRollerVoltage(0.0)
    }

    /** Sets the target absolute angle of the pivot arm in degrees */
    fun setPivotAngle(degrees: Double)

    /** Sets the applied voltage of the pivot motor directly (-12.0 to 12.0 volts) */
    fun setPivotVoltage(volts: Double)

    /** Sets the applied voltage of the intake rollers directly (-12.0 to 12.0 volts) */
    fun setRollerVoltage(volts: Double)

    /** Gets the current absolute angle of the pivot arm in degrees */
    val pivotAngleDegrees: Double
        get() = 0.0

    /** Gets the measured current of the pivot motor in Amperes */
    val pivotCurrentAmps: Double
        get() = 0.0

    /** Gets the measured current of the roller motor in Amperes */
    val rollerCurrentAmps: Double
        get() = 0.0
}

/**
 * Pure abstraction for reading and writing to the physical dual-motor Flywheel shooter.
 */
interface FlywheelIO : SubsystemIO {
    override fun logTelemetry(telemetry: ITelemetry, prefix: String) {
        telemetry.putNumber("$prefix/VelocityRpm", velocityRpm)
        telemetry.putNumber("$prefix/CurrentAmps", currentAmps)
        telemetry.putNumber("$prefix/TempCelsius", tempCelsius)
    }

    override fun safe() {
        setAppliedVoltage(0.0)
    }

    /** Sets the target velocity of the flywheel using closed-loop controller on the motor */
    fun setVelocityRpm(rpm: Double)

    /** Sets the applied voltage of the flywheel motors directly (-12.0 to 12.0 volts) */
    fun setAppliedVoltage(volts: Double)

    /** Gets the measured rotational velocity of the flywheel in RPM */
    val velocityRpm: Double
        get() = 0.0

    /** Gets the measured stator current of the flywheel motors in Amperes */
    val currentAmps: Double
        get() = 0.0

    /** Gets the temperature of the master motor in Celsius */
    val tempCelsius: Double
        get() = 0.0
}
