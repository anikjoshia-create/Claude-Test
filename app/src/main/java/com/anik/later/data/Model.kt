package com.anik.later.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** How often a message repeats after it fires. */
enum class Repeat {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY;

    val label: String
        get() = when (this) {
            NONE -> "Once"
            DAILY -> "Every day"
            WEEKLY -> "Every week"
            MONTHLY -> "Every month"
            YEARLY -> "Every year"
        }
}

/** What happened when we tried to deliver a message. */
enum class Outcome {
    /** The accessibility service pressed send for us. */
    AUTO_SENT,

    /** We could not auto-send, so we posted a tap-to-send notification instead. */
    HANDED_OFF,

    /** Nothing could be done — WhatsApp missing, message gone, etc. */
    FAILED;

    val label: String
        get() = when (this) {
            AUTO_SENT -> "Sent automatically"
            HANDED_OFF -> "Waiting for your tap"
            FAILED -> "Failed"
        }
}

@Entity(tableName = "scheduled_messages")
data class ScheduledMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipientName: String,
    /** Digits only, including country code. */
    val phoneNumber: String,
    val body: String,
    /** Epoch millis of the next time this should fire. */
    val scheduledAt: Long,
    val repeat: Repeat = Repeat.NONE,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "send_attempts")
data class SendAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long,
    val recipientName: String,
    val phoneNumber: String,
    val bodyPreview: String,
    val attemptedAt: Long,
    val outcome: Outcome,
    /** Why it went the way it did — shown verbatim in the history screen. */
    val detail: String? = null,
)
