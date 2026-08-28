package com.op.banktransactiontracker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.op.banktransactiontracker.databinding.ItemPhoneBinding

class PhoneAdapter(
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<PhoneAdapter.ViewHolder>() {

    private val items = mutableListOf<String>()

    fun submitList(list: List<String>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPhoneBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemPhoneBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(phone: String) {
            binding.tvPhone.text = phone
            binding.btnRemovePhone.setOnClickListener { onRemove(phone) }
        }
    }
}