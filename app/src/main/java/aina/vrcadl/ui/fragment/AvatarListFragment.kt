package aina.vrcadl.ui.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import aina.vrcadl.R
import aina.vrcadl.api.VRChatApi
import aina.vrcadl.cache.CacheManager
import aina.vrcadl.data.model.Avatar
import aina.vrcadl.download.DownloadManager
import aina.vrcadl.ui.adapter.AvatarAdapter
import aina.vrcadl.utils.PreferenceManager
import kotlinx.coroutines.launch
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class AvatarListFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AvatarAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var etSearch: EditText
    private lateinit var btnSync: MaterialButton
    private lateinit var btnDownloadSelected: MaterialButton
    private lateinit var cbSelectAll: MaterialCheckBox
    private lateinit var tvEmpty: TextView
    private lateinit var fabScrollTop: FloatingActionButton
    
    private var allAvatars = listOf<Avatar>()
    private var filteredAvatars = listOf<Avatar>()
    
    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Handle folder selection
                val path = uri.path
                if (path != null) {
                    PreferenceManager.setDownloadPath(path)
                }
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_avatar_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupListeners()
        loadCachedAvatars()
    }
    
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        etSearch = view.findViewById(R.id.etSearch)
        btnSync = view.findViewById(R.id.btnSync)
        btnDownloadSelected = view.findViewById(R.id.btnDownloadSelected)
        cbSelectAll = view.findViewById(R.id.cbSelectAll)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        fabScrollTop = view.findViewById(R.id.fabScrollTop)
    }
    
    private fun setupRecyclerView() {
        adapter = AvatarAdapter(
            onItemClick = { avatar ->
                // Show avatar details or download
                DownloadManager.addDownload(avatar)
                Toast.makeText(context, "开始下�? ${avatar.shortName}", Toast.LENGTH_SHORT).show()
            },
            onDownloadClick = { avatar ->
                DownloadManager.addDownload(avatar)
                Toast.makeText(context, "开始下�? ${avatar.shortName}", Toast.LENGTH_SHORT).show()
            },
            onSelectionChanged = { _, _ ->
                updateSelectAllCheckbox()
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()
                fabScrollTop.visibility = if (firstVisibleItem > 10) View.VISIBLE else View.GONE
            }
        })
    }
    
    private fun setupListeners() {
        // Search
        etSearch.addTextChangedListener { text ->
            filterAvatars(text?.toString() ?: "")
        }
        
        // Sync
        btnSync.setOnClickListener {
            syncAvatars()
        }
        
        // Download selected
        btnDownloadSelected.setOnClickListener {
            val selected = adapter.getSelectedAvatars()
            if (selected.isEmpty()) {
                Toast.makeText(context, R.string.toast_no_selection, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Filter out avatars without assetUrl
            val downloadable = selected.filter { it.assetUrl != null }
            val skipped = selected.size - downloadable.size
            
            if (downloadable.isEmpty()) {
                Toast.makeText(context, "选中�?Avatar 没有可下载的资源", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            if (skipped > 0) {
                Toast.makeText(context, "跳过 $skipped 个没有资源的 Avatar", Toast.LENGTH_SHORT).show()
            }
            
            // Check if download path is set
            if (PreferenceManager.getDownloadPath() == null) {
                pickDownloadFolder()
            } else {
                DownloadManager.addDownloads(downloadable)
                Toast.makeText(context, "开始下�?${downloadable.size} �?Avatar", Toast.LENGTH_SHORT).show()
                adapter.deselectAll()
            }
        }
        
        // Select all
        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                adapter.selectAll()
            } else {
                adapter.deselectAll()
            }
        }
        
        // Scroll to top
        fabScrollTop.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }
        
        // Swipe refresh
        swipeRefresh.setOnRefreshListener {
            syncAvatars()
        }
    }
    
    private fun loadCachedAvatars() {
        lifecycleScope.launch {
            val cached = CacheManager.loadAvatarList()
            if (cached != null) {
                allAvatars = cached
                filteredAvatars = cached
                adapter.submitList(filteredAvatars)
                updateEmptyView()
            }
        }
    }
    
    private fun syncAvatars() {
        swipeRefresh.isRefreshing = true
        btnSync.isEnabled = false

        lifecycleScope.launch {
            try {
                val result = VRChatApi.getInstance().getAvatarFiles()
                result.onSuccess { avatars ->
                    allAvatars = avatars
                    filteredAvatars = avatars
                    adapter.submitList(filteredAvatars)
                    updateEmptyView()
                    CacheManager.saveAvatarList(avatars)

                    // Pre-cache images
                    avatars.forEach { avatar ->
                        avatar.thumbnailUrl?.let { url ->
                            CacheManager.cacheImage(url, avatar.id)
                        }
                    }

                    Toast.makeText(context, "同步成功，共 ${avatars.size} �?Avatar", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(context, "${getString(R.string.toast_sync_failed)}: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "${getString(R.string.toast_sync_failed)}: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
                btnSync.isEnabled = true
            }
        }
    }
    
    private fun filterAvatars(query: String) {
        filteredAvatars = if (query.isEmpty()) {
            allAvatars
        } else {
            allAvatars.filter { avatar ->
                avatar.name.contains(query, ignoreCase = true) ||
                avatar.id.contains(query, ignoreCase = true)
            }
        }
        adapter.submitList(filteredAvatars)
        updateEmptyView()
    }
    
    private fun updateEmptyView() {
        tvEmpty.visibility = if (filteredAvatars.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (filteredAvatars.isEmpty()) View.GONE else View.VISIBLE
    }
    
    private fun updateSelectAllCheckbox() {
        val selectedCount = adapter.getSelectedIds().size
        val totalCount = adapter.currentList.size
        cbSelectAll.setOnCheckedChangeListener(null)
        cbSelectAll.isChecked = selectedCount == totalCount && totalCount > 0
        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                adapter.selectAll()
            } else {
                adapter.deselectAll()
            }
        }
    }
    
    private fun pickDownloadFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        folderPickerLauncher.launch(intent)
    }
}
