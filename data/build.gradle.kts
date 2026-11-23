/*
 * :data — capa de datos: implementa las interfaces de :domain.
 * Red (Ktor + safeApiCall + renovación de la sesión), sesión y tokens (DataStore, Keystore / Keychain),
 * base local SQLDelight (AppDatabase, fase 6), cachés JSON en archivos (okio, se van pasando a la base),
 * colas offline-first y sincronizadores de cada feature.
 * Paquetes: org.vicpoo.gasolineproject.data.<feature> + local, network, session, push, di.
 */
plugins {
    id("gasoline.kmp.library")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

/*
 * Base local. Esquema actual en src/commonMain/sqldelight/.../*.sq; cada cambio de esquema es un
 * archivo N.sqm (de la versión N a la N+1). Después de cambiar el esquema:
 *     ./gradlew :data:generateCommonMainAppDatabaseSchema   (genera databases/<versión>.db, se sube a git)
 *     ./gradlew :data:verifyCommonMainAppDatabaseMigration  (comprueba que las migraciones llegan al esquema actual)
 */
sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("org.vicpoo.gasolineproject.data.database")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}

kotlin {
    androidLibrary {
        namespace = "org.vicpoo.gasolineproject.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
            api(libs.koin.core)
            api(libs.ktor.client.core)
            api(libs.datastore.preferences.core)
            api(libs.okio)
            api(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(project(":testing"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
