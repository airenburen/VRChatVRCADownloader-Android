package com.nullk.vrcavrcadownloader.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.nullk.vrcavrcadownloader.R

class WebViewActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnDone: MaterialButton
    
    private var capturedAuthCookie: String? = null
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        
        initViews()
        setupWebView()
        
        // Load VRChat login page
        webView.loadUrl("https://vrchat.com/home/login")
    }
    
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        btnDone = findViewById(R.id.btnDone)
        
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "登录 VRChat"
        
        toolbar.setNavigationOnClickListener {
            finish()
        }
        
        btnDone.setOnClickListener {
            captureAuthCookie()
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
            }
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                
                // Try to capture auth cookie automatically
                captureAuthCookie()
            }
            
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }
        }
    }
    
    private fun captureAuthCookie() {
        val cookieManager = CookieManager.getInstance()
        val cookie = cookieManager.getCookie("https://vrchat.com")
        
        if (cookie != null) {
            // Extract auth cookie
            val authRegex = "auth=([^;]+)".toRegex()
            val match = authRegex.find(cookie)
            
            if (match != null) {
                capturedAuthCookie = match.groupValues[1]
                
                // Return result
                val intent = Intent()
                intent.putExtra(MainActivity.EXTRA_COOKIE, capturedAuthCookie)
                setResult(RESULT_OK, intent)
                finish()
                return
            }
        }
        
        // If we reach here, no auth cookie found
        if (capturedAuthCookie == null) {
            // Show message that user needs to login first
            android.widget.Toast.makeText(
                this,
                "请先完成登录，或点击完成按钮手动确认",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
