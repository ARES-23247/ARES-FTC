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
}

sourceSets {
    main {
        java.srcDirs("../TeamCode/src/main/java", "src/main/kotlin")
    }
}

kotlin {
    jvmToolchain(21)
}

val javaToolchains = project.extensions.getByType<JavaToolchainService>()

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
