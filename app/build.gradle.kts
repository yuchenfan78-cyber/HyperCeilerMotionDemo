plugins {
    id("com.android.application")
}

android {
    namespace = "me.rain.kernel"
    compileSdk = 36

    defaultConfig {
        applicationId = "me.rain.kernel"
        minSdk = 35
        targetSdk = 35
        versionCode = 4
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
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


dependencies {
    implementation("fan.miuix:appcompat:1.0.13.0")
    implementation("fan.miuix:animation:1.0.13.0")
    implementation("fan.miuix:folme:1.0.13.0")
}
