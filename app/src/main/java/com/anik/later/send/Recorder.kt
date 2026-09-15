package com.anik.later.send

import android.content.Context
import com.anik.later.data.AppDatabase
import com.anik.later.data.Outcome
import com.anik.later.data.ScheduledMessage
import com.anik.later.data.SendAttempt

/** Every delivery attempt lands in the history, successful or not. */
object Recorder {

    suspend fun record(
        context: Context,
        message: ScheduledMessage,
        outcome: Outcome,
        detail: String?,
    ) {
        AppDatabase.get(context).attempts().insert(
            SendAttempt(
                messageId = message.id,
                recipientName = message.recipientName,
                phoneNumber = message.phoneNumber,
                bodyPreview = message.body.take(160),
                attemptedAt = System.currentTimeMillis(),
                outcome = outcome,
                detail = detail,
            )
        )
    }
}
