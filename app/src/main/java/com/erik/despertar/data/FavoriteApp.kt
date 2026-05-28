package com.erik.despertar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteApp(
    @PrimaryKey val packageName: String,
    val addedAt: Long = System.currentTimeMillis()
)
