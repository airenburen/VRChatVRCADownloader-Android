package com.nullk.vrcavrcadownloader

import android.app.Application
import com.nullk.vrcavrcadownloader.cache.CacheManager
import com.nullk.vrcavrcadownloader.download.DownloadManager

class VRChatApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize managers
        CacheManager.init(this)
        DownloadManager.init(this)
    }

    companion object {
        lateinit var instance: VRChatApp
            private set
    }
}
