package aina.vrcadl.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import aina.vrcadl.VRChatApp

object PreferenceManager {
    
    private const val PREFS_NAME = "vrca_prefs"
    private const val KEY_AUTH_COOKIE = "auth_cookie"
    private const val KEY_FILENAME_TEMPLATE = "filename_template"
    private const val KEY_DOWNLOAD_PATH = "download_path"
    private const val KEY_PROXY_ENABLED = "proxy_enabled"
    private const val KEY_PROXY_HOST = "proxy_host"
    private const val KEY_PROXY_PORT = "proxy_port"
    private const val KEY_WARNING_ACCEPTED = "warning_accepted"
    
    private val prefs: SharedPreferences by lazy {
        VRChatApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Auth
    fun setAuthCookie(cookie: String?) {
        prefs.edit { putString(KEY_AUTH_COOKIE, cookie) }
    }
    
    fun getAuthCookie(): String? {
        return prefs.getString(KEY_AUTH_COOKIE, null)
    }
    
    fun isLoggedIn(): Boolean {
        return !getAuthCookie().isNullOrEmpty()
    }
    
    fun clearAuth() {
        prefs.edit { remove(KEY_AUTH_COOKIE) }
    }
    
    // Filename Template
    fun setFilenameTemplate(template: String) {
        prefs.edit { putString(KEY_FILENAME_TEMPLATE, template) }
    }
    
    fun getFilenameTemplate(): String? {
        return prefs.getString(KEY_FILENAME_TEMPLATE, "{short_name}")
    }
    
    // Download Path
    fun setDownloadPath(path: String) {
        prefs.edit { putString(KEY_DOWNLOAD_PATH, path) }
    }
    
    fun getDownloadPath(): String? {
        return prefs.getString(KEY_DOWNLOAD_PATH, null)
    }
    
    // Proxy
    fun setProxyEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_PROXY_ENABLED, enabled) }
    }
    
    fun isProxyEnabled(): Boolean {
        return prefs.getBoolean(KEY_PROXY_ENABLED, false)
    }
    
    fun setProxyHost(host: String) {
        prefs.edit { putString(KEY_PROXY_HOST, host) }
    }
    
    fun getProxyHost(): String? {
        return prefs.getString(KEY_PROXY_HOST, null)
    }
    
    fun setProxyPort(port: Int) {
        prefs.edit { putInt(KEY_PROXY_PORT, port) }
    }
    
    fun getProxyPort(): Int {
        return prefs.getInt(KEY_PROXY_PORT, 0)
    }
    
    // Warning
    fun setWarningAccepted(accepted: Boolean) {
        prefs.edit { putBoolean(KEY_WARNING_ACCEPTED, accepted) }
    }
    
    fun isWarningAccepted(): Boolean {
        return prefs.getBoolean(KEY_WARNING_ACCEPTED, false)
    }
    
    // Clear all
    fun clearAll() {
        prefs.edit { clear() }
    }
}
