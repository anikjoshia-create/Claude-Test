package com.anik.later.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anik.later.data.AppDatabase
import com.anik.later.data.ScheduledMessage
import com.anik.later.data.SendAttempt
import com.anik.later.scheduling.Scheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScheduleViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)

    val messages: StateFlow<List<ScheduledMessage>> = db.messages().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<SendAttempt>> = db.attempts().observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun load(id: Long): ScheduledMessage? = db.messages().byId(id)

    fun save(message: ScheduledMessage) {
        viewModelScope.launch {
            val id = if (message.id == 0L) {
                db.messages().insert(message)
            } else {
                db.messages().update(message)
                message.id
            }
            db.messages().byId(id)?.let { Scheduler.schedule(getApplication(), it) }
        }
    }

    fun setEnabled(message: ScheduledMessage, enabled: Boolean) {
        viewModelScope.launch {
            val updated = message.copy(enabled = enabled)
            db.messages().update(updated)
            if (enabled) {
                Scheduler.schedule(getApplication(), updated)
            } else {
                Scheduler.cancel(getApplication(), updated.id)
            }
        }
    }

    fun delete(message: ScheduledMessage) {
        viewModelScope.launch {
            Scheduler.cancel(getApplication(), message.id)
            db.messages().delete(message)
        }
    }

    fun clearHistory() {
        viewModelScope.launch { db.attempts().clear() }
    }
}
