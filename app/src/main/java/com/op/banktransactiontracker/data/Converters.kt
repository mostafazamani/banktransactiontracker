package com.op.banktransactiontracker.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromReminderType(type: ReminderType): String = type.name

    @TypeConverter
    fun toReminderType(value: String): ReminderType = ReminderType.valueOf(value)

    @TypeConverter
    fun fromReminderStatus(status: ReminderStatus): String = status.name

    @TypeConverter
    fun toReminderStatus(value: String): ReminderStatus = ReminderStatus.valueOf(value)
}