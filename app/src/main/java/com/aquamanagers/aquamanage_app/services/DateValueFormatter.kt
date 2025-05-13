package com.aquamanagers.aquamanage_app.services

import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateValueFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        return dateFormat.format(Date(value.toLong()))
    }
}
