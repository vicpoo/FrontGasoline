package org.vicpoo.gasolineproject.platform.location

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import org.vicpoo.gasolineproject.domain.location.TrackingServiceController

/**
 * Servicio en primer plano del viaje en curso.
 * Android pide que una app que usa el GPS en segundo plano muestre una notificación;
 * mientras este servicio esté vivo, el proceso (y el TripTracker) siguen recibiendo ubicaciones.
 */
class TripTrackingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        createChannel()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        runCatching {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(this, gpsLost = false), type)
        }.onFailure {
            // Sin permiso de ubicación Android no deja iniciar el servicio: el viaje sigue en pantalla
            stopSelf()
        }
        return START_STICKY
    }


    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "trip_tracking"
        private const val CHANNEL_NAME = "Viaje en curso"
        private const val TITLE = "Viaje en curso"
        private const val TEXT = "Registrando tu ruta con el GPS."
        private const val TEXT_GPS_LOST = "Sin señal de GPS. Abre la app para seguir registrando la ruta."
        private const val NOTIFICATION_ID = 4101
        private const val ACTION_STOP = "org.vicpoo.gasolineproject.STOP_TRACKING"

        fun startIntent(context: Context) = Intent(context, TripTrackingService::class.java)
        fun stopIntent(context: Context) = Intent(context, TripTrackingService::class.java).setAction(ACTION_STOP)

        /** Notificación del viaje; con [gpsLost] avisa que el GPS dejó de dar lecturas. */
        fun buildNotification(context: Context, gpsLost: Boolean) = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(TITLE)
            .setContentText(if (gpsLost) TEXT_GPS_LOST else TEXT)
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (gpsLost) TEXT_GPS_LOST else TEXT))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setContentIntent(openAppIntent(context))
            .build()

        /** Reemplaza la notificación del servicio (mismo id) sin reiniciarlo. */
        fun updateNotification(context: Context, gpsLost: Boolean) {
            // Sin permiso de notificaciones (Android 13+) no se puede mostrar: se ignora
            runCatching {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, buildNotification(context, gpsLost))
            }
        }

        private fun openAppIntent(context: Context): PendingIntent? {
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
            launch.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            return PendingIntent.getActivity(
                context,
                0,
                launch,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}

/** Inicia / detiene el servicio desde el código común (por la interfaz TrackingServiceController). */
class AndroidTrackingServiceController(private val context: Context) : TrackingServiceController {
    override fun start() {
        // Sin permiso de ubicación Android no deja usar un servicio de tipo "location"
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return
        runCatching { ContextCompat.startForegroundService(context, TripTrackingService.startIntent(context)) }
        askToIgnoreBatteryOptimizations()
    }

    /**
     * Xiaomi, Realme, Oppo y otros frenan el GPS de las apps "optimizadas" aunque tengan servicio en primer
     * plano: llegan huecos de varios segundos o minutos y la ruta mide de menos. Se pide al usuario quitar
     * la app de la optimización de batería (diálogo del sistema), como máximo una vez al día hasta que acepte.
     */
    private fun askToIgnoreBatteryOptimizations() {
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        if (power.isIgnoringBatteryOptimizations(context.packageName)) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_LAST_ASKED, 0L) < ASK_EVERY_MS) return
        prefs.edit().putLong(KEY_LAST_ASKED, now).apply()

        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private companion object {
        const val PREFS = "trip_tracking"
        const val KEY_LAST_ASKED = "battery_optimization_last_asked"
        const val ASK_EVERY_MS = 24 * 60 * 60 * 1000L
    }

    override fun stop() {
        runCatching { context.stopService(TripTrackingService.startIntent(context)) }
    }

    override fun setGpsSignalLost(lost: Boolean) {
        TripTrackingService.updateNotification(context, lost)
    }
}
