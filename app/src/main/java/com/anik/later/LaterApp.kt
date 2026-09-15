package com.anik.later

import android.app.Application
import com.anik.later.send.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LaterApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        Notifications.ensureChannels(this)
    }

    companion object {
        lateinit var instance: LaterApp
            private set

        /** Outlives any one activity — used to finish logging a send after a tap. */
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
