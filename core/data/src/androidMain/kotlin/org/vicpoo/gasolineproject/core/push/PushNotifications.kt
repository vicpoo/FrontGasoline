package org.vicpoo.gasolineproject.core.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Notificaciones de solicitudes de reserva en Android:
 * - El canal se crea al abrir la app (GasolineApplication): con la app cerrada Firebase muestra
 *   el push solo, en este canal (meta-data default_notification_channel_id del Manifest).
 * - Con la app abierta Firebase NO la muestra: [show] la muestra (GasolineMessagingService).
 * - Al tocarla se abre MainActivity con los datos del push en los extras ([targetFrom]).
 */
object PushNotifications {
    /** Debe ser igual al meta-data com.google.firebase.messaging.default_notification_channel_id. */
    const val CHANNEL_ID = "solicitudes"
    private const val CHANNEL_NAME = "Solicitudes de vehículos"
    private const val CHANNEL_DESCRIPTION = "Nuevas solicitudes y respuestas a tus solicitudes."

    /** Ícono monocromático (androidApp/src/main/res/drawable/ic_notification.xml). */
    private const val ICON_NAME = "ic_notification"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = CHANNEL_DESCRIPTION
            },
        )
    }

    @SuppressLint("MissingPermission") // se revisa abajo
    fun show(context: Context, title: String, body: String, data: Map<String, String>) {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return
        createChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes(context))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context, data))
            .build()

        // Un id por notificación de la API (si llega dos veces, se reemplaza en lugar de duplicarse)
        val id = (data[PushTarget.KEY_NOTIFICATION] ?: "$title$body").hashCode()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    /** Lo que se tocó: los datos del push vienen como extras del Intent (lo hace Firebase o [show]). */
    fun targetFrom(intent: Intent?): PushTarget? {
        val extras = intent?.extras ?: return null
        return PushTarget.fromData(
            mapOf(
                PushTarget.KEY_TYPE to extras.getString(PushTarget.KEY_TYPE),
                PushTarget.KEY_REFERENCE to extras.getString(PushTarget.KEY_REFERENCE),
            ),
        )
    }

    private fun openAppIntent(context: Context, data: Map<String, String>): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        launch.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        data.forEach { (key, value) -> launch.putExtra(key, value) }
        return PendingIntent.getActivity(
            context,
            (data[PushTarget.KEY_NOTIFICATION] ?: "").hashCode(),
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    @SuppressLint("DiscouragedApi") // el ícono vive en androidApp, no en este módulo
    private fun iconRes(context: Context): Int {
        val id = context.resources.getIdentifier(ICON_NAME, "drawable", context.packageName)
        return if (id != 0) id else context.applicationInfo.icon
    }
}
