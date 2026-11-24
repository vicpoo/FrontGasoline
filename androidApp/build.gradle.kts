import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    // Firebase: lee google-services.json (notificaciones push)
    alias(libs.plugins.googleServices)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

/*
 * Firma del APK / AAB de release (punto 3.1.6). Los datos salen de keystore.properties en la raíz del
 * proyecto (NO se versiona; ver keystore.properties.example). El keystore (.jks) vive fuera del repo.
 * Sin ese archivo el release se arma sin firmar (sirve para revisar R8, no para instalar ni publicar).
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
dependencies {
    implementation(project(":shared"))
    implementation(project(":domain")) // MainActivity usa PushEvents
    implementation(project(":platform")) // MainActivity y GasolineApplication usan PushNotifications

    implementation(libs.androidx.activity.compose)
    // Pantalla de inicio con el logo (Android 12+ y versiones anteriores)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.koin.android)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "org.vicpoo.gasolineproject"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.vicpoo.gasolineproject"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        // URL de APIGasoline (termina en "/"). La misma en debug y release; si algún día cambia por
        // ambiente, se define dentro de cada buildType. Llega a Koin desde GasolineApplication.
        // Si cambias el host, agrégalo también a res/xml/network_security_config.xml.
        buildConfigField("String", "API_BASE_URL", "\"http://13.222.72.73/api/\"")
    }
    signingConfigs {
        if (!keystoreProperties.isEmpty) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
    buildTypes {
        release {
            // R8: quita el código y los recursos que no se usan y optimiza el resto (APK más chico y rápido).
            // Si algo falla SOLO en release, revisa logcat y agrega la regla en proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true // BuildConfig.API_BASE_URL
    }
}
