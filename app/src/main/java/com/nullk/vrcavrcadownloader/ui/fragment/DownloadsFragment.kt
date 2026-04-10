package com.nullk.vrcavrcadownloader.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nullk.vrcavrcadownloader.R
import com.nullk.vrcavrcadownloader.data.model.DownloadStatus
import com.nullk.vrcavrcadownloader.download.DownloadManager
import com.nullk.vrcavrcadownloader.ui.adapter.DownloadAdapter
import kotlinx.coroutines.launch

class DownloadsFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DownloadAdapter
    private lateinit var btnRetryAll: MaterialButton
    private lateinit var btnClearCompleted: MaterialButton
    private lateinit var tvEmpty: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_downloads, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupListeners()
        observeDownloads()
    }
    
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        btnRetryAll = view.findViewById(R.id.btnRetryAll)
        btnClearCompleted = view.findViewById(R.id.btnClearCompleted)
        tvEmpty = view.findViewById(R.id.tvEmpty)
    }
    
    private fun setupRecyclerView() {
        adapter = DownloadAdapter(
            onCancelClick = { task ->
                when (task.status) {
                    DownloadStatus.COMPLETED -> {
                        showDeleteConfirmDialog(task.id)
                    }
                    else -> {
                        DownloadManager.cancelDownload(task.id)
                    }
                }
            },
            onRetryClick = { task ->
                DownloadManager.retryDownload(task.id)
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }
    
    private fun setupListeners() {
        btnRetryAll.setOnClickListener {
            DownloadManager.retryAllFailed()
            Toast.makeText(context, "重试所有失败任务", Toast.LENGTH_SHORT).show()
        }
        
        btnClearCompleted.setOnClickListener {
            DownloadManager.clearCompleted()
            Toast.makeText(context, "已清理已完成任务", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeDownloads() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                DownloadManager.tasks.collect { tasks ->
                    adapter.submitList(tasks)
                    updateEmptyView(tasks.isEmpty())
                }
            }
        }
    }
    
    private fun updateEmptyView(isEmpty: Boolean) {
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    private fun showDeleteConfirmDialog(taskId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                DownloadManager.removeTask(taskId)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
}
