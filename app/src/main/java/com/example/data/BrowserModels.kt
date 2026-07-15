package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tabs")
data class TabItem(
    @PrimaryKey val id: String, // UUID
    val title: String,
    val url: String,
    val isIncognito: Boolean = false,
    val isActive: Boolean = false,
    val tabGroupId: String? = null
)

@Entity(tableName = "shield_stats")
data class ShieldStats(
    @PrimaryKey val id: Int = 1,
    val adsBlocked: Int = 0,
    val trackersBlocked: Int = 0,
    val bandwidthSavedBytes: Long = 0L, // KB/MB estimation
    val timeSavedMs: Long = 0L // Milliseconds estimation
) {
    val bandwidthSavedMb: Double
        get() = bandwidthSavedBytes / (1024.0 * 1024.0)

    val timeSavedSeconds: Double
        get() = timeSavedMs / 1000.0
}

@Entity(tableName = "quick_shortcuts")
data class QuickShortcut(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val iconUrl: String? = null
)

@Entity(tableName = "browser_extensions")
data class BrowserExtension(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val version: String = "1.0",
    val author: String = "User",
    val jsCode: String,
    val isEnabled: Boolean = true,
    val isBuiltIn: Boolean = false
)

