package mobi.tarantino.proxykillswitcher.feature.killswitch.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

internal class ProxyVpnNotificationFactory(
    private val context: Context,
) {
    fun createOngoingNotification(): Notification {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        return Notification
            .Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_TEXT)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "proxy_kill_switch"
        private const val NOTIFICATION_CHANNEL_NAME = "Proxy kill switch"
        private const val NOTIFICATION_TITLE = "Proxy enabled"
        private const val NOTIFICATION_TEXT = "Device traffic is routed through the saved proxy"
    }
}
