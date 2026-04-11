package com.nullk.vrcavrcadownloader.download

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nullk.vrcavrcadownloader.data.model.Avatar
import com.nullk.vrcavrcadownloader.data.model.DownloadStatus
import com.nullk.vrcavrcadownloader.data.model.DownloadTask
import com.nullk.vrcavrcadownloader.utils.PreferenceManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.coroutineContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object DownloadManager {
    
    private lateinit var appContext: Context
    private val client: OkHttpClient by lazy { createClient() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks
    
    private val activeJobs = ConcurrentHashMap<String, Job>()
    
    fun init(context: Context) {
        appContext = context.applicationContext
    }
    
    private fun createClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
        
        // Configure proxy if enabled
        if (PreferenceManager.isProxyEnabled()) {
            val host = PreferenceManager.getProxyHost()
            val port = PreferenceManager.getProxyPort()
            if (!host.isNullOrEmpty() && port > 0) {
                builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)))
            }
        }
        
        return builder.build()
    }
    
    fun addDownload(avatar: Avatar): DownloadTask {
        val existingTask = _tasks.value.find { it.avatar.id == avatar.id }
        if (existingTask != null) {
            if (existingTask.status == DownloadStatus.FAILED || 
                existingTask.status == DownloadStatus.CANCELLED) {
                retryDownload(existingTask.id)
            }
            return existingTask
        }
        
        val task = DownloadTask(avatar = avatar)
        _tasks.value = _tasks.value + task
        startDownload(task)
        return task
    }
    
    fun addDownloads(avatars: List<Avatar>): List<DownloadTask> {
        return avatars.map { addDownload(it) }
    }
    
    private fun startDownload(task: DownloadTask) {
        val job = scope.launch {
            downloadFile(task)
        }
        activeJobs[task.id] = job
    }
    
    private suspend fun downloadFile(task: DownloadTask) {
        val avatar = task.avatar
        val assetUrl = avatar.assetUrl ?: run {
            updateTask(task.copy(status = DownloadStatus.FAILED, errorMessage = "No asset URL"))
            return
        }
        
        val downloadDir = PreferenceManager.getDownloadPath() 
            ?: appContext.getExternalFilesDir(null)?.absolutePath 
            ?: appContext.filesDir.absolutePath
        
        val filename = avatar.getFormattedFilename(
            PreferenceManager.getFilenameTemplate() ?: "{short_name}"
        ) + ".vrca"
        
        val outputFile = File(downloadDir, filename)
        
        updateTask(task.copy(status = DownloadStatus.DOWNLOADING))
        
        try {
            val request = Request.Builder()
                .url(assetUrl)
                .header("User-Agent", "VRChatVRCADownloader-Android/1.0")
                .apply {
                    PreferenceManager.getAuthCookie()?.let { cookie ->
                        header("Cookie", "auth=$cookie")
                    }
                }
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    updateTask(task.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = "HTTP ${response.code}"
                    ))
                    return
                }
                
                val body = response.body ?: run {
                    updateTask(task.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = "Empty response body"
                    ))
                    return
                }
                
                val totalBytes = body.contentLength()
                updateTask(task.copy(totalBytes = totalBytes))
                
                var inputStream: InputStream? = null
                var outputStream: FileOutputStream? = null
                
                try {
                    inputStream = body.byteStream()
                    outputStream = FileOutputStream(outputFile)
                    
                    val buffer = ByteArray(8192)
                    var downloadedBytes = 0L
                    var lastUpdateTime = System.currentTimeMillis()
                    var lastDownloadedBytes = 0L
                    
                    while (coroutineContext.isActive) {
                        val read = inputStream.read(buffer)
                        if (read == -1) break
                        
                        outputStream.write(buffer, 0, read)
                        downloadedBytes += read
                        
                        val currentTime = System.currentTimeMillis()
                        val timeDiff = currentTime - lastUpdateTime
                        
                        if (timeDiff >= 500) { // Update every 500ms
                            val speed = ((downloadedBytes - lastDownloadedBytes) * 1000) / timeDiff
                            val progress = if (totalBytes > 0) {
                                ((downloadedBytes * 100) / totalBytes).toInt()
                            } else 0
                            
                            updateTask(task.copy(
                                downloadedBytes = downloadedBytes,
                                progress = progress,
                                speed = speed
                            ))
                            
                            lastUpdateTime = currentTime
                            lastDownloadedBytes = downloadedBytes
                        }
                    }
                    
                    outputStream.flush()
                    
                    if (coroutineContext.isActive) {
                        updateTask(task.copy(
                            status = DownloadStatus.COMPLETED,
                            progress = 100,
                            downloadedBytes = downloadedBytes,
                            localPath = outputFile.absolutePath,
                            speed = 0
                        ))
                    } else {
                        outputFile.delete()
                        updateTask(task.copy(
                            status = DownloadStatus.CANCELLED,
                            speed = 0
                        ))
                    }
                    
                } catch (e: IOException) {
                    outputFile.delete()
                    updateTask(task.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = e.message ?: "IO Error",
                        speed = 0
                    ))
                } finally {
                    inputStream?.close()
                    outputStream?.close()
                }
            }
        } catch (e: Exception) {
            outputFile.delete()
            updateTask(task.copy(
                status = DownloadStatus.FAILED,
                errorMessage = e.message ?: "Unknown error",
                speed = 0
            ))
        }
        
        activeJobs.remove(task.id)
    }
    
    fun cancelDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        
        _tasks.value.find { it.id == taskId }?.let { task ->
            if (task.status == DownloadStatus.DOWNLOADING) {
                updateTask(task.copy(status = DownloadStatus.CANCELLED, speed = 0))
            }
        }
    }
    
    fun retryDownload(taskId: String) {
        _tasks.value.find { it.id == taskId }?.let { task ->
            val newTask = task.copy(
                status = DownloadStatus.PENDING,
                progress = 0,
                downloadedBytes = 0,
                speed = 0,
                errorMessage = null
            )
            updateTask(newTask)
            startDownload(newTask)
        }
    }
    
    fun retryAllFailed() {
        _tasks.value.filter { 
            it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED 
        }.forEach { retryDownload(it.id) }
    }
    
    fun removeTask(taskId: String) {
        cancelDownload(taskId)
        _tasks.value = _tasks.value.filter { it.id != taskId }
    }
    
    fun clearCompleted() {
        _tasks.value = _tasks.value.filter { 
            it.status != DownloadStatus.COMPLETED 
        }
    }
    
    private fun updateTask(updatedTask: DownloadTask) {
        _tasks.value = _tasks.value.map { 
            if (it.id == updatedTask.id) updatedTask else it 
        }
    }
    
    fun getTaskCount(): Int = _tasks.value.size
    
    fun getActiveDownloadCount(): Int = 
        _tasks.value.count { it.status == DownloadStatus.DOWNLOADING }
    
    fun hasActiveDownloads(): Boolean = getActiveDownloadCount() > 0
}
