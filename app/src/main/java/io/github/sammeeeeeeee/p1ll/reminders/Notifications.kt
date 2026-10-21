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
import io.github.sammeeeeeeee.p1ll.MainActivity
import io.github.sammeeeeeeee.p1ll.R
import io.github.sammeeeeeeee.p1ll.data.Pill
import io.github.sammeeeeeeee.p1ll.domain.slotTime
import io.github.sammeeeeeeee.p1ll.ui.Fmt
import io.github.sammeeeeeeee.p1ll.ui.components.drawGlyphBadge
import io.github.sammeeeeeeee.p1ll.ui.theme.pillColor
import java.time.ZoneId

object Notifications {
    const val CHANNEL_DOSES = "doses"
    const val CHANNEL_SUPPLY = "supply"

    enum class Kind { DUE, SNOOZED, NAG }

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val chime = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.reminder}")
        val doses = NotificationChannel(
            CHANNEL_DOSES,
            context.getString(R.string.channel_doses),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_doses_desc)
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
        val supply = NotificationChannel(
            CHANNEL_SUPPLY,
            context.getString(R.string.channel_supply),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_supply_desc) }
        manager.createNotificationChannels(listOf(doses, supply))
    }

    fun id(pillId: Long, minute: Int): Int = ((pillId % 1_000_000) * 1440 + minute).toInt()

    fun cancel(context: Context, pillId: Long, minute: Int) {
        NotificationManagerCompat.from(context).cancel(id(pillId, minute))
    }

    fun dose(context: Context, pill: Pill, day: Long, minute: Int, snoozeMinutes: Int, kind: Kind) {
        if (!allowed(context)) return
        val id = id(pill.id, minute)
        val time = Fmt.time(context, minute)
        val text = when (kind) {
            Kind.DUE -> listOf(pill.dosage, context.getString(R.string.notif_due, time))
            Kind.SNOOZED -> listOf(context.getString(R.string.notif_snoozed, time))
            Kind.NAG -> listOf(context.getString(R.string.notif_still_due, time))
        }.filter { it.isNotBlank() }.joinToString(", ")

        fun action(action: String, request: Int) = PendingIntent.getBroadcast(
            context,
            request,
            Intent(context, ActionReceiver::class.java)
                .setAction(action)
                .setData(Uri.parse("p1ll://dose/${pill.id}/$day/$minute"))
                .putExtra(ActionReceiver.EXTRA_PILL, pill.id)
                .putExtra(ActionReceiver.EXTRA_DAY, day)
                .putExtra(ActionReceiver.EXTRA_MINUTE, minute),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DOSES)
            .setSmallIcon(R.drawable.ic_stat_p1ll)
            .setLargeIcon(glyph(context, pill))
            .setContentTitle(pill.name)
            .setContentText(text)
            .setColor(pillColor(pill.color, dark = false).toArgb())
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(slotTime(day, minute, ZoneId.systemDefault()).toInstant().toEpochMilli())
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, id))
            .addAction(R.drawable.ic_notif_check, context.getString(R.string.notif_taken), action(ActionReceiver.ACTION_TAKE, id))
            .addAction(
                R.drawable.ic_notif_snooze,
                context.getString(R.string.notif_snooze, snoozeMinutes),
                action(ActionReceiver.ACTION_SNOOZE, id),
            )
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(id, notification)
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
