package org.firstinspires.ftc.teamcode.opmodes.robot

import com.areslib.action.RobotAction
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.hardware.actuator.IndicatorLightColor

class AresTelemetryHelper(private val base: FtcMecanumRobot) {
    /**
     * Documentation for addTelemetry
     */
    fun addTelemetry(key: String, value: Any) {
        base.telemetryManager.customDriverStationText[key] = value.toString()
    }
    /**
     * Documentation for setIndicatorColor
     */

    fun setIndicatorColor(color: IndicatorLightColor) {
        base.store.dispatch(RobotAction.SetIndicatorLight("indicator", color.position))
    }
}
