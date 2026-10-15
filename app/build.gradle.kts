plugins {
    id("com.android.application")
}

android {
    namespace = "com.sevtinge.hyperceiler.provision"
    compileSdk = 35

    defaultConfig {
        applicationId = "me.rain.motiondemo"
        minSdk = 33
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val syncUpstream = tasks.register<Exec>("syncUpstream") {
    workingDir = rootProject.projectDir
    commandLine("bash", "scripts/sync-origin.sh")
}

tasks.matching {
    it.name == "preBuild" ||
        it.name.startsWith("merge") && it.name.endsWith("Resources") ||
        it.name.startsWith("compile") && it.name.endsWith("JavaWithJavac")
}.configureEach {
    dependsOn(syncUpstream)
}
