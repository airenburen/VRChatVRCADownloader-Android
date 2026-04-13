package aina.vrcadl.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import aina.vrcadl.BuildConfig
import aina.vrcadl.R
import aina.vrcadl.api.VRChatApi
import aina.vrcadl.cache.CacheManager
import aina.vrcadl.utils.PreferenceManager
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var etFilenameTemplate: EditText
    private lateinit var tvDownloadPath: TextView
    private lateinit var btnSelectPath: MaterialButton
    private lateinit var switchProxy: SwitchMaterial
    private lateinit var layoutProxySettings: LinearLayout
    private lateinit var etProxyHost: EditText
    private lateinit var etProxyPort: EditText
    private lateinit var btnTestProxy: MaterialButton
    private lateinit var tvCacheSize: TextView
    private lateinit var btnClearCache: MaterialButton
    private lateinit var tvVersion: TextView
    private lateinit var btnLogout: MaterialButton
    
    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Take persistable permission
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                PreferenceManager.setDownloadPath(uri.toString())
                updateDownloadPathDisplay()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        initViews()
        loadSettings()
        setupListeners()
        updateCacheSize()
    }
    
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        etFilenameTemplate = findViewById(R.id.etFilenameTemplate)
        tvDownloadPath = findViewById(R.id.tvDownloadPath)
        btnSelectPath = findViewById(R.id.btnSelectPath)
        switchProxy = findViewById(R.id.switchProxy)
        layoutProxySettings = findViewById(R.id.layoutProxySettings)
        etProxyHost = findViewById(R.id.etProxyHost)
        etProxyPort = findViewById(R.id.etProxyPort)
        btnTestProxy = findViewById(R.id.btnTestProxy)
        tvCacheSize = findViewById(R.id.tvCacheSize)
        btnClearCache = findViewById(R.id.btnClearCache)
        tvVersion = findViewById(R.id.tvVersion)
        btnLogout = findViewById(R.id.btnLogout)
        
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_settings)
        
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun loadSettings() {
        // Filename template
        etFilenameTemplate.setText(PreferenceManager.getFilenameTemplate() ?: "{short_name}")
        
        // Download path
        updateDownloadPathDisplay()
        
        // Proxy
        switchProxy.isChecked = PreferenceManager.isProxyEnabled()
        layoutProxySettings.visibility = if (switchProxy.isChecked) View.VISIBLE else View.GONE
        etProxyHost.setText(PreferenceManager.getProxyHost() ?: "")
        etProxyPort.setText(PreferenceManager.getProxyPort()?.toString() ?: "")
        
        // Version
        tvVersion.text = getString(R.string.setting_version, BuildConfig.VERSION_NAME)
    }
    
    private fun setupListeners() {
        // Filename template
        etFilenameTemplate.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                PreferenceManager.setFilenameTemplate(etFilenameTemplate.text.toString())
            }
        }
        
        // Download path
        btnSelectPath.setOnClickListener {
            pickDownloadFolder()
        }
        
        // Proxy
        switchProxy.setOnCheckedChangeListener { _, isChecked ->
            PreferenceManager.setProxyEnabled(isChecked)
            layoutProxySettings.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) {
                updateProxySettings()
            }
        }
        
        // Test proxy
        btnTestProxy.setOnClickListener {
            testProxy()
        }
        
        // Clear cache
        btnClearCache.setOnClickListener {
            showClearCacheDialog()
        }
        
        // Logout
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }
    
    private fun updateDownloadPathDisplay() {
        val path = PreferenceManager.getDownloadPath()
        tvDownloadPath.text = path ?: getString(R.string.setting_download_path_hint)
    }
    
    private fun pickDownloadFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        folderPickerLauncher.launch(intent)
    }
    
    private fun updateProxySettings() {
        val host = etProxyHost.text.toString().trim()
        val port = etProxyPort.text.toString().toIntOrNull() ?: 0
        
        PreferenceManager.setProxyHost(host)
        PreferenceManager.setProxyPort(port)
        
        VRChatApi.getInstance().updateProxy(host, port)
    }
    
    private fun testProxy() {
        val host = etProxyHost.text.toString().trim()
        val port = etProxyPort.text.toString().toIntOrNull() ?: 0
        
        if (host.isEmpty() || port <= 0) {
            Toast.makeText(this, "请输入有效的代理地址和端�?, Toast.LENGTH_SHORT).show()
            return
        }
        
        btnTestProxy.isEnabled = false
        btnTestProxy.text = "测试�?.."
        
        lifecycleScope.launch {
            val result = VRChatApi.getInstance().testProxy(host, port)
            result.onSuccess { success ->
                if (success) {
                    Toast.makeText(this@SettingsActivity, R.string.toast_proxy_test_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingsActivity, R.string.toast_proxy_test_failed, Toast.LENGTH_SHORT).show()
                }
            }.onFailure { error ->
                Toast.makeText(this@SettingsActivity, "${getString(R.string.toast_proxy_test_failed)}: ${error.message}", Toast.LENGTH_SHORT).show()
            }
            
            btnTestProxy.isEnabled = true
            btnTestProxy.text = getString(R.string.setting_proxy_test)
        }
    }
    
    private fun updateCacheSize() {
        lifecycleScope.launch {
            val size = CacheManager.getCacheSize()
            tvCacheSize.text = getString(R.string.setting_cache_size, CacheManager.formatCacheSize(size))
        }
    }
    
    private fun showClearCacheDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_clear_cache_title)
            .setMessage(R.string.dialog_clear_cache_message)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                lifecycleScope.launch {
                    CacheManager.clearCache()
                    updateCacheSize()
                    Toast.makeText(this@SettingsActivity, R.string.toast_cache_cleared, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
    
    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("退出登�?)
            .setMessage("确定要退出登录吗�?)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                PreferenceManager.clearAuth()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
    
    override fun onPause() {
        super.onPause()
        // Save settings
        PreferenceManager.setFilenameTemplate(etFilenameTemplate.text.toString())
        if (switchProxy.isChecked) {
            updateProxySettings()
        }
    }
}
