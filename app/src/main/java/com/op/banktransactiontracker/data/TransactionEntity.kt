package com.op.banktransactiontracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateTime: Long,                 // زمان دریافت پیام (میلی‌ثانیه)
    val senderName: String,             // نام فرستنده (یا شماره)
    val phoneNumber: String,            // شماره فرستنده
    val messageBody: String,            // متن کامل پیامک
    val title: String,                  // عنوان انتخاب‌شده (سایر / لغو / متن ریپلای)
    val description: String,            // توضیحات کاربر (قابل ویرایش)
    val type: String = "withdrawal",    // "withdrawal" یا "deposit"
    val amount: Long = 0                // مبلغ عددی برای محاسبه مجموع
)