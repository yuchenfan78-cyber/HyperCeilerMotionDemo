plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "me.rain.kernel"
    compileSdk = 36

    defaultConfig {
        applicationId = "me.rain.kernel"
        minSdk = 35
        targetSdk = 35
        versionCode = 6
        versionName = "0.3.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
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
    implementation("androidx.activity:activity:1.12.1")
    implementation("androidx.fragment:fragment:1.8.9")
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    implementation("top.yukonga.miuix.kmp:miuix-android:0.7.2")
    implementation("androidx.compose.ui:ui:1.9.3")
    implementation("androidx.compose.runtime:runtime:1.9.3")
    implementation("androidx.compose.animation:animation-core:1.9.3")
}
