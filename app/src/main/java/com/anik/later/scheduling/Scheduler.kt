package com.anik.later.scheduling

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.anik.later.data.AppDatabase
import com.anik.later.data.Repeat
import com.anik.later.data.ScheduledMessage
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Owns the AlarmManager side of things: one exact alarm per enabled message.
 *
 * Recurrence is computed forward from the stored time rather than using a repeating
 * alarm, so that "every month on the 31st" behaves sanely and daylight-saving shifts
 * keep the wall-clock time the user picked.
 */
object Scheduler {

    private const val TAG = "Later/Scheduler"

    fun schedule(context: Context, message: ScheduledMessage) {
        if (!message.enabled) {
            cancel(context, message.id)
            return
        }
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context, message.id, create = true) ?: return

        if (canScheduleExact(context)) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, message.scheduledAt, pending)
        } else {
            // Without the exact-alarm permission the OS may delay us by several minutes.
            // Still better than dropping the message on the floor.
            Log.w(TAG, "Exact alarms not permitted; falling back to inexact for #${message.id}")
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, message.scheduledAt, pending)
        }
    }

    fun cancel(context: Context, messageId: Long) {
        val pending = pendingIntent(context, messageId, create = false) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pending)
        pending.cancel()
    }

    /** Re-arms every enabled message. Used after a reboot or an app update. */
    suspend fun rescheduleAll(context: Context) {
        val db = AppDatabase.get(context)
        val now = System.currentTimeMillis()
        for (message in db.messages().allEnabled()) {
            if (message.scheduledAt > now) {
                schedule(context, message)
                continue
            }
            // We were asleep when this should have fired. Roll a repeating message
            // forward; a one-shot fires immediately so the user still gets it.
            val next = nextOccurrence(message.scheduledAt, message.repeat, now)
            if (next == null) {
                schedule(context, message)
            } else {
                val rolled = message.copy(scheduledAt = next)
                db.messages().update(rolled)
                schedule(context, rolled)
            }
        }
    }

    fun canScheduleExact(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        } else {
            true
        }

    /**
     * The first occurrence strictly after [after], preserving the local time of day
     * of [from]. Returns null for non-repeating messages.
     */
    fun nextOccurrence(from: Long, repeat: Repeat, after: Long = System.currentTimeMillis()): Long? {
        if (repeat == Repeat.NONE) return null
        val zone = ZoneId.systemDefault()
        var candidate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(from), zone)
        val cutoff = ZonedDateTime.ofInstant(Instant.ofEpochMilli(after), zone)
        var guard = 0
        while (!candidate.isAfter(cutoff) && guard++ < MAX_ROLL_FORWARD) {
            candidate = when (repeat) {
                Repeat.DAILY -> candidate.plusDays(1)
                Repeat.WEEKLY -> candidate.plusWeeks(1)
                Repeat.MONTHLY -> candidate.plusMonths(1)
                Repeat.YEARLY -> candidate.plusYears(1)
                Repeat.NONE -> return null
            }
        }
        return candidate.toInstant().toEpochMilli()
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun pendingIntent(context: Context, messageId: Long, create: Boolean): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            // The data URI keeps PendingIntents for different messages distinct even
            // though filterEquals() ignores extras.
            data = android.net.Uri.parse("later://message/$messageId")
            putExtra(AlarmReceiver.EXTRA_MESSAGE_ID, messageId)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, messageId.toInt(), intent, flags)
    }

    /** Enough to roll a daily message forward through ~5 years of downtime. */
    private const val MAX_ROLL_FORWARD = 2000
}
