package com.anik.later.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.anik.later.send.SendService

/**
 * Fired by AlarmManager at send time. Exact alarms are one of the exemptions that
 * still allow starting a foreground service from the background, so we hand straight
 * off to [SendService] and get out of the way.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L)
        if (messageId <= 0) return
        Log.i(TAG, "Alarm fired for message #$messageId")
        SendService.start(context, messageId)
    }

    companion object {
        private const val TAG = "Later/AlarmReceiver"
        const val ACTION_FIRE = "com.anik.later.ACTION_FIRE"
        const val EXTRA_MESSAGE_ID = "message_id"
    }
}
