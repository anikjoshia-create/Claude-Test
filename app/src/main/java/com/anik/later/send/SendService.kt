package com.anik.later.send

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.anik.later.data.AppDatabase
import com.anik.later.data.Outcome
import com.anik.later.data.Repeat
import com.anik.later.data.ScheduledMessage
import com.anik.later.scheduling.Scheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Runs for the few seconds it takes to deliver one message.
 *
 * Tries the automatic path first and falls back to a tap-to-send notification the
 * moment anything is not exactly right — a message that needs one tap is a far better
 * outcome than one that silently disappears.
 */
class SendService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        goToForeground()
        val messageId = intent?.getLongExtra(EXTRA_MESSAGE_ID, -1L) ?: -1L
        if (messageId <= 0) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        scope.launch {
            try {
                deliver(messageId)
            } catch (t: Throwable) {
                Log.e(TAG, "Delivery of #$messageId blew up", t)
            } finally {
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun goToForeground() {
        val notification = Notifications.ongoing(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                Notifications.ONGOING_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE,
            )
        } else {
            startForeground(Notifications.ONGOING_ID, notification)
        }
    }

    private suspend fun deliver(messageId: Long) {
        val db = AppDatabase.get(this)
        val message = db.messages().byId(messageId) ?: return
        if (!message.enabled) return

        // Advance the schedule before we attempt anything. If the send goes sideways we
        // would rather miss one occurrence than hammer the same one forever.
        advance(message)

        val blocker = Permissions.autoSendBlocker(this)
        if (blocker != null) {
            handOff(message, blocker)
            return
        }

        val deferred = SendCoordinator.begin(
            SendCoordinator.Request(
                messageId = message.id,
                recipientName = message.recipientName,
                phoneNumber = message.phoneNumber,
                body = message.body,
                deadlineAt = System.currentTimeMillis() + AUTO_WINDOW_MS,
            )
        )

        try {
            startActivity(WhatsApp.chatIntent(message.phoneNumber, message.body))
        } catch (t: Throwable) {
            Log.w(TAG, "Could not open WhatsApp", t)
            SendCoordinator.complete(SendResult.Abandoned("WhatsApp would not open"))
            handOff(message, "WhatsApp would not open")
            return
        }

        val result = withTimeoutOrNull(AUTO_WINDOW_MS) { deferred.await() }
        when (result) {
            is SendResult.Sent ->
                Recorder.record(this, message, Outcome.AUTO_SENT, null)
            is SendResult.Abandoned ->
                handOff(message, result.reason)
            null -> {
                SendCoordinator.complete(SendResult.Abandoned("timed out"))
                handOff(message, "WhatsApp did not respond in time")
            }
        }
    }

    private suspend fun handOff(message: ScheduledMessage, reason: String) {
        Notifications.postHandoff(this, message, reason.replaceFirstChar { it.uppercase() })
        Recorder.record(this, message, Outcome.HANDED_OFF, reason)
    }

    /** Move a repeating message to its next occurrence, or retire a one-shot. */
    private suspend fun advance(message: ScheduledMessage) {
        val db = AppDatabase.get(this)
        if (message.repeat == Repeat.NONE) {
            db.messages().update(message.copy(enabled = false))
            return
        }
        val next = Scheduler.nextOccurrence(message.scheduledAt, message.repeat) ?: return
        val advanced = message.copy(scheduledAt = next)
        db.messages().update(advanced)
        Scheduler.schedule(this, advanced)
    }

    companion object {
        private const val TAG = "Later/SendService"
        private const val EXTRA_MESSAGE_ID = "message_id"
        private const val AUTO_WINDOW_MS = 25_000L

        fun start(context: Context, messageId: Long) {
            val intent = Intent(context, SendService::class.java)
                .putExtra(EXTRA_MESSAGE_ID, messageId)
            context.startForegroundService(intent)
        }
    }
}
