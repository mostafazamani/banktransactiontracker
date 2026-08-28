package com.op.banktransactiontracker.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.op.banktransactiontracker.data.TransactionEntity
import com.op.banktransactiontracker.databinding.ItemTransactionBinding
import com.op.banktransactiontracker.utils.DateUtils
import java.text.NumberFormat
import java.util.Locale

class TransactionAdapter(
    private val onEditClick: (TransactionEntity) -> Unit,
    private val onDeleteClick: (TransactionEntity) -> Unit
) : ListAdapter<TransactionEntity, TransactionAdapter.ViewHolder>(DiffCallback()) {

    private val numberFormat = NumberFormat.getNumberInstance(Locale("fa"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransactionEntity) {
            binding.tvSender.text = item.senderName.ifBlank { item.phoneNumber }
            binding.tvDate.text = DateUtils.formatDateTime(item.dateTime)
            binding.tvTitle.text = item.title

            val isDeposit = item.type == "deposit"
            val typeLabel = if (isDeposit) "واریز" else "برداشت"
            val typeColor = if (isDeposit) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
            val bgColor = if (isDeposit) Color.parseColor("#E8F5E9") else Color.parseColor("#FFEBEE")

            binding.tvType.text = typeLabel
            binding.tvType.setTextColor(typeColor)
            binding.tvType.setBackgroundColor(bgColor)

            if (item.amount > 0) {
                binding.tvAmount.visibility = View.VISIBLE
                binding.tvAmount.text = "${numberFormat.format(item.amount)} ریال"
                binding.tvAmount.setTextColor(typeColor)
            } else {
                binding.tvAmount.visibility = View.GONE
            }

            if (item.description.isBlank()) {
                binding.tvDescription.visibility = View.GONE
            } else {
                binding.tvDescription.visibility = View.VISIBLE
                binding.tvDescription.text = if (isDeposit) {
                    "واریز به :   ${item.description}"
                } else {
                    "برداشت از :   ${item.description}"
                }
            }

            binding.btnEdit.setOnClickListener { onEditClick(item) }
            binding.btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TransactionEntity>() {
        override fun areItemsTheSame(old: TransactionEntity, new: TransactionEntity) = old.id == new.id
        override fun areContentsTheSame(old: TransactionEntity, new: TransactionEntity) = old == new
    }
}