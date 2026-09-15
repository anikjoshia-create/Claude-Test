package com.anik.later.send

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.anik.later.R
import com.anik.later.data.ScheduledMessage

object Notifications {

    const val CHANNEL_RUNNING = "sending"
    const val CHANNEL_HANDOFF = "handoff"
    const val ONGOING_ID = 1

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RUNNING,
                "Sending",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Shown briefly while a scheduled message is being delivered." }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_HANDOFF,
                "Needs a tap",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Shown when a message is ready but could not be sent automatically."
            }
        )
    }

    fun ongoing(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Delivering a scheduled message")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    /**
     * The fallback path: we could not send for the user, so we hand them a one-tap
     * shortcut that opens WhatsApp with everything already filled in.
     */
    fun postHandoff(context: Context, message: ScheduledMessage, reason: String) {
        val intent = HandoffActivity.intent(context, message.id)
        val pending = PendingIntent.getActivity(
            context,
            message.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_HANDOFF)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Send to ${message.recipientName}?")
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setSubText(reason)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setFullScreenIntent(pending, true)
            .addAction(0, "Open WhatsApp", pending)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(handoffId(message.id), notification)
    }

    fun clearHandoff(context: Context, messageId: Long) {
        context.getSystemService(NotificationManager::class.java).cancel(handoffId(messageId))
    }

    private fun handoffId(messageId: Long): Int = (messageId.toInt() and 0xFFFF) + 1000
}
