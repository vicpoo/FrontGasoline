rootProject.name = "GasolineProject"

pluginManagement {
    // Plugins de convención: gasoline.kmp.library, gasoline.kmp.compose
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":shared")

// Etapa 2: un módulo por capa; cada capa tiene una carpeta por feature
include(":domain")
include(":data")
include(":platform")
include(":ui")
include(":testing")
