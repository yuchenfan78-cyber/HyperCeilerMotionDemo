plugins {
    id("com.android.application")
}

android {
    namespace = "me.rain.motiondemo"
    compileSdk = 35

    defaultConfig {
        applicationId = "me.rain.motiondemo"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
