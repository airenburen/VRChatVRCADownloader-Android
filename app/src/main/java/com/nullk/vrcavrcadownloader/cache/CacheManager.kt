package com.nullk.vrcavrcadownloader.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nullk.vrcavrcadownloader.data.model.Avatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object CacheManager {
    
    private lateinit var appContext: Context
    private lateinit var cacheDir: File
    private lateinit var imageCacheDir: File
    private lateinit var dataCacheDir: File
    private val gson = Gson()
    
    private const val AVATAR_LIST_CACHE = "avatar_list.json"
    private const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000 // 24 hours
    
    fun init(context: Context) {
        appContext = context.applicationContext
        cacheDir = File(appContext.cacheDir, "vrca_cache")
        imageCacheDir = File(cacheDir, "images")
        dataCacheDir = File(cacheDir, "data")
        
        imageCacheDir.mkdirs()
        dataCacheDir.mkdirs()
    }
    
    // Avatar list caching
    suspend fun saveAvatarList(avatars: List<Avatar>) = withContext(Dispatchers.IO) {
        try {
            val file = File(dataCacheDir, AVATAR_LIST_CACHE)
            val json = gson.toJson(avatars)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun loadAvatarList(): List<Avatar>? = withContext(Dispatchers.IO) {
        try {
            val file = File(dataCacheDir, AVATAR_LIST_CACHE)
            if (!file.exists()) return@withContext null
            
            // Check if cache is expired
            if (System.currentTimeMillis() - file.lastModified() > CACHE_EXPIRY_MS) {
                file.delete()
                return@withContext null
            }
            
            val json = file.readText()
            val type = object : TypeToken<List<Avatar>>() {}.type
            gson.fromJson<List<Avatar>>(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // Image caching
    suspend fun cacheImage(url: String, avatarId: String): File? = withContext(Dispatchers.IO) {
        try {
            val file = File(imageCacheDir, "${avatarId}.jpg")
            if (file.exists() && file.length() > 0) {
                return@withContext file
            }
            
            val bitmap = Glide.with(appContext)
                .asBitmap()
                .load(url)
                .submit()
                .get()
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun getCachedImage(avatarId: String): File? {
        val file = File(imageCacheDir, "${avatarId}.jpg")
        return if (file.exists() && file.length() > 0) file else null
    }
    
    // General cache operations
    fun getCacheSize(): Long {
        return getDirSize(cacheDir)
    }
    
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            cacheDir.deleteRecursively()
            imageCacheDir.mkdirs()
            dataCacheDir.mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun getDirSize(dir: File): Long {
        var size: Long = 0
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) getDirSize(file) else file.length()
            }
        }
        return size
    }
    
    fun formatCacheSize(size: Long): String {
        return when {
            size >= 1024 * 1024 * 1024 -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.2f KB", size / 1024.0)
            else -> "$size B"
        }
    }
}
