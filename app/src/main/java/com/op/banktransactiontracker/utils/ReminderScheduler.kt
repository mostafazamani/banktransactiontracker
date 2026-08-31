package com.op.banktransactiontracker.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.op.banktransactiontracker.data.ReminderEntity
import com.op.banktransactiontracker.data.ReminderStatus
import com.op.banktransactiontracker.data.ReminderType
import com.op.banktransactiontracker.sms.ReminderReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {

    private const val HOUR_OF_DAY = 9
    private const val MINUTE = 0

    fun schedule(context: Context, reminder: ReminderEntity) {
        if (reminder.status != ReminderStatus.ACTIVE) {
            cancel(context, reminder.id)
            return
        }

        val today = LocalDate.now()
        val targetDate = reminder.reminderDate

        // تاریخ هدف گذشته باشد → هیچ آلارمی نگذار
        if (targetDate.isBefore(today)) {
            cancel(context, reminder.id)
            return
        }

        val daysBefore = when (reminder.type) {
            ReminderType.CHECK -> 7L
            else -> 3L
        }

        val startDate = targetDate.minusDays(daysBefore)

        // اگر کاربر «دیگر یادآوری نشود» زده، فقط روز آخر
        if (!reminder.remindDaily) {
            setAlarmForDate(context, reminder.id, targetDate)
            return
        }

        // اولین روز ممکن برای نوتیف: max(startDate, today)
        var notifyDate = if (startDate.isAfter(today)) startDate else today

        // اگر notifyDate از targetDate رد شده باشد (نباید رخ دهد)
        if (notifyDate.isAfter(targetDate)) {
            cancel(context, reminder.id)
            return
        }

        // اگر روز انتخاب‌شده امروز است و ساعت ۹ صبح گذشته، برو روز بعد
        // (به شرطی که از تاریخ هدف رد نشویم)
        if (notifyDate == today && isNotifyTimePassedToday()) {
            val tomorrow = today.plusDays(1)
            notifyDate = if (!tomorrow.isAfter(targetDate)) {
                tomorrow
            } else {
                // امروز آخرین روز است و ساعت گذشته → چند دقیقه بعد نوتیف بفرست
                setAlarmSoon(context, reminder.id)
                return
            }
        }

        setAlarmForDate(context, reminder.id, notifyDate)
    }

    fun scheduleNextDay(context: Context, reminder: ReminderEntity) {
        if (!reminder.remindDaily) return
        if (reminder.status != ReminderStatus.ACTIVE) return

        val tomorrow = LocalDate.now().plusDays(1)
        if (tomorrow.isAfter(reminder.reminderDate)) return

        setAlarmForDate(context, reminder.id, tomorrow)
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

    fun rescheduleAll(context: Context, reminders: List<ReminderEntity>) {
        reminders.forEach { schedule(context, it) }
    }

    // ───────────────── private ─────────────────

    private fun isNotifyTimePassedToday(): Boolean {
        val now = LocalDateTime.now()
        val notifyToday = LocalDateTime.of(LocalDate.now(), LocalTime.of(HOUR_OF_DAY, MINUTE))
        return now.isAfter(notifyToday)
    }

    private fun setAlarmForDate(context: Context, reminderId: Long, date: LocalDate) {
        val dateTime = LocalDateTime.of(date, LocalTime.of(HOUR_OF_DAY, MINUTE))
        val triggerAt = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // اگر زمان گذشته باشد، آلارم نگذار (جلوی حلقه بی‌نهایت)
        if (triggerAt <= System.currentTimeMillis()) return

        setExactAlarm(context, reminderId, triggerAt)
    }

    /** وقتی امروز آخرین روز است و ساعت ۹ گذشته → حدود ۱ دقیقه بعد نوتیف */
    private fun setAlarmSoon(context: Context, reminderId: Long) {
        val triggerAt = System.currentTimeMillis() + 60_000L // ۱ دقیقه بعد
        setExactAlarm(context, reminderId, triggerAt)
    }

    private fun setExactAlarm(context: Context, reminderId: Long, triggerAt: Long) {
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

        try {
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
        } catch (e: SecurityException) {
            // در اندروید ۱۲+ اگر مجوز SCHEDULE_EXACT_ALARM نباشد
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pending
            )
        }
    }
}