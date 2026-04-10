package com.nullk.vrcavrcadownloader.data.model

data class Avatar(
    val id: String,
    val name: String,
    val description: String? = null,
    val version: Int = 1,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val thumbnailUrl: String? = null,
    val assetUrl: String? = null,
    val authorId: String? = null,
    val authorName: String? = null,
    val releaseStatus: String? = null,
    var isSelected: Boolean = false
) {
    val shortName: String
        get() = name.take(20).let { if (name.length > 20) "$it..." else it }
    
    fun getFormattedFilename(template: String): String {
        return template
            .replace("{short_name}", shortName.replace(" ", "_"))
            .replace("{name}", name.replace(" ", "_"))
            .replace("{version}", version.toString())
            .replace("{id}", id)
            .replace("{date}", updatedAt?.substringBefore("T") ?: "unknown")
    }
}

data class AvatarResponse(
    val id: String,
    val name: String,
    val description: String?,
    val version: Int?,
    val created_at: String?,
    val updated_at: String?,
    val thumbnailImageUrl: String?,
    val assetUrl: String?,
    val authorId: String?,
    val authorName: String?,
    val releaseStatus: String?
)

data class AvatarsResponse(
    val avatars: List<AvatarResponse>?
)
