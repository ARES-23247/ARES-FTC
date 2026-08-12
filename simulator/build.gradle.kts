plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation("com.github.ARES-23247.ARESLib-Kotlin:core:master-SNAPSHOT")
    implementation("com.github.ARES-23247.ARESLib-Kotlin:ftc-hardware:master-SNAPSHOT")
    implementation("com.github.ARES-23247.ARESLib-Kotlin:simulator:master-SNAPSHOT")
    implementation("com.github.ARES-23247.ARESLib-Kotlin:ftc-mocks:master-SNAPSHOT")
    
    val wpiVersion = "2024.3.2"
    implementation("edu.wpi.first.ntcore:ntcore-java:$wpiVersion")
    implementation("edu.wpi.first.wpilibj:wpilibj-java:$wpiVersion")
    implementation("edu.wpi.first.wpiutil:wpiutil-java:$wpiVersion")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

sourceSets {
    main {
        java.srcDirs(
            "../TeamCode/src/main/java",
            "../TeamCode/build/generated/ares/main/kotlin",
            "src/main/kotlin",
        )
    }
}

kotlin {
    jvmToolchain(21)
}

val javaToolchains = project.extensions.getByType<JavaToolchainService>()

// The simulator compiles the real editable adapters plus the same disposable registration source
// as the Android app. It must never grow a simulator-only wiring path.
tasks.named("compileKotlin") {
    dependsOn(":TeamCode:generateAresProject")
}

tasks.named<JavaExec>("run") {
    group = "application"
    mainClass.set("com.areslib.sim.DesktopSimLauncher")
    classpath = sourceSets.main.get().runtimeClasspath
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
    
    val argsList = mutableListOf<String>()
    if (project.hasProperty("appArgs")) {
        argsList.addAll(project.property("appArgs").toString().split(" "))
    }
    args(argsList)
}

tasks.register<JavaExec>("runCalibrationVerification") {
    group = "application"
    mainClass.set("org.firstinspires.ftc.teamcode.CalibrationVerificationAppKt")
    classpath = sourceSets.main.get().runtimeClasspath
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}
