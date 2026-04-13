package aina.vrcadl.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import aina.vrcadl.data.model.Avatar
import aina.vrcadl.data.model.FileResponse
import aina.vrcadl.data.model.FileVersion
import aina.vrcadl.utils.PreferenceManager
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
                if (message.contains("429") || message.contains("Too Many Requests")) {
                    if (attempt < MAX_RETRIES) {
                        delay(RETRY_DELAY_MS * attempt)
                    }
                } else {
                    throw e
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
            val n = 50
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
                    delay(REQUEST_DELAY_MS)
                }
            }

            val avatars = allFiles
                .filter { it.extension == ".vrca" && !it.versions.isNullOrEmpty() }
                .mapNotNull { file ->
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

    /**
     * �?FileResponse �?FileVersion 中提取缩略图 URL
     */
    private fun extractThumbnailUrl(file: FileResponse, version: FileVersion): String? {
        // 1. 检�?FileResponse 的图�?URL
        if (!file.imageUrl.isNullOrEmpty() && file.imageUrl.startsWith("http")) {
            return file.imageUrl
        }
        if (!file.thumbnailImageUrl.isNullOrEmpty() && file.thumbnailImageUrl.startsWith("http")) {
            return file.thumbnailImageUrl
        }
        if (!file.iconUrl.isNullOrEmpty() && file.iconUrl.startsWith("http")) {
            return file.iconUrl
        }
        if (!file.previewImageUrl.isNullOrEmpty() && file.previewImageUrl.startsWith("http")) {
            return file.previewImageUrl
        }

        // 2. 检�?FileVersion 的图�?URL (版本对象可能直接包含)
        if (!version.imageUrl.isNullOrEmpty() && version.imageUrl.startsWith("http")) {
            return version.imageUrl
        }
        if (!version.thumbnailImageUrl.isNullOrEmpty() && version.thumbnailImageUrl.startsWith("http")) {
            return version.thumbnailImageUrl
        }
        if (!version.iconUrl.isNullOrEmpty() && version.iconUrl.startsWith("http")) {
            return version.iconUrl
        }
        if (!version.previewImageUrl.isNullOrEmpty() && version.previewImageUrl.startsWith("http")) {
            return version.previewImageUrl
        }

        // 3. 检查版本的 file variants 中的图片
        version.file?.variants?.forEach { variant ->
            if (variant.type == "image" && !variant.url.isNullOrEmpty() && variant.url.startsWith("http")) {
                return variant.url
            }
        }

        return null
    }

    /**
     * 递归在对象中查找第一个有效的图片 URL
     */
    private fun findFirstImageUrl(obj: Any?): String? {
        return when (obj) {
            is Map<*, *> -> {
                val imageFields = listOf(
                    "imageUrl", "imageURL", "thumbnailImageUrl", "thumbnailUrl",
                    "iconUrl", "previewImageUrl", "url"
                )
                for (field in imageFields) {
                    val value = obj[field]
                    if (value is String && value.startsWith("http") &&
                        (value.contains(".png", ignoreCase = true) ||
                         value.contains(".jpg", ignoreCase = true) ||
                         value.contains(".jpeg", ignoreCase = true) ||
                         value.contains(".webp", ignoreCase = true))) {
                        return value
                    }
                }
                for (value in obj.values) {
                    val found = findFirstImageUrl(value)
                    if (found != null) return found
                }
                null
            }
            is List<*> -> {
                for (item in obj) {
                    val found = findFirstImageUrl(item)
                    if (found != null) return found
                }
                null
            }
            else -> null
        }
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
