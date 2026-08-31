package com.op.banktransactiontracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.op.banktransactiontracker.databinding.ActivityMainBinding
import com.op.banktransactiontracker.ui.ReminderFragment
import com.op.banktransactiontracker.ui.TransactionFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val transactionFragment = TransactionFragment()
    private val reminderFragment = ReminderFragment()
    private var activeFragment: Fragment = transactionFragment

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

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(binding.fragmentContainer.id, reminderFragment, "reminders")
                .hide(reminderFragment)
                .add(binding.fragmentContainer.id, transactionFragment, "transactions")
                .commit()
        } else {
            activeFragment = supportFragmentManager.findFragmentByTag("transactions")
                ?: transactionFragment
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_transactions -> {
                    switchFragment(transactionFragment)
                    binding.toolbar.title = "تراکنش‌ها"
                    true
                }
                R.id.nav_reminders -> {
                    switchFragment(reminderFragment)
                    binding.toolbar.title = "یادآوری‌ها"
                    true
                }
                else -> false
            }
        }

        binding.bottomNav.selectedItemId = R.id.nav_transactions
        checkPermissions()
    }

    private fun switchFragment(target: Fragment) {
        if (activeFragment == target) return
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(target)
            .commit()
        activeFragment = target
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_manage_phones -> {
                // فقط وقتی تب تراکنش فعال است
                if (activeFragment is TransactionFragment) {
                    (activeFragment as TransactionFragment).showManagePhonesDialog()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}