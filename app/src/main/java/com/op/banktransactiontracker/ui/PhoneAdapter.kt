package com.op.banktransactiontracker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.op.banktransactiontracker.R
import com.op.banktransactiontracker.data.BankEntry
import com.op.banktransactiontracker.databinding.ItemPhoneBinding

class PhoneAdapter(
    private val onRemoveBank: (BankEntry) -> Unit,
    private val onEditNumber: (bankName: String, oldNumber: String) -> Unit,
    private val onRemoveNumber: (bankName: String, number: String) -> Unit
) : RecyclerView.Adapter<PhoneAdapter.ViewHolder>() {

    private val items = mutableListOf<BankEntry>()

    fun submitList(list: List<BankEntry>) {
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

        fun bind(bank: BankEntry) {
            binding.tvBankName.text = bank.name
            binding.btnRemoveBank.setOnClickListener { onRemoveBank(bank) }

            val container = binding.layoutNumbers
            container.removeAllViews()

            val inflater = LayoutInflater.from(binding.root.context)
            for (number in bank.numbers) {
                val row = inflater.inflate(R.layout.item_phone_number, container, false)
                val tvNumber = row.findViewById<TextView>(R.id.tvNumber)
                val btnEdit = row.findViewById<ImageButton>(R.id.btnEditNumber)
                val btnRemove = row.findViewById<ImageButton>(R.id.btnRemoveNumber)

                tvNumber.text = number
                btnEdit.setOnClickListener { onEditNumber(bank.name, number) }
                btnRemove.setOnClickListener { onRemoveNumber(bank.name, number) }

                container.addView(row)
            }
        }
    }
}