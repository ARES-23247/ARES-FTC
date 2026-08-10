package org.firstinspires.ftc.teamcode

import com.areslib.Store
import com.areslib.auto.AresAutoCodec
import com.areslib.auto.AutoPose
import com.areslib.auto.AutoRoutine
import com.areslib.auto.AutoRoutineCompiler
import com.areslib.auto.AutoStep
import com.areslib.math.coordinate.CoordinateTransformers
import com.areslib.math.geometry.Pose2d
import com.areslib.pathing.DriveModel
import com.areslib.pathing.HolonomicPathFollower
import com.areslib.pathing.JerkLimitedTrajectoryProvider
import com.areslib.pathing.NamedCommandDescriptor
import com.areslib.pathing.NamedCommands
import com.areslib.pathing.TrajectoryLimits
import com.areslib.pathing.TrajectoryPlanner
import com.areslib.state.RobotState
import com.areslib.subsystem.DrivetrainSubsystem
import com.google.gson.JsonParser
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.firstinspires.ftc.teamcode.dsl.FtcAutoCapabilities
import java.io.File

/** Guards the checked-in GUI assets against the capabilities and compiler shipped to the robot. */
class AutoAssetContractTest {
    private lateinit var projectRoot: File

    @Before
    fun registerRobotCapabilities() {
        projectRoot = findProjectRoot()
        NamedCommands.clear()
        FtcAutoCapabilities.register()
        FtcAutoCapabilities.registerIndicatorActions(
            primaryAvailable = true,
            secondaryAvailable = true
        )
    }

    @After
    fun clearRobotCapabilities() {
        NamedCommands.clear()
    }

    @Test
    fun `manifest exactly matches commands registered by season robot`() {
        val manifest = manifestDescriptors()
        val declared = FtcAutoCapabilities.descriptors.associateBy { it.key.value }
        val registered = NamedCommands.catalog().associateBy { it.key.value }

        assertEquals("editor manifest drifted from source-owned action metadata", declared, manifest)
        assertEquals("runtime registry drifted from the editor manifest", manifest, registered)
    }

    @Test
    fun `every deployed native auto is legal resolvable and compilable`() {
        val autosDirectory = File(projectRoot, "TeamCode/src/main/assets/ares/autos")
        val files = autosDirectory.listFiles { file -> file.isFile && file.extension == "aresauto" }
            .orEmpty()
            .sortedBy(File::getName)
        assertTrue("at least one deployable native auto is required", files.isNotEmpty())

        val advertisedKeys = FtcAutoCapabilities.descriptors.map { it.key.value }.toSet()
        files.forEach { file ->
            val routine = AresAutoCodec.decode(file.readText())
            assertEquals(file.nameWithoutExtension, routine.documentId)
            assertTrue(
                "${file.name} references an action absent from auto-capabilities.json",
                referencedCommands(routine).all(advertisedKeys::contains)
            )
            assertTrue(
                "${file.name} places a pose outside the FTC field",
                allPoses(routine).all(::insideFtcField)
            )

            val compilation = compiler().compile(routine)
            assertTrue(
                "${file.name} failed robot-side compilation: " +
                    compilation.issues.joinToString { it.message },
                compilation.isSuccess
            )
        }
    }

    private fun manifestDescriptors(): Map<String, NamedCommandDescriptor> {
        val file = File(projectRoot, "TeamCode/src/main/assets/ares/auto-capabilities.json")
        val actions = JsonParser().parse(file.readText()).asJsonObject
            .getAsJsonArray("actions")
        return actions.associate { element ->
            val action = element.asJsonObject
            val key = action.get("key").asString
            key to NamedCommandDescriptor(
                key = com.areslib.pathing.CommandKey(key),
                displayName = action.get("displayName").asString,
                description = action.get("description").asString,
                category = action.get("category").asString
            )
        }
    }

    private fun compiler() = AutoRoutineCompiler(
        trajectoryPlanner = TrajectoryPlanner(listOf(JerkLimitedTrajectoryProvider)),
        follower = HolonomicPathFollower(ContractDrivetrain()),
        driveModel = DriveModel.MECANUM,
        limitsForPreset = {
            TrajectoryLimits(
                maxVelocityMps = 1.5,
                maxAccelerationMps2 = 1.5,
                maxJerkMps3 = 6.0,
                maxCentripetalAccelerationMps2 = 1.5,
                maxAngularVelocityRps = 2.0,
                maxAngularAccelerationRps2 = 3.0
            )
        }
    )

    private fun referencedCommands(routine: AutoRoutine): Set<String> = buildSet {
        fun visit(step: AutoStep) {
            step.commandKey?.let(::add)
            step.drive?.let { drive ->
                addAll(drive.duringCommands)
                addAll(drive.arrivalCommands)
                drive.markers.forEach { marker -> add(marker.commandKey) }
            }
            step.children.forEach(::visit)
        }
        routine.steps.forEach(::visit)
    }

    private fun allPoses(routine: AutoRoutine): List<AutoPose> = buildList {
        add(routine.startingPose)
        fun visit(step: AutoStep) {
            step.drive?.target?.let(::add)
            step.children.forEach(::visit)
        }
        routine.steps.forEach(::visit)
    }

    private fun insideFtcField(pose: AutoPose): Boolean {
        val halfField = CoordinateTransformers.FTC_FIELD_SIZE / 2.0
        return pose.xMeters in -halfField..halfField && pose.yMeters in -halfField..halfField
    }

    private fun findProjectRoot(): File = generateSequence(
        File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
    ) {
        it.parentFile
    }.firstOrNull { candidate ->
        File(candidate, "TeamCode/src/main/assets/ares/auto-capabilities.json").isFile
    } ?: error("Could not locate ARES-FTC project root from ${System.getProperty("user.dir")}")

    private class ContractDrivetrain : DrivetrainSubsystem {
        override fun setChassisSpeeds(vx: Double, vy: Double, omega: Double) = Unit
        override fun getEstimatedPose(): Pose2d = Pose2d()
        override fun readSensors(store: Store, timestampMs: Long) = Unit
        override fun writeOutputs(state: RobotState, scale: Double) = Unit
    }
}
