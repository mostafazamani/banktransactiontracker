package com.op.banktransactiontracker.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.op.banktransactiontracker.MainActivity
import com.op.banktransactiontracker.R
import com.op.banktransactiontracker.data.PhoneNumberPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "bank_sms_channel"
        const val KEY_REPLY = "key_reply_text"
        const val ACTION_REPLY = "com.op.banktransactiontracker.ACTION_REPLY"
        const val ACTION_OTHER = "com.op.banktransactiontracker.ACTION_OTHER"
        const val ACTION_CANCEL = "com.op.banktransactiontracker.ACTION_CANCEL"

        const val EXTRA_SENDER = "extra_sender"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        // ترکیب پیام‌های چندبخشی
        val body = StringBuilder()
        var sender = ""
        for (msg in messages) {
            body.append(msg.messageBody)
            sender = msg.originatingAddress ?: ""
        }

        val phoneNumber = sender.trim()
        val messageBody = body.toString().trim()

        // چک کردن اینکه شماره در لیست ذخیره شده باشد
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = PhoneNumberPreferences(context)
            val savedNumbers = prefs.phoneNumbersFlow.first()

            // شماره را نرمال‌سازی می‌کنیم (حذف + و فاصله)
            val normalizedIncoming = normalizePhone(phoneNumber)
            val isMatch = savedNumbers.any { saved ->
                normalizePhone(saved) == normalizedIncoming ||
                        normalizedIncoming.endsWith(normalizePhone(saved)) ||
                        normalizePhone(saved).endsWith(normalizedIncoming)
            }
           // showNotification(context, phoneNumber, messageBody)
            if (isMatch) {
                val sms = extractWithdrawalAmount(messageBody)
                if (sms!=null){
                    showNotification(context, phoneNumber," مبلغ : $sms ریال " )
                }

            }
        }
    }

    private fun normalizePhone(phone: String): String {
        return phone.replace("+", "")
            .replace(" ", "")
            .replace("-", "")
            .trim()
    }

    private fun showNotification(context: Context, phone: String, body: String) {
        createNotificationChannel(context)

        val notificationId = System.currentTimeMillis().toInt()

        // Intent برای باز کردن برنامه
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, notificationId, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // --- اکشن Reply ---
        val replyLabel = "ریپلای"
        val remoteInput = RemoteInput.Builder(KEY_REPLY)
            .setLabel(replyLabel)
            .build()

        val replyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra(EXTRA_SENDER, phone)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_PHONE, phone)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 1, replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            replyLabel,
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        // --- اکشن سایر ---
        val otherIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_OTHER
            putExtra(EXTRA_SENDER, phone)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_PHONE, phone)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val otherPendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 2, otherIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val otherAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_edit,
            "بعدا ثبت شود",
            otherPendingIntent
        ).build()

        // --- اکشن لغو ---
        val cancelIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_CANCEL
            putExtra(EXTRA_SENDER, phone)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_PHONE, phone)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 3, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancelAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "لغو",
            cancelPendingIntent
        ).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(phone)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(replyAction)
            .addAction(otherAction)
            .addAction(cancelAction)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "پیامک بانکی"
            val description = "نوتیفیکیشن تراکنش‌های بانکی"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun extractWithdrawalAmount(message: String): String? {
        if (message.isBlank()) return null

        // نرمال‌سازی متن
        val text = message
            .replace("\r", " ")
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // ==================== فیلتر رمز پویا (OTP) ====================
        val otpKeywords = listOf(
            "رمز پویا",
            "رمز یکبار مصرف",
            "رمز یک بار مصرف",
            "کد تایید",
            "کد تأیید",
            "کد امنیتی",
            "رمز عبور",
            "کد فعالسازی",
            "کد فعال‌سازی",
            "otp",
            "password",
            "verification code",
            "کد ورود",
            "رمز موقت"
        )

        if (otpKeywords.any { text.contains(it, ignoreCase = true) }) {
            return null
        }

        // فیلتر پیام‌های خیلی کوتاه که فقط عدد دارند
        val onlyDigitsAndShort = text.replace(Regex("[^0-9]"), "").length in 4..8 && text.length < 80
        if (onlyDigitsAndShort) {
            return null
        }
        // =============================================================

        // اگر فقط واریز باشد و نشانه‌ای از برداشت نباشد → رد کن
        val hasDeposit = text.contains(Regex("واریز|افزایش موجودی|واریز وجه", RegexOption.IGNORE_CASE))
        val hasWithdrawal = text.contains(Regex("برداشت|خرید|پرداخت|کسر|منفی|debit|خرید شتابی", RegexOption.IGNORE_CASE))
                || text.contains(Regex("""-\s*[0-9,]{4,}"""))

        if (hasDeposit && !hasWithdrawal) return null

        // ---------------- اولویت ۱: مبلغ منفی ----------------
        val negativePattern = Pattern.compile("""-\s*([0-9,]{3,})""")
        val negMatcher = negativePattern.matcher(text)
        if (negMatcher.find()) {
            val amount = negMatcher.group(1)?.trim()
            if (!amount.isNullOrBlank() && amount.replace(",", "").replace("٬", "").toLongOrNull()?.let { it > 0 } == true) {
                return amount
            }
        }

        // ---------------- اولویت ۲: الگوهای مشخص برداشت ----------------
        val patterns = listOf(
            Pattern.compile("""برداشت[:\s]*([0-9,]{3,})""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""مبلغ[:\s]*([0-9,]{3,})\s*ریال?""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:خرید|تراکنش|پرداخت)[:\s].*?([0-9,]{4,})""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:کسر|منفی)[:\s]*([0-9,]{3,})""", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val amount = matcher.group(1)?.trim()
                if (!amount.isNullOrBlank()) {
                    val numericValue = amount.replace(",", "").replace("٬", "").toLongOrNull()
                    if (numericValue != null && numericValue > 1000) {
                        return amount
                    }
                }
            }
        }

        return null
    }
}