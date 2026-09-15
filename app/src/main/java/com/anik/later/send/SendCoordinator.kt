package com.anik.later.send

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicReference

sealed interface SendResult {
    data object Sent : SendResult
    data class Abandoned(val reason: String) : SendResult
}

/**
 * The handshake between whoever opens WhatsApp (the service, or the user tapping the
 * fallback notification) and the accessibility service that presses send.
 *
 * Only one request can be in flight at a time, and every request carries a deadline —
 * so if WhatsApp never comes to the foreground, or the user wanders off, the
 * accessibility service goes back to doing nothing rather than firing a stale send
 * into whatever chat happens to be open later.
 */
object SendCoordinator {

    data class Request(
        val messageId: Long,
        val recipientName: String,
        val phoneNumber: String,
        val body: String,
        val deadlineAt: Long,
    )

    private val pending = AtomicReference<Request?>(null)
    private val deferred = AtomicReference<CompletableDeferred<SendResult>?>(null)

    fun begin(request: Request): CompletableDeferred<SendResult> {
        // Anything still open is stale by definition — we only ever run one at a time.
        complete(SendResult.Abandoned("superseded by a newer message"))
        val result = CompletableDeferred<SendResult>()
        pending.set(request)
        deferred.set(result)
        return result
    }

    /** The live request, or null if there is none or it has expired. */
    fun current(): Request? {
        val request = pending.get() ?: return null
        if (System.currentTimeMillis() > request.deadlineAt) {
            complete(SendResult.Abandoned("timed out waiting for WhatsApp"))
            return null
        }
        return request
    }

    fun complete(result: SendResult) {
        pending.set(null)
        deferred.getAndSet(null)?.complete(result)
    }
}
