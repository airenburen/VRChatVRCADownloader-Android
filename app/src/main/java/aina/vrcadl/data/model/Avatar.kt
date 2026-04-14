package aina.vrcadl.data.model

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
        get() = extractShortAvatarName(name)
    
    fun getFormattedFilename(template: String): String {
        val sanitizedShortName = sanitizeFilename(extractShortAvatarName(name))
        val sanitizedName = sanitizeFilename(name)
        val dateStr = createdAt?.substringBefore("T") ?: "unknown"
        
        return template
            .replace("{short_name}", sanitizedShortName)
            .replace("{name}", sanitizedName)
            .replace("{version}", version.toString())
            .replace("{id}", id)
            .replace("{date}", dateStr)
            .let { sanitizeFilename(it) }
    }
    
    companion object {
        /**
         * 清理文件名中的非法字�?         * 移除: \ / : * ? " < > |
         */
        fun sanitizeFilename(name: String): String {
            val cleaned = name.replace(Regex("[\\\\/:*?\"<>|]+"), "_")
                .trim()
            return if (cleaned.isEmpty()) "Unknown" else cleaned
        }
        
        /**
         * �?VRChat Avatar 导出名称中提取短名称
         * 典型格式: "Avatar - DisplayName - Asset bundle - ..."
         */
        fun extractShortAvatarName(rawName: String): String {
            val text = rawName.trim()
            if (text.isEmpty()) return "Unknown"
            
            // 尝试从标准格式中提取: Avatar - <DisplayName> - Asset bundle -
            val pattern = """^\s*Avatar\s*-\s*(.*?)\s*-\s*Asset\s*bundle\s*-""".toRegex(RegexOption.IGNORE_CASE)
            val match = pattern.find(text)
            
            if (match != null) {
                val shortName = match.groupValues[1].trim()
                return if (shortName.isNotEmpty()) shortName else text
            }
            
            return text
        }
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
