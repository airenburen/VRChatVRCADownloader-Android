package com.nullk.vrcavrcadownloader.data.model

data class FileResponse(
    val id: String?,
    val name: String?,
    val description: String?,
    val extension: String?,
    val imageUrl: String?,
    val versions: List<FileVersion>?
)

data class FileVersion(
    val version: Int?,
    val created_at: String?,
    val file: FileDetails?
)

data class FileDetails(
    val url: String?,
    val variants: List<FileVariant>?
)

data class FileVariant(
    val type: String?,
    val url: String?
)
