rootProject.name = "GasolineProject"

pluginManagement {
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

// Fase 2: módulos de core
include(":core:common")
include(":core:data")
include(":core:ui")
include(":core:testing")

// Fase 2: módulos de features
include(":feature:reservations:api")
include(":feature:trips")
include(":feature:profile")
include(":feature:reservations")
include(":feature:vehicles")
include(":feature:triprecording")
include(":feature:auth")
