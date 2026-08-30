package com.op.banktransactiontracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.op.banktransactiontracker.data.ReminderEntity
import com.op.banktransactiontracker.data.ReminderEntity.ReminderType
import com.op.banktransactiontracker.databinding.FragmentRemindersBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ReminderFragment : Fragment() {

    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderViewModel by viewModels()
    private lateinit var adapter: ReminderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRemindersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView
        adapter = ReminderAdapter(
            onEditClick = { reminder -> viewModel.openEditDialog(reminder) },
            onDeleteClick = { reminder -> confirmDelete(reminder) }
        )
        binding.rvList.adapter = adapter
        binding.rvList.layoutManager = LinearLayoutManager(requireContext())

        // Observers
        viewModel.filteredReminders.observe(viewLifecycleOwner) { reminders ->
            adapter.submitList(reminders)
            binding.rvList.scrollToPosition(0) // به بالا اسکرول
        }

        viewModel.totalByType.observe(viewLifecycleOwner) { totals ->
            updateTotals(totals)
        }

        // فیلترها
        binding.etSearch.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) {
                    viewModel.setSearchQuery(s.toString())
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
        )

        binding.spType.apply {
            setSimpleItems(viewModel.allTypes.map { it.name })
            setOnItemClickListener { _, _, position, _ ->
                val type = viewModel.allTypes.getOrNull(position)
                viewModel.setSelectedType(type)
            }
        }

        // دکمه انتخاب تاریخ
        binding.btnPickDate.setOnClickListener {
            showDatePicker()
        }

        // FAB
        binding.fabAdd.setOnClickListener {
            viewModel.openAddDialog()
            showDialog()
        }

        // پاک کردن فیلتر
        binding.btnClearFilter.setOnClickListener {
            viewModel.clearFilter()
            binding.etSearch.setText("")
            binding.spType.setText("")
            binding.etStartDate.setText("")
            binding.etEndDate.setText("")
        }

        // لود اولیه
        viewModel.loadAllReminders()
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("انتخاب بازه تاریخ")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val start = LocalDate.ofEpochDay(selection.first / 86400000)
            val end = LocalDate.ofEpochDay(selection.second / 86400000)
            viewModel.setDateRange(start, end)
            binding.etStartDate.setText(start.format(DateTimeFormatter.ISO_LOCAL_DATE))
            binding.etEndDate.setText(end.format(DateTimeFormatter.ISO_LOCAL_DATE))
        }

        datePicker.show(parentFragmentManager, "date_range_picker")
    }

    private fun updateTotals(totals: Map<ReminderType, Double>?) {
        if (totals == null) return

        val formatted = totals.entries.joinToString("\n") { (type, amount) ->
            val color = when (type) {
                ReminderType.CHECK -> "#2196F3"   // آبی
                ReminderType.LOAN -> "#4CAF50"    // سبز
                ReminderType.DEBT -> "#FF9800"    // نارنجی
                ReminderType.OTHER -> "#9C27B0"   // بنفش
            }
            val emoji = when (type) {
                ReminderType.CHECK -> "💵"
                ReminderType.LOAN -> "📅"
                ReminderType.DEBT -> "⚖️"
                ReminderType.OTHER -> "📝"
            }
            "$emoji ${type.name} : ${String.format("%,.0f", amount)} تومان"
        }

        binding.tvTotals.text = formatted
    }

    private fun confirmDelete(reminder: ReminderEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("حذف یادآوری")
            .setMessage("آیا مطمئن هستید؟ این عملیات غیرقابل بازگشت است.")
            .setPositiveButton("حذف") { _, _ ->
                viewModel.deleteReminder(reminder)
                Toast.makeText(requireContext(), "یادآوری حذف شد", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun showDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_reminder, null)

        // ارجاع به ویویوها
        val etBank = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etBank)
        val etAmount = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etAmount)
        val etReminderDate = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etReminderDate)
        val etInstallmentAmount = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etInstallmentAmount)
        val etRemainingInstallments = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etRemainingInstallments)
        val etDebtDate = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDebtDate)
        val etDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDescription)
        val etTitle = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTitle)
        val spType = dialogView.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.spType)

        // نمایش فیلدهای مناسب
        val linearCheck = dialogView.findViewById<LinearLayout>(R.id.linearCheck)
        val linearLoan = dialogView.findViewById<LinearLayout>(R.id.linearLoan)
        val linearDebtDate = dialogView.findViewById<LinearLayout>(R.id.linearDebtDate)
        val linearDebtDesc = dialogView.findViewById<LinearLayout>(R.id.linearDebtDesc)
        val linearOtherTitle = dialogView.findViewById<LinearLayout>(R.id.linearOtherTitle)

        spType.setSimpleItems(viewModel.allTypes.map { it.name })
        spType.setOnItemClickListener { _, _, position, _ ->
            val type = viewModel.allTypes[position]
            linearCheck.visibility = if (type == ReminderType.CHECK) View.VISIBLE else View.GONE
            linearLoan.visibility = if (type == ReminderType.LOAN) View.VISIBLE else View.GONE
            linearDebtDate.visibility = if (type == ReminderType.DEBT) View.VISIBLE else View.GONE
            linearDebtDesc.visibility = if (type == ReminderType.DEBT) View.VISIBLE else View.GONE
            linearOtherTitle.visibility = if (type == ReminderType.OTHER) View.VISIBLE else View.GONE
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (viewModel.isEditMode.value == true) "ویرایش یادآوری" else "افزودن یادآوری جدید")
            .setView(dialogView)
            .setPositiveButton("ذخیره") { _, _ ->
                viewModel.lifecycleScope.launch {
                    try {
                        val type = ReminderType.entries[spType.selectedItemPosition]
                        val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
                        val reminderDateStr = etReminderDate.text.toString()
                        val reminderDate = if (reminderDateStr.isNotEmpty()) {
                            LocalDate.parse(reminderDateStr)
                        } else LocalDate.now()

                        if (reminderDate.isBefore(LocalDate.now())) {
                            Toast.makeText(requireContext(), "تاریخ یادآوری نمی‌تواند گذشته باشد!", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val reminder = when (type) {
                            ReminderType.CHECK -> {
                                ReminderEntity(
                                    id = viewModel.currentReminder.value?.id ?: 0L,
                                    type = type,
                                    bank = etBank.text.toString(),
                                    amount = amount,
                                    checkDate = reminderDate,
                                    reminderDate = reminderDate,
                                    title = "چک",
                                    status = ReminderStatus.ACTIVE
                                )
                            }
                            ReminderType.LOAN -> {
                                val installment = etInstallmentAmount.text.toString().toDoubleOrNull() ?: 0.0
                                val remaining = etRemainingInstallments.text.toString().toIntOrNull() ?: 0
                                ReminderEntity(
                                    id = viewModel.currentReminder.value?.id ?: 0L,
                                    type = type,
                                    bank = etBank.text.toString(),
                                    amount = amount,
                                    checkDate = reminderDate,
                                    installmentAmount = installment,
                                    remainingInstallments = remaining,
                                    reminderDate = reminderDate,
                                    title = "وام",
                                    status = ReminderStatus.ACTIVE
                                )
                            }
                            ReminderType.DEBT -> {
                                val debtDateStr = etDebtDate.text.toString()
                                val debtDate = if (debtDateStr.isNotEmpty()) {
                                    LocalDate.parse(debtDateStr)
                                } else LocalDate.now()
                                ReminderEntity(
                                    id = viewModel.currentReminder.value?.id ?: 0L,
                                    type = type,
                                    amount = amount,
                                    debtDate = debtDate,
                                    description = etDescription.text.toString(),
                                    reminderDate = reminderDate,
                                    title = "قرض/بدهی",
                                    status = ReminderStatus.ACTIVE
                                )
                            }
                            ReminderType.OTHER -> {
                                ReminderEntity(
                                    id = viewModel.currentReminder.value?.id ?: 0L,
                                    type = type,
                                    amount = amount,
                                    title = etTitle.text.toString(),
                                    reminderDate = reminderDate,
                                    status = ReminderStatus.ACTIVE
                                )
                            }
                        }

                        viewModel.setReminder(reminder)
                        Toast.makeText(requireContext(), "یادآوری ذخیره شد ✅", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "خطا در ذخیره: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("لغو", null)
            .create()

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}