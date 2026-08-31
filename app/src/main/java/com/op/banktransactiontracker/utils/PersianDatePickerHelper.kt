package com.op.banktransactiontracker.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.NumberPicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.op.banktransactiontracker.R
import java.time.LocalDate

object PersianDatePickerHelper {

    private val monthNames = arrayOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    /**
     * @param minDate حداقل تاریخ قابل انتخاب (شمسی تبدیل‌شده از LocalDate) — null یعنی محدود نباشد
     * @param maxDate حداکثر تاریخ — null یعنی محدود نباشد
     * @param initial تاریخ اولیه (میلادی)
     * @param onDateSelected callback با LocalDate میلادی
     */
    fun show(
        context: Context,
        initial: LocalDate = LocalDate.now(),
        minDate: LocalDate? = null,
        maxDate: LocalDate? = null,
        onDateSelected: (LocalDate) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_persian_date_picker, null)

        val npYear = view.findViewById<NumberPicker>(R.id.npYear)
        val npMonth = view.findViewById<NumberPicker>(R.id.npMonth)
        val npDay = view.findViewById<NumberPicker>(R.id.npDay)

        val todayJalali = DateUtils.toJalali(LocalDate.now())
        val initJalali = DateUtils.toJalali(initial)

        val minYear = minDate?.let { DateUtils.toJalali(it).year } ?: (todayJalali.year - 20)
        val maxYear = maxDate?.let { DateUtils.toJalali(it).year } ?: (todayJalali.year + 20)

        npYear.minValue = minYear
        npYear.maxValue = maxYear
        npYear.value = initJalali.year.coerceIn(minYear, maxYear)
        npYear.wrapSelectorWheel = false

        npMonth.minValue = 1
        npMonth.maxValue = 12
        npMonth.displayedValues = monthNames
        npMonth.value = initJalali.month.coerceIn(1, 12)
        npMonth.wrapSelectorWheel = false

        fun daysInMonth(year: Int, month: Int): Int {
            return when (month) {
                in 1..6 -> 31
                in 7..11 -> 30
                12 -> if (isJalaliLeap(year)) 30 else 29
                else -> 31
            }
        }

        fun updateDayPicker() {
            val maxDay = daysInMonth(npYear.value, npMonth.value)
            val oldDay = npDay.value
            npDay.minValue = 1
            npDay.maxValue = maxDay
            npDay.value = oldDay.coerceIn(1, maxDay)
            npDay.wrapSelectorWheel = false
        }

        npDay.minValue = 1
        npDay.maxValue = daysInMonth(npYear.value, npMonth.value)
        npDay.value = initJalali.day.coerceIn(1, npDay.maxValue)
        npDay.wrapSelectorWheel = false

        npYear.setOnValueChangedListener { _, _, _ -> updateDayPicker() }
        npMonth.setOnValueChangedListener { _, _, _ -> updateDayPicker() }

        MaterialAlertDialogBuilder(context)
            .setTitle("انتخاب تاریخ")
            .setView(view)
            .setPositiveButton("تأیید") { _, _ ->
                val jy = npYear.value
                val jm = npMonth.value
                val jd = npDay.value
                val localDate = DateUtils.fromJalali(jy, jm, jd)

                if (minDate != null && localDate.isBefore(minDate)) {
                    android.widget.Toast.makeText(
                        context,
                        "تاریخ نمی‌تواند قبل از حداقل مجاز باشد",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                if (maxDate != null && localDate.isAfter(maxDate)) {
                    android.widget.Toast.makeText(
                        context,
                        "تاریخ از حداکثر مجاز بیشتر است",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                onDateSelected(localDate)
            }
            .setNegativeButton("لغو", null)
            .show()
    }

    /** سال کبیسه شمسی */
    private fun isJalaliLeap(year: Int): Boolean {
        val breaks = intArrayOf(
            -61, 9, 38, 199, 426, 686, 756, 818, 1111,
            1181, 1210, 1635, 2060, 2097, 2192, 2262,
            2324, 2394, 2456, 3178
        )
        var jp = breaks[0]
        var jump = 0
        for (i in 1 until breaks.size) {
            val jm = breaks[i]
            jump = jm - jp
            if (year < jm) break
            jp = jm
        }
        var n = year - jp
        if (jump - n < 6) n = n - jump + ((jump + 4) / 33) * 33
        var leap = ((n + 1) % 33) - 1
        if (leap == -1) leap = 32
        return leap == 1 || leap == 5 || leap == 9 || leap == 13 ||
                leap == 17 || leap == 22 || leap == 26 || leap == 30
    }
}