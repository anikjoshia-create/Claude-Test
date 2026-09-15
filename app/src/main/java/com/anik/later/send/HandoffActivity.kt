package com.anik.later.send

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.anik.later.LaterApp
import com.anik.later.data.AppDatabase
import com.anik.later.data.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The target of the fallback notification. An invisible trampoline: it opens WhatsApp
 * with the message pre-filled and, if the accessibility service happens to be running,
 * registers a request so it can still press send — turning a two-tap fallback back into
 * a one-tap one.
 */
class HandoffActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L)
        if (messageId <= 0) {
            finish()
            return
        }
        Notifications.clearHandoff(this, messageId)

        val appContext = applicationContext
        LaterApp.scope.launch {
            val message = AppDatabase.get(appContext).messages().byId(messageId)
            if (message == null) {
                withContext(Dispatchers.Main) { finish() }
                return@launch
            }

            val deferred = SendCoordinator.begin(
                SendCoordinator.Request(
                    messageId = message.id,
                    recipientName = message.recipientName,
                    phoneNumber = message.phoneNumber,
                    body = message.body,
                    deadlineAt = System.currentTimeMillis() + HANDOFF_WINDOW_MS,
                )
            )

            withContext(Dispatchers.Main) {
                try {
                    startActivity(WhatsApp.chatIntent(message.phoneNumber, message.body))
                } catch (_: Exception) {
                    Toast.makeText(appContext, "Could not open WhatsApp", Toast.LENGTH_LONG).show()
                }
                finish()
            }

            val result = withTimeoutOrNull(HANDOFF_WINDOW_MS) { deferred.await() }
            if (result is SendResult.Sent) {
                Recorder.record(appContext, message, Outcome.AUTO_SENT, "Sent after you tapped")
            }
        }
    }

    companion object {
        private const val EXTRA_MESSAGE_ID = "message_id"
        private const val HANDOFF_WINDOW_MS = 20_000L

        fun intent(context: Context, messageId: Long): Intent =
            Intent(context, HandoffActivity::class.java).apply {
                putExtra(EXTRA_MESSAGE_ID, messageId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
    }
}
