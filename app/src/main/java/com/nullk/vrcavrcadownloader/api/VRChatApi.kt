package com.nullk.vrcavrcadownloader.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nullk.vrcavrcadownloader.data.model.Avatar
import com.nullk.vrcavrcadownloader.data.model.AvatarResponse
import com.nullk.vrcavrcadownloader.data.model.AvatarsResponse
import com.nullk.vrcavrcadownloader.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class VRChatApi private constructor() {

    private val gson: Gson = GsonBuilder().create()
    private var client: OkHttpClient = createClient()
    
    companion object {
        private const val BASE_URL = "https://api.vrchat.cloud/api/1"
        private const val USER_AGENT = "VRChatVRCADownloader-Android/1.0"
        
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
            .readTimeout(60, TimeUnit.SECONDS)
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
        
        // Add logging interceptor for debug (disabled in release)
        // Logging disabled for production builds
        
        // Configure proxy if enabled
        if (!proxyHost.isNullOrEmpty() && proxyPort > 0) {
            builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort)))
        }
        
        return builder.build()
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
    
    suspend fun getAvatars(n: Int = 100, offset: Int = 0): Result<List<Avatar>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/avatars?n=$n&offset=$offset&releaseStatus=all&sort=updated&order=descending")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val avatarResponses = gson.fromJson(body, Array<AvatarResponse>::class.java)?.toList()
                        val avatars = avatarResponses?.map { it.toAvatar() } ?: emptyList()
                        Result.success(avatars)
                    } else {
                        Result.success(emptyList())
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAvatarById(avatarId: String): Result<Avatar> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/avatars/$avatarId")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val avatarResponse = gson.fromJson(body, AvatarResponse::class.java)
                        Result.success(avatarResponse.toAvatar())
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
    
    private fun AvatarResponse.toAvatar(): Avatar {
        return Avatar(
            id = this.id,
            name = this.name,
            description = this.description,
            version = this.version ?: 1,
            createdAt = this.created_at,
            updatedAt = this.updated_at,
            thumbnailUrl = this.thumbnailImageUrl,
            assetUrl = this.assetUrl,
            authorId = this.authorId,
            authorName = this.authorName,
            releaseStatus = this.releaseStatus
        )
    }
}
