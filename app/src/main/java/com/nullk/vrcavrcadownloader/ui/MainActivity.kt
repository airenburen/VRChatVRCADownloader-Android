package com.nullk.vrcavrcadownloader.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.nullk.vrcavrcadownloader.R
import com.nullk.vrcavrcadownloader.api.VRChatApi
import com.nullk.vrcavrcadownloader.ui.adapter.ViewPagerAdapter
import com.nullk.vrcavrcadownloader.utils.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var authContainer: View
    private lateinit var etCookie: EditText
    private lateinit var btnLoginCookie: MaterialButton
    private lateinit var btnLoginWebView: MaterialButton
    private lateinit var btnSettings: ImageButton
    
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupViewPager()
        setupListeners()
        checkLoginState()
        showWarningIfNeeded()
    }
    
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        authContainer = findViewById(R.id.authContainer)
        etCookie = findViewById(R.id.etCookie)
        btnLoginCookie = findViewById(R.id.btnLoginCookie)
        btnLoginWebView = findViewById(R.id.btnLoginWebView)
        btnSettings = findViewById(R.id.btnSettings)
        
        setSupportActionBar(toolbar)
    }
    
    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this)
        viewPager.adapter = viewPagerAdapter
        viewPager.offscreenPageLimit = 2
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = viewPagerAdapter.getPageTitle(position)
        }.attach()
    }
    
    private fun setupListeners() {
        // Login with cookie
        btnLoginCookie.setOnClickListener {
            val cookie = etCookie.text.toString().trim()
            if (cookie.isEmpty()) {
                Toast.makeText(this, "请输入 Cookie", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Extract auth value from cookie string
            val authValue = extractAuthCookie(cookie)
            if (authValue == null) {
                Toast.makeText(this, "Cookie 格式不正确", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            loginWithCookie(authValue)
        }
        
        // Login with WebView
        btnLoginWebView.setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            startActivityForResult(intent, REQUEST_WEBVIEW_LOGIN)
        }
        
        // Settings
        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun checkLoginState() {
        if (PreferenceManager.isLoggedIn()) {
            showMainContent()
        } else {
            showAuthScreen()
        }
    }
    
    private fun showWarningIfNeeded() {
        if (!PreferenceManager.isWarningAccepted()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.warning_title)
                .setMessage(R.string.warning_message)
                .setPositiveButton(R.string.warning_agree) { _, _ ->
                    PreferenceManager.setWarningAccepted(true)
                }
                .setCancelable(false)
                .show()
        }
    }
    
    private fun loginWithCookie(cookie: String) {
        btnLoginCookie.isEnabled = false
        btnLoginWebView.isEnabled = false
        
        lifecycleScope.launch {
            try {
                // Save cookie temporarily
                PreferenceManager.setAuthCookie(cookie)
                
                // Test login
                val result = VRChatApi.getInstance().getCurrentUser()
                result.onSuccess { user ->
                    val displayName = user["displayName"] as? String ?: "User"
                    Toast.makeText(this@MainActivity, "欢迎, $displayName!", Toast.LENGTH_SHORT).show()
                    showMainContent()
                }.onFailure { error ->
                    PreferenceManager.clearAuth()
                    Toast.makeText(this@MainActivity, "登录失败: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                PreferenceManager.clearAuth()
                Toast.makeText(this@MainActivity, "登录失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnLoginCookie.isEnabled = true
                btnLoginWebView.isEnabled = true
            }
        }
    }
    
    private fun extractAuthCookie(cookie: String): String? {
        // Handle various formats:
        // auth=xxx;
        // auth=xxx
        // Cookie: auth=xxx;
        val cleanCookie = cookie.replace("Cookie:", "").trim()
        
        val regex = "auth=([^;]+)".toRegex()
        val match = regex.find(cleanCookie)
        return match?.groupValues?.get(1)
    }
    
    private fun showAuthScreen() {
        authContainer.visibility = View.VISIBLE
        toolbar.visibility = View.GONE
        tabLayout.visibility = View.GONE
        viewPager.visibility = View.GONE
    }
    
    private fun showMainContent() {
        authContainer.visibility = View.GONE
        toolbar.visibility = View.VISIBLE
        tabLayout.visibility = View.VISIBLE
        viewPager.visibility = View.VISIBLE
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_WEBVIEW_LOGIN && resultCode == RESULT_OK) {
            val cookie = data?.getStringExtra(EXTRA_COOKIE)
            if (cookie != null) {
                loginWithCookie(cookie)
            }
        }
    }
    
    companion object {
        private const val REQUEST_WEBVIEW_LOGIN = 100
        const val EXTRA_COOKIE = "extra_cookie"
    }
}
