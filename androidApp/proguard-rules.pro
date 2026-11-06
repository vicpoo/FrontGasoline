# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# App de Combustible (Etapa 4: release con R8)
# La mayoría de las librerías traen sus propias reglas (kotlinx-serialization, OkHttp, Firebase,
# WorkManager, DataStore, MapLibre). Aquí solo va lo que falta.
# ---------------------------------------------------------------------------

# Números de línea en los errores de release (con el mapping.txt se ven los nombres reales)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Ktor: clases opcionales que solo existen en la JVM de escritorio
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn io.ktor.util.debug.**

# Ktor: el motor que usa Coil para descargar fotos se busca con ServiceLoader
-keep class * implements io.ktor.client.HttpClientEngineContainer

# kotlinx-serialization: DTOs, rutas de navegación y lo que se guarda en el teléfono
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class org.vicpoo.gasolineproject.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class org.vicpoo.gasolineproject.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# WorkManager crea el worker por su nombre de clase
-keep class org.vicpoo.gasolineproject.platform.sync.PendingSyncWorker { <init>(...); }
