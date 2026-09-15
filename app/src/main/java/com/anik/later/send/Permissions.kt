package com.anik.later.send

import android.Manifest
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.anik.later.scheduling.Scheduler

/**
 * The setup screen and the send path ask the same questions, so they ask them here.
 *
 * Auto-send needs three things beyond the alarm itself: the accessibility service to do
 * the clicking, permission to draw over other apps (without it Android refuses to let a
 * background app launch WhatsApp's activity at all), and an unlocked device — we can
 * neither see nor drive WhatsApp's UI behind a lockscreen.
 */
object Permissions {

    fun accessibilityEnabled(context: Context): Boolean {
        val expected = ComponentName(context, WhatsAppAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabled.split(':').any {
            ComponentName.unflattenFromString(it) == expected
        }
    }

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun canScheduleExactAlarms(context: Context): Boolean = Scheduler.canScheduleExact(context)

    fun notificationsAllowed(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun batteryUnrestricted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)

    fun deviceLocked(context: Context): Boolean =
        context.getSystemService(KeyguardManager::class.java).isDeviceLocked

    /**
     * Why auto-send cannot run at all, or null if it is worth attempting.
     *
     * Note what is deliberately *not* here: the overlay permission. It is the
     * documented way to be allowed to launch WhatsApp from the background, but some
     * devices permit the launch without it — an exact alarm briefly puts us on the
     * power allowlist. So we try regardless and let the attempt tell us, rather than
     * refusing up front on a device where it would have worked.
     */
    fun autoSendBlocker(context: Context): String? = when {
        !WhatsApp.isInstalled(context) -> "WhatsApp is not installed"
        !accessibilityEnabled(context) -> "auto-send is turned off"
        deviceLocked(context) -> "your phone was locked"
        else -> null
    }
}
