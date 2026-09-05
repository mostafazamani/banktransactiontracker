package com.op.banktransactiontracker.utils

import com.op.banktransactiontracker.utils.DateUtils.fromJalali
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale

object DateUtils {

    // ────────── تبدیل میلادی ↔ شمسی ──────────

    data class JalaliDate(val year: Int, val month: Int, val day: Int) {
        /** مثلاً ۱۴۰۳/۰۵/۱۶ */
        fun format(separator: String = "/"): String {
            return "${toPersianDigits(year)}$separator${toPersianDigits(month.toString().padStart(2, '0'))}$separator${toPersianDigits(day.toString().padStart(2, '0'))}"
        }

        /** مثلاً ۱۶ مرداد ۱۴۰۳ */
        fun formatLong(): String {
            val monthName = persianMonthNames.getOrElse(month - 1) { "" }
            return "${toPersianDigits(day)} $monthName ${toPersianDigits(year)}"
        }
    }

    private val persianMonthNames = arrayOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    fun toJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        var gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
                ((gy2 + 399) / 400) + gd + gdm[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        return JalaliDate(jy, jm, jd)
    }

    fun toJalali(date: LocalDate): JalaliDate =
        toJalali(date.year, date.monthValue, date.dayOfMonth)

    fun toJalali(millis: Long): JalaliDate {
        val date = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return toJalali(date)
    }

    fun fromJalali(jy: Int, jm: Int, jd: Int): LocalDate {
        var gy: Int
        val gm: Int
        val gd: Int
        val jy2 = jy + 1595
        var days = -355668 + (365 * jy2) + ((jy2 / 33) * 8) + (((jy2 % 33) + 3) / 4) + jd +
                if (jm < 7) (jm - 1) * 31 else ((jm - 7) * 30) + 186
        gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            days--
            gy += 100 * (days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        var gyFinal = gy + 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gyFinal += (days - 1) / 365
            days = (days - 1) % 365
        }
        gd = days + 1
        val salA = if ((gyFinal % 4 == 0 && gyFinal % 100 != 0) || (gyFinal % 400 == 0)) 1 else 0
        val v = intArrayOf(0, 31, salA + 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gmTemp = 0
        var gdTemp = gd
        while (gmTemp < 13 && gdTemp > v[gmTemp]) {
            gdTemp -= v[gmTemp]
            gmTemp++
        }
        gm = gmTemp
        return LocalDate.of(gyFinal, gm, gdTemp)
    }

    // ────────── فرمت نمایش ──────────

    /** تاریخ + ساعت شمسی از میلی‌ثانیه — مثلاً ۱۴۰۳/۰۵/۱۶  ۱۴:۳۰ */
    fun formatDateTime(millis: Long): String {
        val jalali = toJalali(millis)
        val time = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        val h = time.hour.toString().padStart(2, '0')
        val m = time.minute.toString().padStart(2, '0')
        return "${jalali.format()}  ${toPersianDigits("$h:$m")}"
    }

    /** فقط تاریخ شمسی از میلی‌ثانیه */
    fun formatDateOnly(millis: Long): String = toJalali(millis).format()

    /** تاریخ شمسی از LocalDate — مثلاً ۱۴۰۳/۰۵/۱۶ */
    fun formatLocalDate(date: LocalDate): String = toJalali(date).format()

    /** تاریخ شمسی بلند — مثلاً ۱۶ مرداد ۱۴۰۳ */
    fun formatLocalDateLong(date: LocalDate): String = toJalali(date).formatLong()

    fun getStartOfDay(millis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getEndOfDay(millis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun toPersianDigits(input: String): String {
        val persian = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (c in input) {
            if (c in '0'..'9') sb.append(persian[c - '0']) else sb.append(c)
        }
        return sb.toString()
    }

    fun toPersianDigits(number: Int): String = toPersianDigits(number.toString())

    fun toEnglishDigits(input: String): String {
        val persian = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (c in input) {
            val index = persian.indexOf(c)
            if (index >= 0) sb.append('0' + index) else sb.append(c)
        }
        return sb.toString()
    }

    /**
     * پارس تاریخ شمسی (با رقم فارسی یا انگلیسی)
     * فرمت‌های قابل قبول: ۱۴۰۵/۰۶/۰۹   یا  1405/06/09   یا  1405-06-09
     * خروجی: LocalDate میلادی  یا  null اگر نامعتبر باشه
     */
    fun parseJalaliDate(input: String): LocalDate? {
        if (input.isBlank()) return null

        val normalized = toEnglishDigits(input.trim())
            .replace(" ", "")
            .replace("-", "/")
            .replace("٫", "/")   // نقطه اعشار فارسی گاهی اشتباهی استفاده می‌شه

        val parts = normalized.split("/")
        if (parts.size != 3) return null

        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null

        // اعتبارسنجی ساده
        if (year < 1300 || year > 1500) return null
        if (month !in 1..12) return null
        if (day !in 1..31) return null

        return try {
            fromJalali(year, month, day)
        } catch (e: Exception) {
            null
        }
    }
}/** تبدیل ارقام فارسی به انگلیسی */
