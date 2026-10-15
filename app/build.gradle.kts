plugins {
    id("com.android.application")
}

android {
    namespace = "me.rain.kernel"
    compileSdk = 35

    defaultConfig {
        applicationId = "me.rain.kernel"
        minSdk = 33
        targetSdk = 35
        versionCode = 3
        versionName = "3.0"
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
