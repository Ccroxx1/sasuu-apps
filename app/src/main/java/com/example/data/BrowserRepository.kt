package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BrowserRepository(private val browserDao: BrowserDao) {

    // --- Bookmarks ---
    val allBookmarks: Flow<List<Bookmark>> = browserDao.getAllBookmarks()

    suspend fun addBookmark(bookmark: Bookmark) = withContext(Dispatchers.IO) {
        browserDao.insertBookmark(bookmark)
    }

    suspend fun removeBookmark(bookmark: Bookmark) = withContext(Dispatchers.IO) {
        browserDao.deleteBookmark(bookmark)
    }

    suspend fun removeBookmarkByUrl(url: String) = withContext(Dispatchers.IO) {
        browserDao.deleteBookmarkByUrl(url)
    }

    suspend fun isBookmarked(url: String): Boolean = withContext(Dispatchers.IO) {
        browserDao.isBookmarked(url)
    }


    // --- History ---
    val allHistory: Flow<List<HistoryItem>> = browserDao.getAllHistory()

    suspend fun addHistoryItem(historyItem: HistoryItem) = withContext(Dispatchers.IO) {
        browserDao.insertHistory(historyItem)
    }

    suspend fun removeHistoryItem(id: Int) = withContext(Dispatchers.IO) {
        browserDao.deleteHistoryItem(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        browserDao.clearHistory()
    }


    // --- Tabs ---
    val allTabs: Flow<List<TabItem>> = browserDao.getAllTabs()

    suspend fun addTab(tabItem: TabItem) = withContext(Dispatchers.IO) {
        browserDao.insertTab(tabItem)
    }

    suspend fun updateTab(tabItem: TabItem) = withContext(Dispatchers.IO) {
        browserDao.updateTab(tabItem)
    }

    suspend fun removeTab(tabItem: TabItem) = withContext(Dispatchers.IO) {
        browserDao.deleteTab(tabItem)
    }

    suspend fun removeTabById(id: String) = withContext(Dispatchers.IO) {
        browserDao.deleteTabById(id)
    }

    suspend fun clearAllTabs() = withContext(Dispatchers.IO) {
        browserDao.clearAllTabs()
    }


    // --- Shield Stats ---
    val shieldStats: Flow<ShieldStats?> = browserDao.getShieldStats()

    suspend fun incrementShieldStats(ads: Int, trackers: Int, bytes: Long, ms: Long) = withContext(Dispatchers.IO) {
        // Ensure stats exist first
        val current = browserDao.getShieldStatsOnce()
        if (current == null) {
            browserDao.insertOrUpdateShieldStats(ShieldStats(id = 1))
        }
        browserDao.incrementShieldStats(ads, trackers, bytes, ms)
    }

    suspend fun resetShieldStats() = withContext(Dispatchers.IO) {
        browserDao.insertOrUpdateShieldStats(ShieldStats(id = 1, adsBlocked = 0, trackersBlocked = 0, bandwidthSavedBytes = 0L, timeSavedMs = 0L))
    }


    // --- Quick Shortcuts ---
    val allShortcuts: Flow<List<QuickShortcut>> = browserDao.getAllShortcuts()

    suspend fun addShortcut(shortcut: QuickShortcut) = withContext(Dispatchers.IO) {
        browserDao.insertShortcut(shortcut)
    }

    suspend fun removeShortcut(shortcut: QuickShortcut) = withContext(Dispatchers.IO) {
        browserDao.deleteShortcut(shortcut)
    }


    // --- Browser Extensions ---
    val allExtensions: Flow<List<BrowserExtension>> = browserDao.getAllExtensions()

    suspend fun addExtension(extension: BrowserExtension) = withContext(Dispatchers.IO) {
        browserDao.insertExtension(extension)
    }

    suspend fun updateExtension(extension: BrowserExtension) = withContext(Dispatchers.IO) {
        browserDao.updateExtension(extension)
    }

    suspend fun removeExtension(extension: BrowserExtension) = withContext(Dispatchers.IO) {
        browserDao.deleteExtension(extension)
    }
}
