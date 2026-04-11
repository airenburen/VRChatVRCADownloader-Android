package com.nullk.vrcavrcadownloader.data.model

import com.google.gson.annotations.SerializedName

data class FileResponse(
    val id: String?,
    val name: String?,
    val description: String?,
    val extension: String?,
    @SerializedName("imageUrl")
    val imageUrl: String?,
    @SerializedName("thumbnailImageUrl")
    val thumbnailImageUrl: String?,
    val versions: List<FileVersion>?
)

data class FileVersion(
    val version: Int?,
    @SerializedName("created_at")
    val createdAt: String?,
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
