package com.op.banktransactiontracker.ui

import android.content.Context
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.op.banktransactiontracker.data.ReminderEntity.ReminderType

object ReminderChart {
    fun setupPieChart(chart: PieChart, totals: Map<ReminderType, Double>) {
        val entries = totals.entries.map { (type, amount) ->
            PieEntry(amount.toFloat(), type.name)
        }

        val dataSet = PieDataSet(entries, "مجموع مبالغ")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 14f

        val pieData = PieData(dataSet)
        chart.data = pieData
        chart.description.isEnabled = false
        chart.setUsePercentValues(true)
        chart.invalidate()
    }
}