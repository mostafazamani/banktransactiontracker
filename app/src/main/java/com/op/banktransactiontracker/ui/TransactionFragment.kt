package com.op.banktransactiontracker.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.op.banktransactiontracker.data.BankEntry
import com.op.banktransactiontracker.data.TransactionEntity
import com.op.banktransactiontracker.databinding.DialogAddEditTransactionBinding
import com.op.banktransactiontracker.databinding.DialogManagePhonesBinding
import com.op.banktransactiontracker.databinding.FragmentTransactionsBinding
import com.op.banktransactiontracker.utils.DateUtils
import com.op.banktransactiontracker.utils.PersianDatePickerHelper
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

class TransactionFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by viewModels {
        TransactionViewModelFactory(requireActivity().application)
    }

    private lateinit var transactionAdapter: TransactionAdapter
    private var currentBankList: List<BankEntry> = emptyList()
    private val numberFormat = NumberFormat.getNumberInstance(Locale("fa"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupFab()
        observeData()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onEditClick = { showAddEditDialog(it) },
            onDeleteClick = { confirmDelete(it) }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString()?.trim())
            }
        })
    }

    private fun setupFilters() {
        binding.chipPhoneFilter.setOnClickListener { showPhoneFilterDialog() }
        binding.chipDateFilter.setOnClickListener { showDateFilterDialog() }
        binding.chipClearFilters.setOnClickListener {
            viewModel.clearFilters()
            binding.chipPhoneFilter.text = "همه بانک‌ها"
            binding.chipDateFilter.text = "همه تاریخ‌ها"
            binding.chipClearFilters.visibility = View.GONE
            binding.etSearch.setText("")
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener { showAddEditDialog(null) }
    }

    private fun observeData() {
        viewModel.transactions.observe(viewLifecycleOwner) { list ->
            transactionAdapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            updateTotals(list)
        }

        viewModel.banks.observe(viewLifecycleOwner) { banks ->
            currentBankList = banks
        }
    }

    private fun updateTotals(list: List<TransactionEntity>) {
        var totalDeposit = 0L
        var totalWithdrawal = 0L
        for (item in list) {
            if (item.type == "deposit") totalDeposit += item.amount
            else totalWithdrawal += item.amount
        }
        binding.tvTotalDeposit.text = "واریز: ${numberFormat.format(totalDeposit)} ریال"
        binding.tvTotalWithdrawal.text = "برداشت: ${numberFormat.format(totalWithdrawal)} ریال"
    }

    private fun showAddEditDialog(existing: TransactionEntity?) {
        val dialogBinding = DialogAddEditTransactionBinding.inflate(LayoutInflater.from(requireContext()))
        val isEdit = existing != null

        if (isEdit) {
            dialogBinding.etPhone.setText(existing!!.phoneNumber)
            dialogBinding.etSenderName.setText(existing.senderName)
            dialogBinding.etMessage.setText(if (existing.amount > 0) existing.amount.toString() else "")
            dialogBinding.etTitle.setText(existing.title)
            dialogBinding.etDescription.setText(existing.description)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isEdit) "ویرایش تراکنش" else "افزودن تراکنش دستی")
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEdit) "ذخیره" else "افزودن") { _, _ ->
                val phone = dialogBinding.etPhone.text.toString().trim()
                val sender = dialogBinding.etSenderName.text.toString().trim()
                val amountText = dialogBinding.etMessage.text.toString().trim()
                    .replace(",", "").replace("٬", "").replace(" ", "")
                val title = dialogBinding.etTitle.text.toString().trim().ifBlank { "دستی" }
                val description = dialogBinding.etDescription.text.toString().trim()

                if (phone.isBlank()) {
                    Toast.makeText(requireContext(), "شماره الزامی است", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val amount = amountText.toLongOrNull() ?: 0L
                if (amount <= 0) {
                    Toast.makeText(requireContext(), "مبلغ را درست وارد کنید", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val options = arrayOf("واریز", "برداشت")
                val defaultIndex = if (existing?.type == "deposit") 0 else 1

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("نوع تراکنش")
                    .setSingleChoiceItems(options, defaultIndex, null)
                    .setPositiveButton("تأیید") { dialog, _ ->
                        val listView = (dialog as androidx.appcompat.app.AlertDialog).listView
                        val selected = listView.checkedItemPosition
                        val type = if (selected == 0) "deposit" else "withdrawal"
                        val messageBody = amount.toString()

                        if (isEdit) {
                            val updated = existing!!.copy(
                                phoneNumber = phone,
                                senderName = sender.ifBlank { phone },
                                messageBody = messageBody,
                                title = title,
                                description = description,
                                type = type,
                                amount = amount
                            )
                            viewModel.updateTransaction(updated)
                        } else {
                            val newItem = TransactionEntity(
                                dateTime = System.currentTimeMillis(),
                                senderName = sender.ifBlank { phone },
                                phoneNumber = phone,
                                messageBody = messageBody,
                                title = title,
                                description = description,
                                type = type,
                                amount = amount
                            )
                            viewModel.insertTransaction(newItem)
                        }
                    }
                    .setNegativeButton("لغو", null)
                    .show()
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun confirmDelete(item: TransactionEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("حذف تراکنش")
            .setMessage("آیا از حذف این تراکنش مطمئن هستید؟")
            .setPositiveButton("بله") { _, _ -> viewModel.deleteTransaction(item) }
            .setNegativeButton("خیر", null)
            .show()
    }

    private fun showPhoneFilterDialog() {
        val options = mutableListOf("همه بانک‌ها")
        options.addAll(currentBankList.map { it.name })

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("فیلتر بر اساس بانک")
            .setItems(options.toTypedArray()) { _, which ->
                if (which == 0) {
                    viewModel.setBankFilter(null)
                    binding.chipPhoneFilter.text = "همه بانک‌ها"
                } else {
                    val selected = currentBankList[which - 1].name
                    viewModel.setBankFilter(selected)
                    binding.chipPhoneFilter.text = selected
                }
                updateClearFilterVisibility()
            }
            .show()
    }

    private fun showDateFilterDialog() {
        val options = arrayOf("همه تاریخ‌ها", "امروز", "۷ روز اخیر", "۳۰ روز اخیر", "انتخاب بازه دلخواه")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("فیلتر تاریخ")
            .setItems(options) { _, which ->
                val now = System.currentTimeMillis()
                when (which) {
                    0 -> {
                        viewModel.setDateRange(null, null)
                        binding.chipDateFilter.text = "همه تاریخ‌ها"
                    }
                    1 -> {
                        viewModel.setDateRange(DateUtils.getStartOfDay(now), DateUtils.getEndOfDay(now))
                        binding.chipDateFilter.text = "امروز"
                    }
                    2 -> {
                        viewModel.setDateRange(now - 7 * 24 * 60 * 60 * 1000L, now)
                        binding.chipDateFilter.text = "۷ روز اخیر"
                    }
                    3 -> {
                        viewModel.setDateRange(now - 30 * 24 * 60 * 60 * 1000L, now)
                        binding.chipDateFilter.text = "۳۰ روز اخیر"
                    }
                    4 -> showCustomDateRangeDialog()
                }
                updateClearFilterVisibility()
            }
            .show()
    }

    private fun showCustomDateRangeDialog() {
        PersianDatePickerHelper.show(
            context = requireContext(),
            initial = LocalDate.now().minusMonths(1),
            onDateSelected = { startLocal ->
                val from = startLocal.atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli()

                PersianDatePickerHelper.show(
                    context = requireContext(),
                    initial = LocalDate.now(),
                    minDate = startLocal,
                    onDateSelected = { endLocal ->
                        val to = endLocal.atTime(23, 59, 59)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant().toEpochMilli()

                        viewModel.setDateRange(from, to)
                        binding.chipDateFilter.text =
                            "${DateUtils.formatLocalDate(startLocal)} تا ${DateUtils.formatLocalDate(endLocal)}"
                        updateClearFilterVisibility()
                    }
                )
            }
        )
    }

    private fun updateClearFilterVisibility() {
        val hasFilter = binding.chipPhoneFilter.text != "همه بانک‌ها" ||
                binding.chipDateFilter.text != "همه تاریخ‌ها" ||
                !binding.etSearch.text.isNullOrBlank()
        binding.chipClearFilters.visibility = if (hasFilter) View.VISIBLE else View.GONE
    }

    /** فراخوانی از MainActivity برای دیالوگ مدیریت بانک‌ها */
    fun showManagePhonesDialog() {
        val dialogBinding = DialogManagePhonesBinding.inflate(LayoutInflater.from(requireContext()))
        val phoneAdapter = PhoneAdapter(
            onRemoveBank = { bank ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("حذف بانک")
                    .setMessage("آیا بانک «${bank.name}» حذف شود؟")
                    .setPositiveButton("حذف") { _, _ -> viewModel.removeBank(bank.name) }
                    .setNegativeButton("لغو", null)
                    .show()
            },
            onEditNumber = { bankName, oldNumber ->
                val input = android.widget.EditText(requireContext()).apply {
                    setText(oldNumber)
                    hint = "شماره جدید"
                }
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("ویرایش شماره")
                    .setView(input)
                    .setPositiveButton("ذخیره") { _, _ ->
                        val newNumber = input.text.toString().trim()
                        if (newNumber.isNotBlank()) {
                            viewModel.editNumberInBank(bankName, oldNumber, newNumber)
                        }
                    }
                    .setNegativeButton("لغو", null)
                    .show()
            },
            onRemoveNumber = { bankName, number ->
                viewModel.removeNumberFromBank(bankName, number)
            }
        )

        dialogBinding.rvPhones.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.rvPhones.adapter = phoneAdapter

        viewModel.banks.observe(viewLifecycleOwner) { banks ->
            phoneAdapter.submitList(banks)
        }

        dialogBinding.btnAddPhone.setOnClickListener {
            val name = dialogBinding.etBankName.text.toString().trim()
            val numbersRaw = dialogBinding.etNewPhone.text.toString().trim()
            if (name.isBlank() || numbersRaw.isBlank()) {
                Toast.makeText(requireContext(), "نام بانک و شماره الزامی است", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val numbers = numbersRaw.split(",", "،").map { it.trim() }.filter { it.isNotBlank() }
            viewModel.addBank(name, numbers)
            dialogBinding.etBankName.setText("")
            dialogBinding.etNewPhone.setText("")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("مدیریت بانک‌ها و شماره‌ها")
            .setView(dialogBinding.root)
            .setPositiveButton("بستن", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}