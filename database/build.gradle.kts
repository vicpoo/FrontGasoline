/*
 * :database — esquema de la base local (SQLDelight): los .sq, las migraciones .sqm y el código que
 * genera SQLDelight (AppDatabase y sus consultas). Nada más: quién abre la base y la usa vive en
 * :data (LocalDatabase y los repositorios) y la conexión cifrada en :platform (SqlDriverFactory).
 *
 * Por qué es un módulo aparte y con target jvm() en vez de androidLibrary: el plugin de SQLDelight
 * (hasta la 2.4.0) no funciona con el plugin nuevo de librería KMP de Android
 * (com.android.kotlin.multiplatform.library): arma sus tareas antes de que existan las bases y el
 * compileSdk ("there are no databases set up" / "compileSdk version is not set"). Sin plugin de
 * Android usa el camino normal de Kotlin Multiplatform. El código generado es Kotlin puro, y Android
 * consume la variante jvm() de un módulo KMP sin problema.
 *
 * Después de cambiar el esquema (.sq / .sqm):
 *     ./gradlew :database:generateCommonMainAppDatabaseSchema   (genera databases/<versión>.db, se sube a git)
 *     ./gradlew :database:verifyCommonMainAppDatabaseMigration  (comprueba que las migraciones llegan al esquema actual)
 * Cada cambio de esquema es un archivo N.sqm (de la versión N a la N+1); nunca se edita un .sqm ya publicado.
 */
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvm {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.sqldelight.runtime)
        }
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("org.vicpoo.gasolineproject.database")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}
