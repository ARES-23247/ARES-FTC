package org.firstinspires.ftc.teamcode.tools

import java.io.File
import java.util.Scanner

/**
 * Generates a seven-file starting point for an immutable Redux subsystem, hardware boundary,
 * desktop mock, and controller. Generated IO follows ARES lifecycle rules: one cached read pass
 * in `refresh()`, output-only setters, `HardwareRegistry` registration, and an explicit safe stop.
 *
 * To run: Use the "Run" button in Android Studio next to the `main` function.
 */
fun main() {
    val scanner = Scanner(System.`in`)
    print("Enter subsystem name (e.g., Climber, Intake, Shooter): ")
    val name = scanner.nextLine().trim()
    
    if (name.isEmpty()) {
        println("Error: Name cannot be empty.")
        return
    }
    val lowerName = name.replaceFirstChar { it.lowercase() }

    // Define the base package and output directory
    val basePackage = "org.firstinspires.ftc.teamcode"
    
    // Note: When running from Android Studio, the working directory is usually the root project.
    // We will resolve relative to TeamCode.
    val teamCodeDir = File("TeamCode/src/main/java/org/firstinspires/ftc/teamcode")
    if (!teamCodeDir.exists()) {
        println("Error: Could not find TeamCode directory at ${teamCodeDir.absolutePath}")
        println("Please ensure you are running this from the project root.")
        return
    }
    val stateDir = File(teamCodeDir, "state")
    val hardwareDir = File(teamCodeDir, "hardware")
    val controlDir = File(teamCodeDir, "control")
    
    listOf(stateDir, hardwareDir, controlDir).forEach { it.mkdirs() }

    println("Generating subsystem: $name")

    // 1. State
    val stateFile = File(stateDir, "${name}State.kt")
    stateFile.writeText("""
        package $basePackage.state

        import com.areslib.state.SubsystemState

        /**
         * Immutable Redux state for the $name subsystem.
         */
        data class ${name}State(
            /** Commanded mechanism position in the subsystem's documented physical unit. */
            val targetPosition: Double = 0.0,
            /** Last valid cached position observation in the same physical unit. */
            val currentPosition: Double = 0.0
        ) : SubsystemState
    """.trimIndent() + "\n")
    println("Created: ${stateFile.path}")

    // 2. Action
    val actionFile = File(stateDir, "${name}Action.kt")
    actionFile.writeText("""
        package $basePackage.state

        import com.areslib.action.RobotAction

        /**
         * Actions for transitioning the $name subsystem.
         */
        sealed class ${name}Action : RobotAction {
            data class SetTarget(val position: Double, override val timestampMs: Long = com.areslib.util.RobotClock.currentTimeMillis()) : ${name}Action()
            data class UpdateSensors(val position: Double, val currentAmps: Double, override val timestampMs: Long = com.areslib.util.RobotClock.currentTimeMillis()) : ${name}Action()
        }
    """.trimIndent() + "\n")
    println("Created: ${actionFile.path}")

    // 3. Reducer
    val reducerFile = File(stateDir, "${name}Reducer.kt")
    reducerFile.writeText("""
        package $basePackage.state

        /**
         * Pure reducer for the $name subsystem.
         */
        object ${name}Reducer {
            /** Returns a new state snapshot without hardware access or side effects. */
            fun reduce(state: ${name}State, action: ${name}Action): ${name}State {
                return when (action) {
                    is ${name}Action.SetTarget -> state.copy(targetPosition = action.position)
                    is ${name}Action.UpdateSensors -> state.copy(currentPosition = action.position)
                }
            }
        }
    """.trimIndent() + "\n")
    println("Created: ${reducerFile.path}")

    // 4. IO Interface
    val ioInterfaceFile = File(hardwareDir, "${name}IO.kt")
    ioInterfaceFile.writeText("""
        package $basePackage.hardware

        import com.areslib.hardware.SubsystemIO

        /**
         * Abstract hardware interface for $name.
         */
        interface ${name}IO : SubsystemIO, AutoCloseable {
            /** Cached position in the mechanism's documented physical unit. */
            val position: Double
            /** Last valid cached current observation in amperes. */
            val currentAmps: Double
            /** Commands actuator voltage; this method must not perform sensor reads. */
            fun setVoltage(voltage: Double)
        }
    """.trimIndent() + "\n")
    println("Created: ${ioInterfaceFile.path}")

    // 5. FTC Hardware IO
    val ftcIoFile = File(hardwareDir, "Ftc${name}IO.kt")
    ftcIoFile.writeText("""
        package $basePackage.hardware

        import com.qualcomm.robotcore.hardware.HardwareMap
        import com.qualcomm.robotcore.hardware.DcMotorEx
        /** FTC hardware boundary for the `$lowerName` device; all getters are cache-only. */
        class Ftc${name}IO(hardwareMap: HardwareMap) : ${name}IO {
            private val motor: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, "$lowerName")
            private var _cachedPosition = 0.0
            private var _cachedAmps = 0.0

            init {
                // Enables batch refresh, telemetry, emergency stop, and lifecycle close.
                com.areslib.hardware.HardwareRegistry.registerDevice("$name", this)
            }

            override val position: Double
                get() = _cachedPosition

            override val currentAmps: Double
                get() = _cachedAmps

            override fun refresh() {
                // Called once after REV bulk-cache clearing; never read hardware from a getter.
                _cachedPosition = try { motor.currentPosition.toDouble() } catch (_: Exception) { 0.0 }
                _cachedAmps = try {
                    motor.getCurrent(org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit.AMPS)
                        .takeIf { it.isFinite() } ?: 0.0
                } catch (_: Exception) {
                    0.0
                }
            }

            override fun setVoltage(voltage: Double) {
                val safeVoltage = voltage.takeIf { it.isFinite() } ?: 0.0
                motor.power = (safeVoltage / 12.0).coerceIn(-1.0, 1.0)
            }

            override fun safe() {
                motor.power = 0.0
            }

            override fun close() {
                safe()
            }
        }
    """.trimIndent() + "\n")
    println("Created: ${ftcIoFile.path}")

    // 6. Mock Hardware IO
    val mockIoFile = File(hardwareDir, "Mock${name}IO.kt")
    mockIoFile.writeText("""
        package $basePackage.hardware
        /** Deterministic desktop model implementing the same cached IO contract. */
        class Mock${name}IO : ${name}IO {
            override var position: Double = 0.0
                private set
            override var currentAmps: Double = 0.0
                private set
                
            private var activeVoltage = 0.0

            override fun refresh() {
                // Simulate simple physics step
                position += activeVoltage * 0.1
                currentAmps = kotlin.math.abs(activeVoltage * 0.5)
            }

            override fun setVoltage(voltage: Double) {
                activeVoltage = voltage.coerceIn(-12.0, 12.0)
            }

            override fun safe() {
                activeVoltage = 0.0
            }

            override fun close() {
                safe()
            }
        }
    """.trimIndent() + "\n")
    println("Created: ${mockIoFile.path}")

    // 7. Controller
    val controllerFile = File(controlDir, "${name}Controller.kt")
    controllerFile.writeText("""
        package $basePackage.control

        import $basePackage.state.${name}State
        import $basePackage.hardware.${name}IO

        class ${name}Controller(private val io: ${name}IO) {
            /** Computes output from immutable state; hardware observations are dispatched elsewhere. */
            fun update(state: ${name}State, dtSeconds: Double) {
                // Replace with a time-aware controller when this mechanism needs dynamics.
                val error = state.targetPosition - state.currentPosition
                val kP = 0.5
                val voltage = (error * kP).coerceIn(-12.0, 12.0)
                
                io.setVoltage(voltage)
            }
        }
    """.trimIndent() + "\n")
    println("Created: ${controllerFile.path}")

    println("\nSuccess! Subsystem $name has been generated.")
    println("IMPORTANT: Compose ${name}Reducer with the robot reducer and register both IO and subsystem during initialization.")
}
