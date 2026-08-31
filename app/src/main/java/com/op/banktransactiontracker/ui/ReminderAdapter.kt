package com.op.banktransactiontracker.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.op.banktransactiontracker.data.ReminderEntity
import com.op.banktransactiontracker.data.ReminderType
import com.op.banktransactiontracker.databinding.ItemReminderBinding
import com.op.banktransactiontracker.utils.DateUtils
import java.time.format.DateTimeFormatter

class ReminderAdapter(
    private val onEditClick: (ReminderEntity) -> Unit,
    private val onDeleteClick: (ReminderEntity) -> Unit
) : ListAdapter<ReminderEntity, ReminderAdapter.ViewHolder>(DiffCallback()) {

    private val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReminderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemReminderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(reminder: ReminderEntity) {
            val typeLabel = when (reminder.type) {
                ReminderType.CHECK -> "💵 چک"
                ReminderType.LOAN -> "📅 وام"
                ReminderType.DEBT -> "⚖️ قرض/بدهی"
                ReminderType.OTHER -> "📝 سایر"
            }
            binding.tvType.text = typeLabel

            val details = when (reminder.type) {
                ReminderType.CHECK -> {
                    val bank = reminder.bank ?: "-"
                    val beneficiary = reminder.beneficiary ?: "-"
                    "بانک: $bank | ذینفع: $beneficiary"
                }
                ReminderType.LOAN -> {
                    val bank = reminder.bank ?: "-"
                    val installment = String.format("%,.0f", reminder.installmentAmount ?: 0.0)
                    val remaining = reminder.remainingInstallments ?: 0
                    "بانک: $bank | قسط: $installment | باقی‌مانده: $remaining"
                }
                ReminderType.DEBT -> reminder.description ?: "-"
                ReminderType.OTHER -> reminder.title.ifBlank { "-" }
            }
            binding.tvDetails.text = details

            val color = when (reminder.type) {
                ReminderType.CHECK -> Color.parseColor("#2196F3")
                ReminderType.LOAN -> Color.parseColor("#4CAF50")
                ReminderType.DEBT -> Color.parseColor("#FF9800")
                ReminderType.OTHER -> Color.parseColor("#9C27B0")
            }

            binding.tvAmount.text = String.format("%,.0f ریال", reminder.amount)
            binding.tvAmount.setTextColor(color)

            binding.tvDate.text = "📅 ${DateUtils.formatLocalDate(reminder.reminderDate)}"

            binding.btnEdit.setOnClickListener { onEditClick(reminder) }
            binding.btnDelete.setOnClickListener { onDeleteClick(reminder) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ReminderEntity>() {
        override fun areItemsTheSame(old: ReminderEntity, new: ReminderEntity): Boolean =
            old.id == new.id

        override fun areContentsTheSame(old: ReminderEntity, new: ReminderEntity): Boolean =
            old == new
    }
}