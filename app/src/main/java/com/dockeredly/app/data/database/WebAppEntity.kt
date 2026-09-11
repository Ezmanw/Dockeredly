package com.dockeredly.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "web_apps")
data class WebAppEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val description: String,
    val engine: String,
    val iconType: String,
    val iconSourceUrl: String?,
    val iconLocalPath: String?,
    val sortOrder: Int,
    val createdAt: Long,
    val lastUsedAt: Long?,
    val profileId: String,
)
