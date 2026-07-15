package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserDao {

    // --- Bookmarks ---
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url LIMIT 1)")
    suspend fun isBookmarked(url: String): Boolean


    // --- History ---
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(historyItem: HistoryItem)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Int)

    @Query("DELETE FROM history")
    suspend fun clearHistory()


    // --- Tabs ---
    @Query("SELECT * FROM tabs")
    fun getAllTabs(): Flow<List<TabItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tabItem: TabItem)

    @Update
    suspend fun updateTab(tabItem: TabItem)

    @Delete
    suspend fun deleteTab(tabItem: TabItem)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun deleteTabById(id: String)

    @Query("DELETE FROM tabs")
    suspend fun clearAllTabs()


    // --- Shield Stats ---
    @Query("SELECT * FROM shield_stats WHERE id = 1")
    fun getShieldStats(): Flow<ShieldStats?>

    @Query("SELECT * FROM shield_stats WHERE id = 1")
    suspend fun getShieldStatsOnce(): ShieldStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateShieldStats(shieldStats: ShieldStats)

    @Query("UPDATE shield_stats SET adsBlocked = adsBlocked + :ads, trackersBlocked = trackersBlocked + :trackers, bandwidthSavedBytes = bandwidthSavedBytes + :bytes, timeSavedMs = timeSavedMs + :ms WHERE id = 1")
    suspend fun incrementShieldStats(ads: Int, trackers: Int, bytes: Long, ms: Long)


    // --- Quick Shortcuts ---
    @Query("SELECT * FROM quick_shortcuts")
    fun getAllShortcuts(): Flow<List<QuickShortcut>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: QuickShortcut)

    @Delete
    suspend fun deleteShortcut(shortcut: QuickShortcut)
}
