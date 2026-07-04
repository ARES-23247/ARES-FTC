plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation("com.areslib:core:1.0-SNAPSHOT")
    implementation("com.areslib:ftc-hardware:1.0-SNAPSHOT")
    implementation("com.areslib:simulator:1.0-SNAPSHOT")
}

sourceSets {
    main {
        java.srcDirs("../TeamCode/src/main/java")
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
