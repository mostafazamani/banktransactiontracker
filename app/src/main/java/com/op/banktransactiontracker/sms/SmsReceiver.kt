
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
        const val EXTRA_TYPE = "extra_type"           // "withdrawal" یا "deposit"
        const val EXTRA_AMOUNT = "extra_amount"       // مبلغ عددی
    }

    data class ExtractedAmount(
        val amountText: String,
        val amountValue: Long,
        val type: String   // "withdrawal" یا "deposit"
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val body = StringBuilder()
        var sender = ""
        for (msg in messages) {
            body.append(msg.messageBody)
            sender = msg.originatingAddress ?: ""
        }

        val phoneNumber = sender.trim()
        val messageBody = body.toString().trim()

        CoroutineScope(Dispatchers.IO).launch {
            val prefs = PhoneNumberPreferences(context)

            // نرمال‌سازی و تطبیق شماره اینجاست (+98 و صفر اول)
            val bankName = prefs.findBankName(phoneNumber) ?: return@launch

            val extracted = extractAmount(messageBody) ?: return@launch
            val prefix = if (extracted.type == "deposit") "واریز" else "برداشت"

            showNotification(
                context,
                bankName,
                phoneNumber,
                "$prefix : ${extracted.amountText} ریال",
                messageBody,
                extracted.type,
                extracted.amountValue
            )
        }
    }

    private fun normalizePhone(phone: String): String {
        var p = phone.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        when {
            p.startsWith("+98") -> p = p.removePrefix("+98")
            p.startsWith("0098") -> p = p.removePrefix("0098")
            p.startsWith("98") && p.length > 10 -> p = p.removePrefix("98")
        }

        if (p.startsWith("0") && p.length > 1) {
            p = p.removePrefix("0")
        }

        return p
    }

    private fun showNotification(
        context: Context,
        bankName: String,
        phone: String,
        displayBody: String,
        fullMessageBody: String,
        type: String,
        amount: Long
    ) {
        createNotificationChannel(context)
        val notificationId = System.currentTimeMillis().toInt()

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, notificationId, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun baseIntent(action: String, requestCodeOffset: Int, mutable: Boolean): PendingIntent {
            val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_SENDER, bankName)   // نام بانک
                putExtra(EXTRA_BODY, fullMessageBody)
                putExtra(EXTRA_PHONE, phone)       // شماره واقعی
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_TYPE, type)
                putExtra(EXTRA_AMOUNT, amount)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.getBroadcast(context, notificationId + requestCodeOffset, intent, flags)
        }

        val replyLabel = "ریپلای"
        val remoteInput = RemoteInput.Builder(KEY_REPLY).setLabel(replyLabel).build()
        val replyPendingIntent = baseIntent(ACTION_REPLY, 1, true)
        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send, replyLabel, replyPendingIntent
        ).addRemoteInput(remoteInput).setAllowGeneratedReplies(true).build()

        val otherAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_edit, "بعدا ثبت شود",
            baseIntent(ACTION_OTHER, 2, false)
        ).build()

        val cancelAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel, "لغو",
            baseIntent(ACTION_CANCEL, 3, false)
        ).build()

        val titlePrefix = if (type == "deposit") "واریز" else "برداشت"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$titlePrefix - $bankName")
            .setContentText(displayBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullMessageBody))
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

    fun extractAmount(message: String): ExtractedAmount? {
        if (message.isBlank()) return null

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

        val onlyDigitsAndShort = text.replace(Regex("[^0-9]"), "").length in 4..8 && text.length < 80
        if (onlyDigitsAndShort) {
            return null
        }
        // =============================================================

        val hasDeposit = text.contains(Regex("واریز|افزایش موجودی|واریز وجه|deposit|credit", RegexOption.IGNORE_CASE))
        val hasWithdrawal = text.contains(Regex("برداشت|خرید|پرداخت|کسر|منفی|debit|خرید شتابی", RegexOption.IGNORE_CASE))
                || text.contains(Regex("""-\s*[0-9,]{4,}"""))

        // اگر هیچ‌کدام نبود رد کن
        if (!hasDeposit && !hasWithdrawal) return null

        // اولویت با مبلغ منفی → برداشت
        val negativePattern = Pattern.compile("""-\s*([0-9,]{3,})""")
        val negMatcher = negativePattern.matcher(text)
        if (negMatcher.find()) {
            val amountText = negMatcher.group(1)?.trim()
            val value = amountText?.replace(",", "")?.replace("٬", "")?.toLongOrNull()
            if (value != null && value > 0) {
                return ExtractedAmount(amountText!!, value, "withdrawal")
            }
        }

        // الگوهای مبلغ
        val patterns = listOf(
            Pattern.compile("""برداشت[:\s]*([0-9,]{3,})""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""واریز[:\s]*([0-9,]{3,})""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""مبلغ[:\s]*([0-9,]{3,})\s*ریال?""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:خرید|تراکنش|پرداخت)[:\s].*?([0-9,]{4,})""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:کسر|منفی)[:\s]*([0-9,]{3,})""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:افزایش موجودی|واریز وجه)[:\s]*([0-9,]{3,})""", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val amountText = matcher.group(1)?.trim()
                if (!amountText.isNullOrBlank()) {
                    val numericValue = amountText.replace(",", "").replace("٬", "").toLongOrNull()
                    if (numericValue != null && numericValue > 1000) {
                        val type = when {
                            hasWithdrawal && !hasDeposit -> "withdrawal"
                            hasDeposit && !hasWithdrawal -> "deposit"
                            pattern.pattern().contains("واریز") || pattern.pattern().contains("افزایش") -> "deposit"
                            else -> "withdrawal"
                        }
                        return ExtractedAmount(amountText, numericValue, type)
                    }
                }
            }
        }

        return null
    }
}