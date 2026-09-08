package com.varedog.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.ServiceCompat

class OverlayService : Service() {

    private val views = mutableListOf<OverlayTextView>()
    private lateinit var windowManager: WindowManager

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val content = intent?.getStringExtra(EXTRA_CONTENT) ?: "提醒"
        showOverlay(content)
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID, "悬浮提醒显示中",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
        val notification: Notification =
            androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("提醒显示中")
                .setContentText("长按悬浮文本可关闭")
                .setOngoing(true)
                .build()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            else 0
        )
    }

    private fun showOverlay(content: String) {
        val view = OverlayTextView(this)
        view.text = content
        view.onClose = { removeOverlay(view) }
        view.attach(windowManager, OverlayTextView.buildLayoutParams(this, views.size))
        views.add(view)
    }

    private fun removeOverlay(view: OverlayTextView) {
        view.detach()
        views.remove(view)
        if (views.isEmpty()) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    companion object {
        const val EXTRA_CONTENT = "content"
        private const val CHANNEL_ID = "overlay_service"
        private const val NOTIFICATION_ID = 1001
    }
}
