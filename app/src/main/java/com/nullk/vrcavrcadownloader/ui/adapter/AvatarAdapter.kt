package com.nullk.vrcavrcadownloader.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.checkbox.MaterialCheckBox
import com.nullk.vrcavrcadownloader.R
import com.nullk.vrcavrcadownloader.cache.CacheManager
import com.nullk.vrcavrcadownloader.data.model.Avatar

class AvatarAdapter(
    private val onItemClick: (Avatar) -> Unit,
    private val onDownloadClick: (Avatar) -> Unit,
    private val onSelectionChanged: (Avatar, Boolean) -> Unit
) : ListAdapter<Avatar, AvatarAdapter.ViewHolder>(DiffCallback()) {
    
    private var selectedIds = mutableSetOf<String>()
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_avatar, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    fun setSelectedIds(ids: Set<String>) {
        selectedIds = ids.toMutableSet()
        notifyDataSetChanged()
    }
    
    fun getSelectedIds(): Set<String> = selectedIds.toSet()
    
    fun selectAll() {
        selectedIds = currentList.map { it.id }.toMutableSet()
        notifyDataSetChanged()
    }
    
    fun deselectAll() {
        selectedIds.clear()
        notifyDataSetChanged()
    }
    
    fun getSelectedAvatars(): List<Avatar> {
        return currentList.filter { selectedIds.contains(it.id) }
    }
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cbSelect: MaterialCheckBox = itemView.findViewById(R.id.cbSelect)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvVersion: TextView = itemView.findViewById(R.id.tvVersion)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val btnDownload: ImageButton = itemView.findViewById(R.id.btnDownload)
        
        fun bind(avatar: Avatar) {
            tvName.text = avatar.name
            tvVersion.text = "Version: ${avatar.version}"
            tvDate.text = avatar.updatedAt?.substringBefore("T") ?: "Unknown"
            
            // Load image from cache or network
            val cachedImage = CacheManager.getCachedImage(avatar.id)
            if (cachedImage != null) {
                Glide.with(itemView.context)
                    .load(cachedImage)
                    .placeholder(R.drawable.bg_avatar_placeholder)
                    .into(ivAvatar)
            } else {
                Glide.with(itemView.context)
                    .load(avatar.thumbnailUrl)
                    .placeholder(R.drawable.bg_avatar_placeholder)
                    .into(ivAvatar)
            }
            
            // Selection state
            cbSelect.setOnCheckedChangeListener(null)
            cbSelect.isChecked = selectedIds.contains(avatar.id)
            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedIds.add(avatar.id)
                } else {
                    selectedIds.remove(avatar.id)
                }
                onSelectionChanged(avatar, isChecked)
            }
            
            // Click listeners
            itemView.setOnClickListener { onItemClick(avatar) }
            btnDownload.setOnClickListener { onDownloadClick(avatar) }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Avatar>() {
        override fun areItemsTheSame(oldItem: Avatar, newItem: Avatar): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Avatar, newItem: Avatar): Boolean {
            return oldItem == newItem
        }
    }
}
