package com.aquamanagers.aquamanage_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.aquamanagers.aquamanage_app.R

class AvatarAdapter(
    private val avatars: List<Int>,
    private val onAvatarClick: (Int) -> Unit
) : RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_avatar, parent, false)
        return AvatarViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
        holder.bind(avatars[position])
    }

    override fun getItemCount(): Int = avatars.size

    inner class AvatarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatarImage: ImageView = itemView.findViewById(R.id.avatarImage)

        fun bind(avatarResId: Int) {
            avatarImage.setImageResource(avatarResId)
            itemView.setOnClickListener {
                onAvatarClick(avatarResId)
            }
        }
    }
}
