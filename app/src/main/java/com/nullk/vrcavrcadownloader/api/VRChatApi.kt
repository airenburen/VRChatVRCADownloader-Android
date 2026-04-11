package com.nullk.vrcavrcadownloader.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nullk.vrcavrcadownloader.data.model.Avatar
import com.nullk.vrcavrcadownloader.data.model.FileResponse
import com.nullk.vrcavrcadownloader.data.model.FileVersion
import com.nullk.vrcavrcadownloader.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class VRChatApi private constructor() {

    private val gson: Gson = GsonBuilder().create()
    private var client: OkHttpClient = createClient()

    companion object {
        private const val BASE_URL = "https://api.vrchat.cloud/api/1"
        private const val FILES_URL = "$BASE_URL/files"
        private const val USER_AGENT = "VRChatVRCADownloader-Android/1.0"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L
        private const val REQUEST_DELAY_MS = 100L

        @Volatile
        private var instance: VRChatApi? = null

        fun getInstance(): VRChatApi {
            return instance ?: synchronized(this) {
                instance ?: VRChatApi().also { instance = it }
            }
        }
    }

    fun updateProxy(host: String?, port: Int) {
        client = createClient(host, port)
    }

    private fun createClient(proxyHost: String? = null, proxyPort: Int = 0): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .apply {
                        PreferenceManager.getAuthCookie()?.let { cookie ->
                            header("Cookie", "auth=$cookie")
                        }
                    }
                    .build()
                chain.proceed(request)
            }

        if (!proxyHost.isNullOrEmpty() && proxyPort > 0) {
            builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort)))
        }

        return builder.build()
    }

    private suspend fun <T> withRetry(block: suspend () -> T): T {
        var lastException: Exception? = null
        for (attempt in 1..MAX_RETRIES) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                val message = e.message ?: ""
                // Check if it's a rate limit error (429)
                if (message.contains("429") || message.contains("Too Many Requests")) {
                    if (attempt < MAX_RETRIES) {
                        delay(RETRY_DELAY_MS * attempt) // Exponential backoff
                    }
                } else {
                    throw e // Not a rate limit error, throw immediately
                }
            }
        }
        throw lastException ?: Exception("Max retries exceeded")
    }

    suspend fun getCurrentUser(): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/auth/user")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val user = gson.fromJson(body, Map::class.java) as Map<String, Any>
                        Result.success(user)
                    } else {
                        Result.failure(Exception("Empty response"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvatarFiles(): Result<List<Avatar>> = withContext(Dispatchers.IO) {
        try {
            val allFiles = mutableListOf<FileResponse>()
            var offset = 0
            val n = 50 // Reduced batch size to avoid rate limiting
            var hasMore = true

            while (hasMore) {
                val files = withRetry {
                    val request = Request.Builder()
                        .url("$FILES_URL?n=$n&offset=$offset")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw Exception("HTTP ${response.code}")
                        }

                        val body = response.body?.string()
                        if (body.isNullOrEmpty()) {
                            emptyList()
                        } else {
                            gson.fromJson(body, Array<FileResponse>::class.java)?.toList() ?: emptyList()
                        }
                    }
                }

                if (files.isEmpty()) {
                    hasMore = false
                } else {
                    allFiles.addAll(files)
                    offset += n
                    // Add delay between requests to avoid rate limiting
                    delay(REQUEST_DELAY_MS)
                }
            }

            // Filter .vrca files and convert to Avatar
            val avatars = allFiles
                .filter { it.extension == ".vrca" && !it.versions.isNullOrEmpty() }
                .mapNotNull { file ->
                    // Get latest version
                    val latestVersion = file.versions?.maxByOrNull { it.version ?: 0 }
                        ?: return@mapNotNull null

                    val fileUrl = latestVersion.file?.url
                        ?: return@mapNotNull null

                    Avatar(
                        id = file.id ?: return@mapNotNull null,
                        name = file.name ?: "Unknown",
                        description = file.description,
                        version = latestVersion.version ?: 1,
                        createdAt = latestVersion.createdAt,
                        updatedAt = latestVersion.createdAt,
                        thumbnailUrl = extractThumbnailUrl(file, latestVersion),
                        assetUrl = fileUrl,
                        authorId = null,
                        authorName = null,
                        releaseStatus = null
                    )
                }
                .sortedByDescending { it.createdAt }

            Result.success(avatars)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractThumbnailUrl(file: FileResponse, version: FileVersion): String? {
        // Try to get from file's imageUrl first
        file.imageUrl?.let { return it }

        // Try thumbnailImageUrl
        file.thumbnailImageUrl?.let { return it }

        // Try to get from version's file metadata
        version.file?.variants?.forEach { variant ->
            if (variant.type == "image") {
                return variant.url
            }
        }

        return null
    }

    suspend fun testProxy(host: String, port: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val testClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)))
                .build()

            val request = Request.Builder()
                .url("https://api.vrchat.cloud")
                .head()
                .build()

            testClient.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful || response.code == 404)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
