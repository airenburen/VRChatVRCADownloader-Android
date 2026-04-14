package aina.vrcadl.data.model

import java.util.UUID

data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val avatar: Avatar,
    var status: DownloadStatus = DownloadStatus.PENDING,
    var progress: Int = 0,
    var downloadedBytes: Long = 0,
    var totalBytes: Long = 0,
    var speed: Long = 0,
    var errorMessage: String? = null,
    var localPath: String? = null
) {
    val progressText: String
        get() = "$progress%"
    
    val sizeText: String
        get() = "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}"
    
    val speedText: String
        get() = "${formatBytes(speed)}/s"
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}
