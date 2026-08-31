package com.op.banktransactiontracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val type: ReminderType,                      // نوع یادآوری
    val title: String = "",                      // عنوان (برای OTHER و نمایش)
    val bank: String? = null,                    // نام بانک (چک / وام)
    val beneficiary: String? = null,             // ذینفع چک
    val amount: Double,                          // مبلغ اصلی
    val checkDate: LocalDate? = null,            // تاریخ چک / وام (اختیاری)
    val installmentAmount: Double? = null,       // مبلغ هر قسط (وام)
    val remainingInstallments: Int? = null,      // تعداد اقساط باقی‌مانده (وام)
    val debtDate: LocalDate? = null,             // تاریخ گرفتن قرض
    val description: String? = null,             // توضیحات قرض/بدهی
    val reminderDate: LocalDate,                 // تاریخ یادآوری (تاریخ چک / قسط / تسویه / سایر)
    val status: ReminderStatus = ReminderStatus.ACTIVE,
    val remindDaily: Boolean = true,             // آیا روزانه یادآوری شود؟
    val lastNotifiedDate: LocalDate? = null      // آخرین روزی که نوتیف فرستاده شده
)

enum class ReminderType {
    CHECK, LOAN, DEBT, OTHER
}

enum class ReminderStatus {
    ACTIVE, COMPLETED, DELETED
}