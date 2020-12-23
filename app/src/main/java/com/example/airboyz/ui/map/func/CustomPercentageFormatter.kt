package com.example.airboyz.ui.map.func

import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat

// Shows percentage on pieChart value

class CustomPercentageFormatter : ValueFormatter() {

    private val mFormat = DecimalFormat("###,###,##0.0")


    override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
        return "${mFormat.format(value)}%"
    }
}