

package com.op.banktransactiontracker.sms

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.op.banktransactiontracker.data.AppDatabase
import com.op.banktransactiontracker.data.TransactionEntity
import com.op.banktransactiontracker.data.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sender = intent.getStringExtra(SmsReceiver.EXTRA_SENDER) ?: return
        val body = intent.getStringExtra(SmsReceiver.EXTRA_BODY) ?: return
        val phone = intent.getStringExtra(SmsReceiver.EXTRA_PHONE) ?: sender
        val notificationId = intent.getIntExtra(SmsReceiver.EXTRA_NOTIFICATION_ID, -1)
        val type = intent.getStringExtra(SmsReceiver.EXTRA_TYPE) ?: "withdrawal"
        val amount = intent.getLongExtra(SmsReceiver.EXTRA_AMOUNT, 0L)

        val title: String
        val description: String

        when (intent.action) {
            SmsReceiver.ACTION_REPLY -> {
                val remoteInput = RemoteInput.getResultsFromIntent(intent)
                val replyText = remoteInput?.getCharSequence(SmsReceiver.KEY_REPLY)?.toString()?.trim()
                title = if (!replyText.isNullOrEmpty()) replyText else "ریپلای"
                description = replyText ?: ""
            }
            SmsReceiver.ACTION_OTHER -> {
                title = "بعدا ثبت شود"
                description = "ثبت نشده"
            }
            SmsReceiver.ACTION_CANCEL -> {
                title = "لغو"
                description = ""
            }
            else -> return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getDatabase(context).transactionDao()
            val repository = TransactionRepository(dao)

            val transaction = TransactionEntity(
                dateTime = System.currentTimeMillis(),
                senderName = sender,
                phoneNumber = phone,
                messageBody = body,
                title = title,
                description = description,
                type = type,
                amount = amount
            )
            repository.insert(transaction)
        }

        if (notificationId != -1) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(notificationId)
        }
    }
}