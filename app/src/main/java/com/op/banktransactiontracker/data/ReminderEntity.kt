package com.op.banktransactiontracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val type: ReminderType,                    // نوع یادآوری
    val title: String,                         // عنوان یادآوری (برای سایر)
    val bank: String? = null,                  // چک / وام
    val amount: Double,                        // مبلغ
    val checkDate: LocalDate? = null,          // تاریخ چک / وام
    val installmentAmount: Double? = null,     // مبلغ قسط (وام)
    val remainingInstallments: Int? = null,    // تعداد اقساط باقی‌مانده (وام)
    val debtDate: LocalDate? = null,           // تاریخ تسویه قرض/بدهی
    val description: String? = null,           // توضیحات قرض/بدهی
    val reminderDate: LocalDate,               // تاریخ یادآوری (می‌تواند آینده باشد)
    val status: ReminderStatus = ReminderStatus.ACTIVE // وضعیت نوتیفیکیشن
)

enum class ReminderType {
    CHECK, LOAN, DEBT, OTHER
}

enum class ReminderStatus {
    ACTIVE, COMPLETED, DELETED
}