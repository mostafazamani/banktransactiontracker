package com.op.banktransactiontracker.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.op.banktransactiontracker.data.ReminderEntity
import com.op.banktransactiontracker.data.ReminderType
import com.op.banktransactiontracker.sms.ReminderReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {

    private const val HOUR_OF_DAY = 9   // ساعت ۹ صبح
    private const val MINUTE = 0

    fun schedule(context: Context, reminder: ReminderEntity) {
        if (reminder.status != com.op.banktransactiontracker.data.ReminderStatus.ACTIVE) {
            cancel(context, reminder.id)
            return
        }

        val today = LocalDate.now()
        val targetDate = reminder.reminderDate

        // روزهای قبل از شروع یادآوری
        val daysBefore = when (reminder.type) {
            ReminderType.CHECK -> 7
            else -> 3
        }

        val startDate = targetDate.minusDays(daysBefore.toLong())

        // اگر تاریخ هدف گذشته باشد، چیزی schedule نکن
        if (targetDate.isBefore(today)) {
            cancel(context, reminder.id)
            return
        }

        // اولین روزی که باید نوتیف بفرستیم
        val firstNotifyDate = if (startDate.isAfter(today)) startDate else today

        // اگر کاربر «دیگر یادآوری نشود» زده، فقط روز آخر
        val notifyDate = if (!reminder.remindDaily) {
            targetDate
        } else {
            firstNotifyDate
        }

        if (notifyDate.isAfter(targetDate)) return

        setAlarm(context, reminder.id, notifyDate)
    }

    fun scheduleNextDay(context: Context, reminder: ReminderEntity) {
        if (!reminder.remindDaily) return
        val tomorrow = LocalDate.now().plusDays(1)
        if (tomorrow.isAfter(reminder.reminderDate)) return
        setAlarm(context, reminder.id, tomorrow)
    }

    fun cancel(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    private fun setAlarm(context: Context, reminderId: Long, date: LocalDate) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val dateTime = LocalDateTime.of(date, LocalTime.of(HOUR_OF_DAY, MINUTE))
        val triggerAt = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (triggerAt < System.currentTimeMillis()) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }

        val pending = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pending
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pending
            )
        }
    }

    fun rescheduleAll(context: Context, reminders: List<ReminderEntity>) {
        reminders.forEach { schedule(context, it) }
    }
}