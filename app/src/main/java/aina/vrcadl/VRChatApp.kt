package aina.vrcadl

import android.app.Application
import aina.vrcadl.cache.CacheManager
import aina.vrcadl.download.DownloadManager

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
