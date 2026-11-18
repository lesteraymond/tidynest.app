package com.example.tidynest

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

object Reminders {
    private const val CHANNEL_ID = "chore_reminders"
    private const val WINDOW_MS = 30 * 60 * 1000L
    private val DAYS = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun nextTrigger(chores: List<Chore>): Long? {
        val now = System.currentTimeMillis()
        var best: Long? = null
        for (chore in chores.filter { it.reminder }) {
            for (offset in 0..7) {
                val c = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, offset)
                    set(Calendar.HOUR_OF_DAY, chore.timeMinutes / 60)
                    set(Calendar.MINUTE, chore.timeMinutes % 60)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val day = DAYS[c.get(Calendar.DAY_OF_WEEK) - 1]
                if (!chore.days.contains(day) || c.timeInMillis <= now) continue
                if (offset == 0 && chore.doneToday) continue
                if (best == null || c.timeInMillis < best) best = c.timeInMillis
                break
            }
        }
        return best
    }

    fun schedule(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val userId = Session.userId(context)
        if (userId == -1L) {
            alarms.cancel(pendingIntent(context))
            return
        }
        val at = nextTrigger(Database(context).getChores(userId))
        if (at == null) {
            alarms.cancel(pendingIntent(context))
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(context))
        }
    }

    fun notifyDue(context: Context) {
        val userId = Session.userId(context)
        if (userId == -1L) return
        val now = System.currentTimeMillis()
        val start = Chore.startOfToday()
        val due = Database(context).getChores(userId).filter {
            val at = start + it.timeMinutes * 60_000L
            it.reminder && it.isScheduledToday && !it.doneToday && at <= now && now - at <= WINDOW_MS
        }
        post(context, due, "Chore time (${due.size})")
    }

    fun notifyDueToday(context: Context, userId: Long, title: String) {
        if (userId == -1L) return
        val due = Database(context).getChores(userId)
            .filter { it.reminder && it.isScheduledToday && !it.doneToday }
            .sortedBy { it.timeMinutes }
        post(context, due, title)
    }

    private fun post(context: Context, due: List<Chore>, title: String) {
        if (!canNotify(context) || due.isEmpty()) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Chore reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, Dashboard::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = due.joinToString("\n") { "${it.timeLabel} · ${it.title}" }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(title)
            .setContentText(due.first().let { "${it.timeLabel} · ${it.title}" } +
                if (due.size > 1) " +${due.size - 1} more" else "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(1, notification)
    }

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context, 0, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.notifyDue(context)
        Reminders.schedule(context)
    }
}
