pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.github.com/ReChronoRain/HyperCeiler") {
            credentials {
                username = System.getenv("MIUIX_USER")
                    ?: providers.gradleProperty("gpr.user").orNull
                    ?: "github"
                password = System.getenv("MIUIX_TOKEN")
                    ?: providers.gradleProperty("gpr.key").orNull
                    ?: ""
            }
            content { includeGroup("fan.miuix") }
        }
    }
}

rootProject.name = "RainKernel"
include(":app")
