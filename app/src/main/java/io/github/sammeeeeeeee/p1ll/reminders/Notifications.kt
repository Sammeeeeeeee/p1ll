package io.github.sammeeeeeeee.p1ll.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.net.Uri
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import io.github.sammeeeeeeee.p1ll.MainActivity
import io.github.sammeeeeeeee.p1ll.R
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.domain.slotTime
import io.github.sammeeeeeeee.p1ll.ui.Fmt
import io.github.sammeeeeeeee.p1ll.ui.components.drawGlyphBadge
import io.github.sammeeeeeeee.p1ll.ui.theme.pillColor
import java.time.ZoneId
import java.util.UUID

object Notifications {
    const val CHANNEL_DOSES = "doses"
    /**
     * Where a snoozed reminder moves: minimized, never alerting again. Stock Android files it under
     * Silent, collapsed. Samsung's One UI has no Silent section (one list, sorted by time) but
     * collapses minimized notifications and can gather them at the bottom (Filter notifications).
     */
    const val CHANNEL_QUIET = "doses_quiet"
    const val CHANNEL_SUPPLY = "supply"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val chime = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.reminder}")
        val doses = NotificationChannel(
            CHANNEL_DOSES,
            context.getString(R.string.channel_doses),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            setSound(
                chime,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 40, 90, 60)
        }
        // Created at LOW in 1.1.x; Android lowers an existing channel to MIN here unless the user
        // has changed its settings.
        val quiet = NotificationChannel(
            CHANNEL_QUIET,
            context.getString(R.string.channel_quiet),
            NotificationManager.IMPORTANCE_MIN,
        )
        val supply = NotificationChannel(
            CHANNEL_SUPPLY,
            context.getString(R.string.channel_supply),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannels(listOf(doses, quiet, supply))
    }

    fun id(pillId: Long, minute: Int): Int = ((pillId % 1_000_000) * 1440 + minute).toInt()

    /** A snoozed reminder is posted under this tag, as its own notification (see [dose]). */
    private const val TAG_SNOOZED = "snoozed"

    fun cancel(context: Context, pillId: Long, minute: Int) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(id(pillId, minute))
        manager.cancel(TAG_SNOOZED, id(pillId, minute))
    }

    /**
     * One reminder per dose. It alerts once and stays (out of reach of Clear all) until the dose is
     * taken or snoozed. [quiet] is the snoozed form: a minimized silent copy, with only Take left. [repostedAt] marks a "remind again" repeat: any copy still showing is replaced
     * by a fresh post stamped at that time, so Android treats it as a new alert, not a quiet update.
     */
    fun dose(context: Context, pill: Pill, day: Long, minute: Int, repostedAt: Long? = null, quiet: Boolean = false) {
        if (!allowed(context)) return
        val id = id(pill.id, minute)
        val due = Fmt.time(context, minute)
        val dosage = pill.dosage.trim()
        val text = if (dosage.isEmpty()) context.getString(R.string.notif_due, due)
        else context.getString(R.string.notif_dose_due, dosage, due)
        val data = Uri.parse("p1ll://dose/${pill.id}/$day/$minute")

        fun Intent.dose() = setData(data)
            .putExtra(ActionReceiver.EXTRA_PILL, pill.id)
            .putExtra(ActionReceiver.EXTRA_DAY, day)
            .putExtra(ActionReceiver.EXTRA_MINUTE, minute)

        // Take opens the app, which records the dose in front of you; Snooze works in the background.
        val take = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_TAKE)
                .dose()
                .putExtra(EXTRA_TOKEN, token(context))
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, if (quiet) CHANNEL_QUIET else CHANNEL_DOSES)
            .setSmallIcon(R.drawable.ic_stat_p1ll)
            .setLargeIcon(glyph(context, pill))
            .setContentTitle(pill.name)
            .setContentText(text)
            .setColor(pillColor(pill.color, dark = false).toArgb())
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (quiet) NotificationCompat.PRIORITY_MIN else NotificationCompat.PRIORITY_HIGH)
            .setWhen(repostedAt ?: slotTime(day, minute, ZoneId.systemDefault()).toInstant().toEpochMilli())
            .setShowWhen(true)
            .setOngoing(!quiet)
            .setOnlyAlertOnce(quiet)
            .setContentIntent(openApp(context, id))
            .addAction(R.drawable.ic_notif_check, context.getString(R.string.notif_taken), take)
        if (!quiet) {
            val snooze = PendingIntent.getBroadcast(
                context,
                id,
                Intent(context, ActionReceiver::class.java).setAction(ActionReceiver.ACTION_SNOOZE).dose(),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(R.drawable.ic_notif_snooze, context.getString(R.string.notif_snooze), snooze)
        }
        val notification = builder.build()
        val manager = NotificationManagerCompat.from(context)
        if (quiet) {
            // Android won't move a notification to another section while the shade is open, so an
            // update would sit among the alerts until it closed. A new notification is placed by its
            // own importance straight away, so the snoozed copy replaces the reminder instead.
            manager.cancel(id)
            @Suppress("MissingPermission")
            manager.notify(TAG_SNOOZED, id, notification)
        } else {
            // One notification per dose: a new reminder replaces an older snoozed one at the same time.
            manager.cancel(TAG_SNOOZED, id)
            if (repostedAt != null) manager.cancel(id)
            @Suppress("MissingPermission")
            manager.notify(id, notification)
        }
    }

    private const val EXTRA_TOKEN = "token"

    /** A random value only this install knows, so another app can't send MainActivity a Take. */
    private fun token(context: Context): String {
        val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        return prefs.getString("takeToken", null) ?: UUID.randomUUID().toString().also {
            prefs.edit { putString("takeToken", it) }
        }
    }

    fun isOwnTake(context: Context, intent: Intent): Boolean =
        intent.getStringExtra(EXTRA_TOKEN)?.let { it == token(context) } == true

    enum class Health { OK, OFF, SILENT }

    /** Whether reminders can alert: besides the app-wide permission, a long-press can turn the channel down to silent. */
    fun health(context: Context): Health {
        if (!allowed(context)) return Health.OFF
        val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL_DOSES) ?: return Health.OK
        return when {
            channel.importance == NotificationManager.IMPORTANCE_NONE -> Health.OFF
            channel.importance < NotificationManager.IMPORTANCE_DEFAULT -> Health.SILENT
            else -> Health.OK
        }
    }

    fun lowStock(context: Context, pill: Pill) {
        if (!allowed(context)) return
        val left = pill.stock ?: return
        val perDay = (pill.times.size * pill.perDose).coerceAtLeast(1)
        val days = left / perDay
        val text = if (pill.asNeeded) context.resources.getQuantityString(R.plurals.left, left, left)
        else context.getString(
            R.string.notif_low_text,
            context.resources.getQuantityString(R.plurals.left, left, left),
            context.resources.getQuantityString(R.plurals.days, days, days),
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_SUPPLY)
            .setSmallIcon(R.drawable.ic_stat_p1ll)
            .setLargeIcon(glyph(context, pill))
            .setContentTitle(context.getString(R.string.notif_low_title, pill.name))
            .setContentText(text)
            .setColor(pillColor(pill.color, dark = false).toArgb())
            .setAutoCancel(true)
            .setContentIntent(openApp(context, -pill.id.toInt()))
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(-(pill.id % 1_000_000).toInt() - 1, notification)
    }

    @SuppressLint("InlinedApi") // the permission check only matters on 33+, where the constant exists
    fun allowed(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33

    private fun openApp(context: Context, request: Int) = PendingIntent.getActivity(
        context,
        request,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** The same glyph the app draws, rendered for the notification's large icon. */
    private fun glyph(context: Context, pill: Pill): Bitmap {
        val px = (64 * context.resources.displayMetrics.density).toInt()
        val image = ImageBitmap(px, px)
        CanvasDrawScope().draw(Density(context), LayoutDirection.Ltr, Canvas(image), Size(px.toFloat(), px.toFloat())) {
            drawGlyphBadge(pill.shape, pillColor(pill.color, dark = false))
        }
        return image.asAndroidBitmap()
    }
}
