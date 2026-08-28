package com.op.banktransactiontracker

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.op.banktransactiontracker.data.TransactionEntity
import com.op.banktransactiontracker.databinding.ActivityMainBinding
import com.op.banktransactiontracker.databinding.DialogAddEditTransactionBinding
import com.op.banktransactiontracker.databinding.DialogManagePhonesBinding
import com.op.banktransactiontracker.ui.PhoneAdapter
import com.op.banktransactiontracker.ui.TransactionAdapter
import com.op.banktransactiontracker.ui.TransactionViewModel
import com.op.banktransactiontracker.ui.TransactionViewModelFactory
import com.op.banktransactiontracker.utils.DateUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TransactionViewModel by viewModels {
        TransactionViewModelFactory(application)
    }

    private lateinit var transactionAdapter: TransactionAdapter
    private var currentPhoneList: List<String> = emptyList()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "مجوزها با موفقیت دریافت شد", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "برای کارکرد صحیح برنامه مجوزها لازم است", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupFab()
        observeData()
        checkPermissions()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onEditClick = { showAddEditDialog(it) },
            onDeleteClick = { confirmDelete(it) }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
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
            binding.chipPhoneFilter.text = "همه شماره‌ها"
            binding.chipDateFilter.text = "همه تاریخ‌ها"
            binding.chipClearFilters.visibility = View.GONE
            binding.etSearch.setText("")
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddEditDialog(null)
        }
    }

    private fun observeData() {
        viewModel.transactions.observe(this) { list ->
            transactionAdapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.phoneNumbers.observe(this) { phones ->
            currentPhoneList = phones.toList().sorted()
        }
    }

    // -------------------- دیالوگ‌ها --------------------

    private fun showAddEditDialog(existing: TransactionEntity?) {
        val dialogBinding = DialogAddEditTransactionBinding.inflate(LayoutInflater.from(this))
        val isEdit = existing != null

        if (isEdit) {
            dialogBinding.etPhone.setText(existing!!.phoneNumber)
            dialogBinding.etSenderName.setText(existing.senderName)
            dialogBinding.etMessage.setText(existing.messageBody)
            dialogBinding.etTitle.setText(existing.title)
            dialogBinding.etDescription.setText(existing.description)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(if (isEdit) "ویرایش تراکنش" else "افزودن تراکنش دستی")
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEdit) "ذخیره" else "افزودن") { _, _ ->
                val phone = dialogBinding.etPhone.text.toString().trim()
                val sender = dialogBinding.etSenderName.text.toString().trim()
                val message = dialogBinding.etMessage.text.toString().trim()
                val title = dialogBinding.etTitle.text.toString().trim().ifBlank { "دستی" }
                val description = dialogBinding.etDescription.text.toString().trim()

                if (phone.isBlank() || message.isBlank()) {
                    Toast.makeText(this, "شماره و متن پیام الزامی است", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (isEdit) {
                    val updated = existing!!.copy(
                        phoneNumber = phone,
                        senderName = sender.ifBlank { phone },
                        messageBody = message,
                        title = title,
                        description = description
                    )
                    viewModel.updateTransaction(updated)
                } else {
                    val newItem = TransactionEntity(
                        dateTime = System.currentTimeMillis(),
                        senderName = sender.ifBlank { phone },
                        phoneNumber = phone,
                        messageBody = message,
                        title = title,
                        description = description
                    )
                    viewModel.insertTransaction(newItem)
                }
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    private fun confirmDelete(item: TransactionEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle("حذف تراکنش")
            .setMessage("آیا از حذف این تراکنش مطمئن هستید؟")
            .setPositiveButton("بله") { _, _ ->
                viewModel.deleteTransaction(item)
            }
            .setNegativeButton("خیر", null)
            .show()
    }

    private fun showPhoneFilterDialog() {
        val options = mutableListOf("همه شماره‌ها")
        options.addAll(currentPhoneList)

        MaterialAlertDialogBuilder(this)
            .setTitle("فیلتر بر اساس شماره")
            .setItems(options.toTypedArray()) { _, which ->
                if (which == 0) {
                    viewModel.setPhoneFilter(null)
                    binding.chipPhoneFilter.text = "همه شماره‌ها"
                } else {
                    val selected = currentPhoneList[which - 1]
                    viewModel.setPhoneFilter(selected)
                    binding.chipPhoneFilter.text = selected
                }
                updateClearFilterVisibility()
            }
            .show()
    }

    private fun showDateFilterDialog() {
        val options = arrayOf("همه تاریخ‌ها", "امروز", "۷ روز اخیر", "۳۰ روز اخیر", "انتخاب بازه دلخواه")

        MaterialAlertDialogBuilder(this)
            .setTitle("فیلتر تاریخ")
            .setItems(options) { _, which ->
                val now = System.currentTimeMillis()
                when (which) {
                    0 -> {
                        viewModel.setDateRange(null, null)
                        binding.chipDateFilter.text = "همه تاریخ‌ها"
                    }
                    1 -> {
                        val start = DateUtils.getStartOfDay(now)
                        val end = DateUtils.getEndOfDay(now)
                        viewModel.setDateRange(start, end)
                        binding.chipDateFilter.text = "امروز"
                    }
                    2 -> {
                        val start = now - 7 * 24 * 60 * 60 * 1000L
                        viewModel.setDateRange(start, now)
                        binding.chipDateFilter.text = "۷ روز اخیر"
                    }
                    3 -> {
                        val start = now - 30 * 24 * 60 * 60 * 1000L
                        viewModel.setDateRange(start, now)
                        binding.chipDateFilter.text = "۳۰ روز اخیر"
                    }
                    4 -> showCustomDateRangeDialog()
                }
                updateClearFilterVisibility()
            }
            .show()
    }

    private fun showCustomDateRangeDialog() {
        val calendar = Calendar.getInstance()

        // تاریخ شروع
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                val from = DateUtils.getStartOfDay(calendar.timeInMillis)

                // تاریخ پایان
                DatePickerDialog(
                    this,
                    { _, y2, m2, d2 ->
                        calendar.set(y2, m2, d2)
                        val to = DateUtils.getEndOfDay(calendar.timeInMillis)
                        viewModel.setDateRange(from, to)
                        binding.chipDateFilter.text =
                            "${DateUtils.formatDateOnly(from)} تا ${DateUtils.formatDateOnly(to)}"
                        updateClearFilterVisibility()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateClearFilterVisibility() {
        val hasFilter = viewModel.selectedPhone.value != null ||
                viewModel.fromDate.value != null ||
                !binding.etSearch.text.isNullOrBlank()
        binding.chipClearFilters.visibility = if (hasFilter) View.VISIBLE else View.GONE
    }

    private fun showManagePhonesDialog() {
        val dialogBinding = DialogManagePhonesBinding.inflate(LayoutInflater.from(this))
        val phoneAdapter = PhoneAdapter { phone ->
            viewModel.removePhoneNumber(phone)
        }
        dialogBinding.rvPhones.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvPhones.adapter = phoneAdapter

        // مشاهده لیست زنده
        viewModel.phoneNumbers.observe(this) { phones ->
            phoneAdapter.submitList(phones.toList().sorted())
        }

        dialogBinding.btnAddPhone.setOnClickListener {
            val number = dialogBinding.etNewPhone.text.toString().trim()
            if (number.isNotBlank()) {
                viewModel.addPhoneNumber(number)
                dialogBinding.etNewPhone.setText("")
            } else {
                Toast.makeText(this, "شماره را وارد کنید", Toast.LENGTH_SHORT).show()
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("مدیریت شماره‌های بانکی")
            .setView(dialogBinding.root)
            .setPositiveButton("بستن", null)
            .show()
    }

    // -------------------- مجوزها --------------------

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needRequest = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needRequest) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_manage_phones -> {
                showManagePhonesDialog()
                true
            }
            R.id.action_request_permissions -> {
                checkPermissions()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}