package com.anik.later.send

import android.content.Context
import android.content.Intent
import android.net.Uri

object WhatsApp {

    const val PACKAGE = "com.whatsapp"

    fun isInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(PACKAGE, 0)
        true
    } catch (_: Exception) {
        false
    }

    /**
     * Opens the chat with [phoneNumber] and drops [body] straight into the composer.
     * Targeting the package explicitly skips the "open with" chooser.
     */
    fun chatIntent(phoneNumber: String, body: String): Intent {
        val digits = normalise(phoneNumber)
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$digits&text=${Uri.encode(body)}")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** WhatsApp wants country code + number, digits only. */
    fun normalise(phoneNumber: String): String = phoneNumber.filter { it.isDigit() }
}
