package com.aquamanagers.aquamanage_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aquamanagers.aquamanage_app.R

class UsesAdapter(private val images: List<Int>): RecyclerView.Adapter<UsesAdapter.UsesViewHolder>(){

    class UsesViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val imageView: ImageView = view.findViewById(R.id.itemImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_horizontal_image, parent, false)
        return UsesViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsesViewHolder, position: Int) {
        holder.imageView.setImageResource(images[position])
    }

    override fun getItemCount(): Int = images.size
}