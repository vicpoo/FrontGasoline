package org.vicpoo.gasolineproject.core.location

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

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
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
        }.onFailure {
            // Sin permiso de ubicación Android no deja iniciar el servicio: el viaje sigue en pantalla
            stopSelf()
        }
        return START_STICKY
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(applicationInfo.icon)
        .setContentTitle(TITLE)
        .setContentText(TEXT)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
        .setContentIntent(openAppIntent())
        .build()

    private fun openAppIntent(): PendingIntent? {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        launch.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(
            this,
            0,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
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
        private const val NOTIFICATION_ID = 4101
        private const val ACTION_STOP = "org.vicpoo.gasolineproject.STOP_TRACKING"

        fun startIntent(context: Context) = Intent(context, TripTrackingService::class.java)
        fun stopIntent(context: Context) = Intent(context, TripTrackingService::class.java).setAction(ACTION_STOP)
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
    }

    override fun stop() {
        runCatching { context.stopService(TripTrackingService.startIntent(context)) }
    }
}
