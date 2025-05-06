package com.aquamanagers.aquamanage_app.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class ReminderAdapter(
    private val context: Context,
    private val layouts: Array<Int>
) : RecyclerView.Adapter<ReminderAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(layouts[viewType], parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = layouts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // No binding necessary for static layout pages
    }

    override fun getItemViewType(position: Int): Int = position
}

