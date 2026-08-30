package com.op.banktransactiontracker.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.op.banktransactiontracker.MainActivity
import com.op.banktransactiontracker.R
import com.op.banktransactiontracker.data.AppDatabase
import com.op.banktransactiontracker.data.ReminderEntity
import com.op.banktransactiontracker.data.ReminderRepository
import com.op.banktransactiontracker.data.ReminderStatus
import com.op.banktransactiontracker.data.ReminderType
import com.op.banktransactiontracker.utils.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "reminder_channel"
        const val ACTION_REMIND = "com.op.banktransactiontracker.ACTION_REMIND"
        const val ACTION_REMIND_AGAIN = "com.op.banktransactiontracker.ACTION_REMIND_AGAIN"
        const val ACTION_NO_MORE_REMIND = "com.op.banktransactiontracker.ACTION_NO_MORE_REMIND"
        const val ACTION_DONE = "com.op.banktransactiontracker.ACTION_DONE"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REMIND -> handleRemind(context, intent)
            ACTION_REMIND_AGAIN -> handleRemindAgain(context, intent)
            ACTION_NO_MORE_REMIND -> handleNoMoreRemind(context, intent)
            ACTION_DONE -> handleDone(context, intent)
            Intent.ACTION_BOOT_COMPLETED -> rescheduleAllOnBoot(context)
        }
    }

    private fun handleRemind(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getDatabase(context).reminderDao()
            val repo = ReminderRepository(dao)
            val reminder = repo.getById(reminderId) ?: return@launch

            if (reminder.status != ReminderStatus.ACTIVE) return@launch

            val today = LocalDate.now()
            val isLastDay = today.isEqual(reminder.reminderDate)

            showNotification(context, reminder, isLastDay)

            // آپدیت آخرین روز نوتیف
            repo.update(reminder.copy(lastNotifiedDate = today))
        }
    }

    private fun handleRemindAgain(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (reminderId == -1L) return

        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getDatabase(context).reminderDao()
            val repo = ReminderRepository(dao)
            val reminder = repo.getById(reminderId) ?: return@launch

            // مطمئن شو که daily روشن است
            val updated = reminder.copy(remindDaily = true)
            repo.update(updated)

            // فردا دوباره schedule کن
            ReminderScheduler.scheduleNextDay(context, updated)
        }

        cancelNotification(context, notificationId)
    }

    private fun handleNoMoreRemind(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (reminderId == -1L) return

        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getDatabase(context).reminderDao()
            val repo = ReminderRepository(dao)
            val reminder = repo.getById(reminderId) ?: return@launch

            val updated = reminder.copy(remindDaily = false)
            repo.update(updated)

            // فقط روز آخر را schedule کن
            ReminderScheduler.schedule(context, updated)
        }

        cancelNotification(context, notificationId)
    }

    private fun handleDone(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (reminderId == -1L) return

        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getDatabase(context).reminderDao()
            val repo = ReminderRepository(dao)
            val reminder = repo.getById(reminderId) ?: return@launch

            repo.update(reminder.copy(status = ReminderStatus.COMPLETED))
            ReminderScheduler.cancel(context, reminderId)
        }

        cancelNotification(context, notificationId)
    }

    private fun showNotification(context: Context, reminder: ReminderEntity, isLastDay: Boolean) {
        createChannel(context)

        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

        val title: String
        val body: String

        when (reminder.type) {
            ReminderType.CHECK -> {
                title = "یادآوری چک"
                body = "بانک: ${reminder.bank ?: "-"}\n" +
                        "ذینفع: ${reminder.beneficiary ?: "-"}\n" +
                        "مبلغ: ${String.format("%,.0f", reminder.amount)} ریال\n" +
                        "تاریخ: ${reminder.reminderDate.format(formatter)}"
            }
            ReminderType.LOAN -> {
                title = "یادآوری قسط وام"
                body = "بانک: ${reminder.bank ?: "-"}\n" +
                        "مبلغ قسط: ${String.format("%,.0f", reminder.installmentAmount ?: reminder.amount)} ریال\n" +
                        "تاریخ: ${reminder.reminderDate.format(formatter)}"
            }
            ReminderType.DEBT -> {
                title = "یادآوری قرض / بدهی"
                body = "توضیحات: ${reminder.description ?: "-"}\n" +
                        "مبلغ: ${String.format("%,.0f", reminder.amount)} ریال\n" +
                        "تاریخ تسویه: ${reminder.reminderDate.format(formatter)}"
            }
            ReminderType.OTHER -> {
                title = "یادآوری: ${reminder.title ?: "سایر"}"
                body = "مبلغ: ${String.format("%,.0f", reminder.amount)} ریال\n" +
                        "تاریخ: ${reminder.reminderDate.format(formatter)}"
            }
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPending = PendingIntent.getActivity(
            context, notificationId, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPending)

        if (isLastDay) {
            // فقط گزینه «انجام شد»
            val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_DONE
                putExtra(EXTRA_REMINDER_ID, reminder.id)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            val donePending = PendingIntent.getBroadcast(
                context, notificationId + 10, doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "انجام شد", donePending)
        } else {
            // دو گزینه: یادآوری شود + دیگر یادآوری نشود
            val againIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_REMIND_AGAIN
                putExtra(EXTRA_REMINDER_ID, reminder.id)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            val againPending = PendingIntent.getBroadcast(
                context, notificationId + 1, againIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val noMoreIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_NO_MORE_REMIND
                putExtra(EXTRA_REMINDER_ID, reminder.id)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            val noMorePending = PendingIntent.getBroadcast(
                context, notificationId + 2, noMoreIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(0, "یادآوری شود", againPending)
            builder.addAction(0, "دیگر یادآوری نشود", noMorePending)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    private fun cancelNotification(context: Context, notificationId: Int) {
        if (notificationId == -1) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "یادآوری‌ها",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "نوتیفیکیشن یادآوری چک، وام، قرض و سایر"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun rescheduleAllOnBoot(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getDatabase(context).reminderDao()
            val repo = ReminderRepository(dao)
            val active = repo.getAllActive()
            ReminderScheduler.rescheduleAll(context, active)
        }
    }
}