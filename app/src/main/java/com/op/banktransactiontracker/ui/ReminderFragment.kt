package com.op.banktransactiontracker.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.op.banktransactiontracker.R
import com.op.banktransactiontracker.data.ReminderEntity
import com.op.banktransactiontracker.data.ReminderStatus
import com.op.banktransactiontracker.data.ReminderType
import com.op.banktransactiontracker.databinding.FragmentRemindersBinding
import com.op.banktransactiontracker.utils.DateUtils
import com.op.banktransactiontracker.utils.PersianDatePickerHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

class ReminderFragment : Fragment() {

    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderViewModel by viewModels {
        ReminderViewModelFactory(requireActivity().application)
    }

    private lateinit var adapter: ReminderAdapter
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRemindersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ReminderAdapter(
            onEditClick = { reminder ->
                viewModel.openEditDialog(reminder)
                showAddEditDialog()
            },
            onDeleteClick = { reminder -> confirmDelete(reminder) }
        )
        binding.rvList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvList.adapter = adapter

        observeData()
        setupSearch()
        setupFilters()
        setupFab()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredReminders.collectLatest { list ->
                adapter.submitList(list)
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                binding.rvList.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalByType.collectLatest { totals ->
                updateTotals(totals)
            }
        }
    }

    private fun updateTotals(totals: Map<ReminderType, Double>) {
        fun format(amount: Double) = String.format("%,.0f", amount)

        binding.tvTotalCheck.text = "💵 چک: ${format(totals[ReminderType.CHECK] ?: 0.0)} ریال"
        binding.tvTotalLoan.text = "📅 وام: ${format(totals[ReminderType.LOAN] ?: 0.0)} ریال"
        binding.tvTotalDebt.text = "⚖️ قرض/بدهی: ${format(totals[ReminderType.DEBT] ?: 0.0)} ریال"
        binding.tvTotalOther.text = "📝 سایر: ${format(totals[ReminderType.OTHER] ?: 0.0)} ریال"
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString()?.trim() ?: "")
            }
        })
    }

    private fun setupFilters() {
        binding.chipTypeFilter.setOnClickListener { showTypeFilterDialog() }
        binding.chipDateFilter.setOnClickListener { showDateFilterDialog() }
        binding.chipClearFilters.setOnClickListener {
            viewModel.clearFilter()
            binding.etSearch.setText("")
            binding.chipTypeFilter.text = "همه انواع"
            binding.chipDateFilter.text = "همه تاریخ‌ها"
            binding.chipClearFilters.visibility = View.GONE
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            viewModel.openAddDialog()
            showAddEditDialog()
        }
    }

    private fun showTypeFilterDialog() {
        val options = mutableListOf("همه انواع")
        options.addAll(viewModel.allTypes.map {
            when (it) {
                ReminderType.CHECK -> "چک"
                ReminderType.LOAN -> "وام"
                ReminderType.DEBT -> "قرض/بدهی"
                ReminderType.OTHER -> "سایر"
            }
        })

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("فیلتر بر اساس نوع")
            .setItems(options.toTypedArray()) { _, which ->
                if (which == 0) {
                    viewModel.setSelectedType(null)
                    binding.chipTypeFilter.text = "همه انواع"
                } else {
                    val type = viewModel.allTypes[which - 1]
                    viewModel.setSelectedType(type)
                    binding.chipTypeFilter.text = options[which]
                }
                updateClearFilterVisibility()
            }
            .show()
    }

    private fun showDateFilterDialog() {
        val options = arrayOf("همه تاریخ‌ها", "انتخاب بازه دلخواه")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("فیلتر تاریخ")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        viewModel.setDateRange(null, null)
                        binding.chipDateFilter.text = "همه تاریخ‌ها"
                    }
                    1 -> showDateRangePicker()
                }
                updateClearFilterVisibility()
            }
            .show()
    }

    private fun showDateRangePicker() {
        PersianDatePickerHelper.show(
            context = requireContext(),
            initial = LocalDate.now().minusMonths(1),
            onDateSelected = { start ->
                PersianDatePickerHelper.show(
                    context = requireContext(),
                    initial = LocalDate.now(),
                    minDate = start,
                    onDateSelected = { end ->
                        viewModel.setDateRange(start, end)
                        binding.chipDateFilter.text =
                            "${DateUtils.formatLocalDate(start)} تا ${DateUtils.formatLocalDate(end)}"
                        updateClearFilterVisibility()
                    }
                )
            }
        )
    }

    private fun updateClearFilterVisibility() {
        val hasFilter = binding.chipTypeFilter.text != "همه انواع" ||
                binding.chipDateFilter.text != "همه تاریخ‌ها" ||
                !binding.etSearch.text.isNullOrBlank()
        binding.chipClearFilters.visibility = if (hasFilter) View.VISIBLE else View.GONE
    }

    private fun confirmDelete(reminder: ReminderEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("حذف یادآوری")
            .setMessage("آیا از حذف این یادآوری مطمئن هستید؟")
            .setPositiveButton("حذف") { _, _ ->
                viewModel.deleteReminder(reminder)
                Toast.makeText(requireContext(), "یادآوری حذف شد", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun showAddEditDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_reminder, null)

        val spType = dialogView.findViewById<AutoCompleteTextView>(R.id.spType)
        val etBank = dialogView.findViewById<TextInputEditText>(R.id.etBank)
        val etBeneficiary = dialogView.findViewById<TextInputEditText>(R.id.etBeneficiary)
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.etAmount)
        val etReminderDate = dialogView.findViewById<TextInputEditText>(R.id.etReminderDate)
        val etLoanBank = dialogView.findViewById<TextInputEditText>(R.id.etLoanBank)
        val etInstallmentAmount = dialogView.findViewById<TextInputEditText>(R.id.etInstallmentAmount)
        val etRemainingInstallments = dialogView.findViewById<TextInputEditText>(R.id.etRemainingInstallments)
        val etDebtDate = dialogView.findViewById<TextInputEditText>(R.id.etDebtDate)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etDescription)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etTitle)

        val linearCheck = dialogView.findViewById<LinearLayout>(R.id.linearCheck)
        val linearLoan = dialogView.findViewById<LinearLayout>(R.id.linearLoan)
        val linearDebt = dialogView.findViewById<LinearLayout>(R.id.linearDebt)
        val linearOther = dialogView.findViewById<LinearLayout>(R.id.linearOther)

        val typeLabels = listOf("چک", "وام", "قرض/بدهی", "سایر")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, typeLabels)
        spType.setAdapter(typeAdapter)

        fun updateFieldsVisibility(position: Int) {
            linearCheck.visibility = if (position == 0) View.VISIBLE else View.GONE
            linearLoan.visibility = if (position == 1) View.VISIBLE else View.GONE
            linearDebt.visibility = if (position == 2) View.VISIBLE else View.GONE
            linearOther.visibility = if (position == 3) View.VISIBLE else View.GONE
        }

        spType.setOnItemClickListener { _, _, position, _ ->
            updateFieldsVisibility(position)
        }

        // پر کردن در حالت ویرایش
        val existing = viewModel.currentReminder.value
        val isEdit = viewModel.isEditMode.value && existing != null

        if (isEdit && existing != null) {
            val typeIndex = when (existing.type) {
                ReminderType.CHECK -> 0
                ReminderType.LOAN -> 1
                ReminderType.DEBT -> 2
                ReminderType.OTHER -> 3
            }
            spType.setText(typeLabels[typeIndex], false)
            updateFieldsVisibility(typeIndex)

            etAmount.setText(if (existing.amount > 0) existing.amount.toLong().toString() else "")
            etReminderDate.setText(DateUtils.formatLocalDate(existing.reminderDate))
            etReminderDate.tag = existing.reminderDate

            when (existing.type) {
                ReminderType.CHECK -> {
                    etBank.setText(existing.bank ?: "")
                    etBeneficiary.setText(existing.beneficiary ?: "")
                }
                ReminderType.LOAN -> {
                    etLoanBank.setText(existing.bank ?: "")
                    etInstallmentAmount.setText(
                        existing.installmentAmount?.toLong()?.toString() ?: ""
                    )
                    etRemainingInstallments.setText(
                        existing.remainingInstallments?.toString() ?: ""
                    )
                }
                ReminderType.DEBT -> {
                    etDebtDate.setText(existing.debtDate?.format(dateFormatter) ?: "")
                    etDescription.setText(existing.description ?: "")
                }
                ReminderType.OTHER -> {
                    etTitle.setText(existing.title)
                }
            }
        } else {
            spType.setText(typeLabels[0], false)
            updateFieldsVisibility(0)
        }

        // انتخاب تاریخ یادآوری
        etReminderDate.setOnClickListener {
            showDatePicker(allowPast = false) { date ->
                etReminderDate.setText(DateUtils.formatLocalDate(date))
                etReminderDate.tag = date
            }
        }

        // انتخاب تاریخ گرفتن قرض
        etDebtDate.setOnClickListener {
            showDatePicker(allowPast = true) { date ->
                etDebtDate.setText(DateUtils.formatLocalDate(date))
                etDebtDate.tag = date
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isEdit) "ویرایش یادآوری" else "افزودن یادآوری جدید")
            .setView(dialogView)
            .setPositiveButton("ذخیره") { _, _ ->
                try {
                    val selectedTypeText = spType.text.toString()
                    val typeIndex = typeLabels.indexOf(selectedTypeText)
                    if (typeIndex < 0) {
                        Toast.makeText(requireContext(), "نوع یادآوری را انتخاب کنید", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val type = viewModel.allTypes[typeIndex]

                    val amountText = etAmount.text.toString().trim()
                        .replace(",", "")
                        .replace("٬", "")
                        .replace(" ", "")
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        Toast.makeText(requireContext(), "مبلغ را به صورت عدد صحیح وارد کنید", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val reminderDateStr = etReminderDate.text.toString().trim()
                    if (reminderDateStr.isBlank()) {
                        Toast.makeText(requireContext(), "تاریخ یادآوری الزامی است", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val reminderDate = LocalDate.parse(reminderDateStr, dateFormatter)
                    if (reminderDate.isBefore(LocalDate.now())) {
                        Toast.makeText(requireContext(), "تاریخ یادآوری نمی‌تواند قبل از امروز باشد", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val id = existing?.id ?: 0L

                    val reminder = when (type) {
                        ReminderType.CHECK -> {
                            val bank = etBank.text.toString().trim()
                            val beneficiary = etBeneficiary.text.toString().trim()
                            if (bank.isBlank()) {
                                Toast.makeText(requireContext(), "نام بانک الزامی است", Toast.LENGTH_SHORT).show()
                                return@setPositiveButton
                            }
                            ReminderEntity(
                                id = id,
                                type = type,
                                title = "چک",
                                bank = bank,
                                beneficiary = beneficiary.ifBlank { null },
                                amount = amount,
                                checkDate = reminderDate,
                                reminderDate = reminderDate,
                                status = ReminderStatus.ACTIVE,
                                remindDaily = true
                            )
                        }
                        ReminderType.LOAN -> {
                            val bank = etLoanBank.text.toString().trim()
                            if (bank.isBlank()) {
                                Toast.makeText(requireContext(), "نام بانک الزامی است", Toast.LENGTH_SHORT).show()
                                return@setPositiveButton
                            }
                            val installment = etInstallmentAmount.text.toString().trim()
                                .replace(",", "").replace("٬", "").toDoubleOrNull() ?: amount
                            val remaining = etRemainingInstallments.text.toString().trim().toIntOrNull() ?: 0
                            ReminderEntity(
                                id = id,
                                type = type,
                                title = "وام",
                                bank = bank,
                                amount = amount,
                                installmentAmount = installment,
                                remainingInstallments = remaining,
                                checkDate = reminderDate,
                                reminderDate = reminderDate,
                                status = ReminderStatus.ACTIVE,
                                remindDaily = true
                            )
                        }
                        ReminderType.DEBT -> {
                            val debtDateStr = etDebtDate.text.toString().trim()
                            val debtDate = if (debtDateStr.isNotBlank()) {
                                LocalDate.parse(debtDateStr, dateFormatter)
                            } else null
                            val description = etDescription.text.toString().trim()
                            ReminderEntity(
                                id = id,
                                type = type,
                                title = "قرض/بدهی",
                                amount = amount,
                                debtDate = debtDate,
                                description = description.ifBlank { null },
                                reminderDate = reminderDate,
                                status = ReminderStatus.ACTIVE,
                                remindDaily = true
                            )
                        }
                        ReminderType.OTHER -> {
                            val title = etTitle.text.toString().trim()
                            if (title.isBlank()) {
                                Toast.makeText(requireContext(), "عنوان الزامی است", Toast.LENGTH_SHORT).show()
                                return@setPositiveButton
                            }
                            ReminderEntity(
                                id = id,
                                type = type,
                                title = title,
                                amount = amount,
                                reminderDate = reminderDate,
                                status = ReminderStatus.ACTIVE,
                                remindDaily = true
                            )
                        }
                    }

                    viewModel.setReminder(reminder)
                    Toast.makeText(requireContext(), "یادآوری ذخیره شد ✅", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "خطا: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun showDatePicker(allowPast: Boolean = false, onDateSelected: (LocalDate) -> Unit) {
        val minDate = if (allowPast) null else LocalDate.now()
        PersianDatePickerHelper.show(
            context = requireContext(),
            initial = LocalDate.now(),
            minDate = minDate,
            maxDate = null,
            onDateSelected = onDateSelected
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}