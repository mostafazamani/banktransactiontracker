package com.op.banktransactiontracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.op.banktransactiontracker.data.TransactionEntity
import com.op.banktransactiontracker.databinding.ItemTransactionBinding
import com.op.banktransactiontracker.utils.DateUtils

class TransactionAdapter(
    private val onEditClick: (TransactionEntity) -> Unit,
    private val onDeleteClick: (TransactionEntity) -> Unit
) : ListAdapter<TransactionEntity, TransactionAdapter.ViewHolder>(DiffCallback()) {

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
            binding.tvMessage.text = item.messageBody

            if (item.description.isBlank()) {
                binding.tvDescription.visibility = View.GONE
            } else {
                binding.tvDescription.visibility = View.VISIBLE
                binding.tvDescription.text = "برداشت از :   " +item.description
            }

            binding.btnEdit.setOnClickListener { onEditClick(item) }
            binding.btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TransactionEntity>() {
        override fun areItemsTheSame(old: TransactionEntity, new: TransactionEntity) =
            old.id == new.id

        override fun areContentsTheSame(old: TransactionEntity, new: TransactionEntity) =
            old == new
    }
}