package org.firstinspires.ftc.teamcode.dsl

import com.areslib.ftc.dsl.FtcTeleOpContext
import com.areslib.hardware.actuator.IndicatorLightColor
import org.firstinspires.ftc.teamcode.opmodes.AresRobot

/**
 * Season-layer control helpers written with Kotlin 2.4 context parameters.
 *
 * Each helper declares the TeleOp context it needs instead of taking it as an explicit
 * parameter. Inside `teleOp { controls { ... } }` blocks the context resolves implicitly from
 * the block's receiver — verified on the stable 2.4 subset in ContextParameterProbeTest — so
 * OpMode code calls these like local functions. No experimental compiler flags are involved,
 * and because these are internal to the season app they never appear in a published signature.
 */
context(ctx: FtcTeleOpContext<AresRobot>)
internal fun cyclePrimaryIndicator(forward: Boolean, index: Int): Int {
    val colors = IndicatorLightColor.entries
    val next = if (forward) (index + 1) % colors.size else (index - 1 + colors.size) % colors.size
    ctx.robot.setIndicatorColor(colors[next])
    return next
}

/** See [cyclePrimaryIndicator]; cycles the secondary indicator light. */
context(ctx: FtcTeleOpContext<AresRobot>)
internal fun cycleSecondaryIndicator(forward: Boolean, index: Int): Int {
    val colors = IndicatorLightColor.entries
    val next = if (forward) (index + 1) % colors.size else (index - 1 + colors.size) % colors.size
    ctx.robot.setSecondIndicatorColor(colors[next])
    return next
}
