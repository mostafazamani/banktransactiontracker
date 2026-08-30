package com.op.banktransactiontracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val type: ReminderType,

    // مشترک
    val amount: Double,
    val reminderDate: LocalDate,          // تاریخ هدف (چک / قسط / تسویه / سایر)
    val status: ReminderStatus = ReminderStatus.ACTIVE,
    val remindDaily: Boolean = true,      // آیا روزانه یادآوری شود؟
    val lastNotifiedDate: LocalDate? = null,

    // چک
    val bank: String? = null,
    val beneficiary: String? = null,      // ذینفع چک

    // وام
    val installmentAmount: Double? = null,
    val remainingInstallments: Int? = null,

    // قرض / بدهی
    val takeDate: LocalDate? = null,      // تاریخ گرفتن قرض
    val description: String? = null,

    // سایر
    val title: String? = null
)

enum class ReminderType {
    CHECK,      // چک
    LOAN,       // وام
    DEBT,       // قرض و بدهی
    OTHER       // سایر
}

enum class ReminderStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}