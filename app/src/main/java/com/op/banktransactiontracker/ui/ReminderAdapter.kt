package com.op.banktransactiontracker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.op.banktransactiontracker.data.ReminderEntity
import com.op.banktransactiontracker.data.ReminderEntity.ReminderStatus
import com.op.banktransactiontracker.databinding.ItemReminderBinding
import java.time.format.DateTimeFormatter

class ReminderAdapter(
    private val onEditClick: (ReminderEntity) -> Unit,
    private val onDeleteClick: (ReminderEntity) -> Unit
) : ListAdapter<ReminderEntity, ReminderAdapter.ViewHolder>(ReminderDiffCallback()) {

    class ViewHolder(private val binding: ItemReminderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(reminder: ReminderEntity) {
            binding.tvType.text = reminder.type.name
            binding.tvDetails.text = when (reminder.type) {
                ReminderEntity.ReminderType.CHECK -> reminder.bank ?: ""
                ReminderEntity.ReminderType.LOAN -> "قسط: ${String.format("%,.0f", reminder.installmentAmount ?: 0)} - ${reminder.remainingInstallments ?: 0} قسط"
                ReminderEntity.ReminderType.DEBT -> reminder.description ?: ""
                ReminderEntity.ReminderType.OTHER -> reminder.title ?: ""
            }

            binding.tvAmount.text = String.format("%,.0f", reminder.amount)
            binding.tvAmount.setTextColor(
                when (reminder.type) {
                    ReminderEntity.ReminderType.CHECK -> android.graphics.Color.parseColor("#2196F3")
                    ReminderEntity.ReminderType.LOAN -> android.graphics.Color.parseColor("#4CAF50")
                    ReminderEntity.ReminderType.DEBT -> android.graphics.Color.parseColor("#FF9800")
                    ReminderEntity.ReminderType.OTHER -> android.graphics.Color.parseColor("#9C27B0")
                }
            )

            binding.tvDate.text = "📅 " + reminder.reminderDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

            binding.btnEdit.setOnClickListener { onEditClick(reminder) }
            binding.btnDelete.setOnClickListener { onDeleteClick(reminder) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReminderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ReminderDiffCallback : DiffUtil.ItemCallback<ReminderEntity>() {
    override fun areItemsTheSame(oldItem: ReminderEntity, newItem: ReminderEntity): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: ReminderEntity, newItem: ReminderEntity): Boolean {
        return oldItem == newItem
    }
}