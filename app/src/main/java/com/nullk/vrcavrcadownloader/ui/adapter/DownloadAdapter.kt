package com.nullk.vrcavrcadownloader.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nullk.vrcavrcadownloader.R
import com.nullk.vrcavrcadownloader.cache.CacheManager
import com.nullk.vrcavrcadownloader.data.model.DownloadStatus
import com.nullk.vrcavrcadownloader.data.model.DownloadTask

class DownloadAdapter(
    private val onCancelClick: (DownloadTask) -> Unit,
    private val onRetryClick: (DownloadTask) -> Unit
) : ListAdapter<DownloadTask, DownloadAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_download, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
        private val tvSpeed: TextView = itemView.findViewById(R.id.tvSpeed)
        private val tvSize: TextView = itemView.findViewById(R.id.tvSize)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val btnAction: ImageButton = itemView.findViewById(R.id.btnAction)
        
        fun bind(task: DownloadTask) {
            val avatar = task.avatar
            
            tvName.text = avatar.name
            tvProgress.text = task.progressText
            tvSpeed.text = task.speedText
            tvSize.text = task.sizeText
            progressBar.progress = task.progress
            
            // Load image
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
            
            // Status and action button
            when (task.status) {
                DownloadStatus.PENDING -> {
                    tvStatus.text = "等待中..."
                    tvStatus.setTextColor(itemView.context.getColor(R.color.text_secondary))
                    btnAction.setImageResource(R.drawable.ic_cancel)
                    btnAction.setOnClickListener { onCancelClick(task) }
                }
                DownloadStatus.DOWNLOADING -> {
                    tvStatus.text = "下载中..."
                    tvStatus.setTextColor(itemView.context.getColor(R.color.info))
                    btnAction.setImageResource(R.drawable.ic_cancel)
                    btnAction.setOnClickListener { onCancelClick(task) }
                }
                DownloadStatus.PAUSED -> {
                    tvStatus.text = "已暂停"
                    tvStatus.setTextColor(itemView.context.getColor(R.color.warning))
                    btnAction.setImageResource(R.drawable.ic_retry)
                    btnAction.setOnClickListener { onRetryClick(task) }
                }
                DownloadStatus.COMPLETED -> {
                    tvStatus.text = "下载完成"
                    tvStatus.setTextColor(itemView.context.getColor(R.color.success))
                    btnAction.setImageResource(R.drawable.ic_clear)
                    btnAction.setOnClickListener { onCancelClick(task) }
                }
                DownloadStatus.FAILED -> {
                    tvStatus.text = "下载失败: ${task.errorMessage ?: ""}"
                    tvStatus.setTextColor(itemView.context.getColor(R.color.error))
                    btnAction.setImageResource(R.drawable.ic_retry)
                    btnAction.setOnClickListener { onRetryClick(task) }
                }
                DownloadStatus.CANCELLED -> {
                    tvStatus.text = "已取消"
                    tvStatus.setTextColor(itemView.context.getColor(R.color.text_secondary))
                    btnAction.setImageResource(R.drawable.ic_retry)
                    btnAction.setOnClickListener { onRetryClick(task) }
                }
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<DownloadTask>() {
        override fun areItemsTheSame(oldItem: DownloadTask, newItem: DownloadTask): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: DownloadTask, newItem: DownloadTask): Boolean {
            return oldItem == newItem
        }
    }
}
