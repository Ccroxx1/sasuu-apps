package com.example.ui

import android.app.Application
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiClient
import com.example.data.*
import com.example.security.AdBlocker
import com.example.security.SecurityCenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class SearchEngine(val title: String, val searchUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    BRAVE("Brave Search", "https://search.brave.com/search?q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    BING("Bing", "https://www.bing.com/search?q=")
}

data class DownloadItem(
    val id: String,
    val fileName: String,
    val url: String,
    val progress: Int, // 0 to 100
    val isComplete: Boolean,
    val sizeBytes: Long,
    val isSuspicious: Boolean = false
)

data class ChatMessage(
    val sender: String, // "user" or "ai"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface Screen {
    object Home : Screen
    object Browser : Screen
    object TabManager : Screen
    object Settings : Screen
    object SecurityCenterScreen : Screen
}

class BrowserViewModel(
    application: Application,
    private val repository: BrowserRepository
) : AndroidViewModel(application) {

    // --- UI Navigation ---
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // --- Tab Management ---
    val allTabs: StateFlow<List<TabItem>> = repository.allTabs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val _isIncognitoMode = MutableStateFlow(false)
    val isIncognitoMode: StateFlow<Boolean> = _isIncognitoMode.asStateFlow()

    // Current URL input in the bar
    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    fun updateUrlInput(input: String) {
        _urlInput.value = input
        fetchSmartSearchSuggestions(input)
    }

    // --- Bookmarks, History, Shortcuts ---
    val bookmarks: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortcuts: StateFlow<List<QuickShortcut>> = repository.allShortcuts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shieldStats: StateFlow<ShieldStats> = repository.shieldStats
        .map { it ?: ShieldStats() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShieldStats())

    // --- Shield Settings ---
    private val _shieldEnabled = MutableStateFlow(true)
    val shieldEnabled: StateFlow<Boolean> = _shieldEnabled.asStateFlow()

    private val _blockAds = MutableStateFlow(true)
    val blockAds: StateFlow<Boolean> = _blockAds.asStateFlow()

    private val _blockTrackers = MutableStateFlow(true)
    val blockTrackers: StateFlow<Boolean> = _blockTrackers.asStateFlow()

    private val _forceHttps = MutableStateFlow(true)
    val forceHttps: StateFlow<Boolean> = _forceHttps.asStateFlow()

    private val _blockThirdPartyCookies = MutableStateFlow(true)
    val blockThirdPartyCookies: StateFlow<Boolean> = _blockThirdPartyCookies.asStateFlow()

    private val _blockScripts = MutableStateFlow(false)
    val blockScripts: StateFlow<Boolean> = _blockScripts.asStateFlow()

    private val _dataSaverMode = MutableStateFlow(false)
    val dataSaverMode: StateFlow<Boolean> = _dataSaverMode.asStateFlow()

    private val _searchEngine = MutableStateFlow(SearchEngine.BRAVE)
    val searchEngine: StateFlow<SearchEngine> = _searchEngine.asStateFlow()

    // --- Biometric Lock ---
    private val _biometricLockEnabled = MutableStateFlow(false)
    val biometricLockEnabled: StateFlow<Boolean> = _biometricLockEnabled.asStateFlow()

    private val _privateTabsLocked = MutableStateFlow(false)
    val privateTabsLocked: StateFlow<Boolean> = _privateTabsLocked.asStateFlow()

    // --- AI Feature States ---
    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiSummary = MutableStateFlow<String?>(null)
    val aiSummary: StateFlow<String?> = _aiSummary.asStateFlow()

    private val _aiTranslation = MutableStateFlow<String?>(null)
    val aiTranslation: StateFlow<String?> = _aiTranslation.asStateFlow()

    private val _aiReadingChat = MutableStateFlow<List<ChatMessage>>(emptyList())
    val aiReadingChat: StateFlow<List<ChatMessage>> = _aiReadingChat.asStateFlow()

    private val _smartSuggestions = MutableStateFlow<List<String>>(emptyList())
    val smartSuggestions: StateFlow<List<String>> = _smartSuggestions.asStateFlow()

    // --- Downloads Manager ---
    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    // --- Active Page States (Current Web Page content extracted) ---
    private val _currentPageText = MutableStateFlow("")
    val currentPageText: StateFlow<String> = _currentPageText.asStateFlow()

    private val _currentPageTitle = MutableStateFlow("")
    val currentPageTitle: StateFlow<String> = _currentPageTitle.asStateFlow()

    private val _desktopModeEnabled = MutableStateFlow(false)
    val desktopModeEnabled: StateFlow<Boolean> = _desktopModeEnabled.asStateFlow()

    private val _readerModeEnabled = MutableStateFlow(false)
    val readerModeEnabled: StateFlow<Boolean> = _readerModeEnabled.asStateFlow()

    init {
        // Automatically open a default tab if no tabs exist
        viewModelScope.launch {
            repository.allTabs.first().let { currentTabs ->
                if (currentTabs.isEmpty()) {
                    createNewTab("Home", "about:blank")
                } else {
                    val active = currentTabs.firstOrNull { it.isActive } ?: currentTabs.first()
                    _activeTabId.value = active.id
                    _isIncognitoMode.value = active.isIncognito
                    if (active.url != "about:blank") {
                        _currentScreen.value = Screen.Browser
                        _urlInput.value = active.url
                    }
                }
            }
        }
    }

    // --- Custom Web Resource Block Tracker ---
    fun reportAdBlocked() {
        if (!shieldEnabled.value) return
        viewModelScope.launch {
            // Estimate saving 80KB and 200ms per blocked tracker/ad
            repository.incrementShieldStats(1, 0, 81920L, 200L)
        }
    }

    fun reportTrackerBlocked() {
        if (!shieldEnabled.value) return
        viewModelScope.launch {
            // Estimate saving 50KB and 150ms per blocked tracker/ad
            repository.incrementShieldStats(0, 1, 51200L, 150L)
        }
    }

    // --- Tab Control Methods ---
    fun createNewTab(title: String = "Home", url: String = "about:blank") {
        viewModelScope.launch {
            val uuid = UUID.randomUUID().toString()
            val newTab = TabItem(
                id = uuid,
                title = title,
                url = url,
                isIncognito = _isIncognitoMode.value,
                isActive = true
            )
            
            // Mark all other tabs as inactive
            allTabs.value.filter { it.isIncognito == _isIncognitoMode.value }.forEach {
                repository.updateTab(it.copy(isActive = false))
            }

            repository.addTab(newTab)
            _activeTabId.value = uuid
            _urlInput.value = if (url == "about:blank") "" else url
            
            if (url == "about:blank") {
                _currentScreen.value = Screen.Home
            } else {
                _currentScreen.value = Screen.Browser
            }
        }
    }

    fun switchTab(tabId: String) {
        viewModelScope.launch {
            val tab = allTabs.value.firstOrNull { it.id == tabId } ?: return@launch
            
            // Set all same mode tabs to inactive, set this one to active
            allTabs.value.forEach {
                if (it.id == tabId) {
                    repository.updateTab(it.copy(isActive = true))
                } else if (it.isActive && it.isIncognito == tab.isIncognito) {
                    repository.updateTab(it.copy(isActive = false))
                }
            }

            _activeTabId.value = tabId
            _isIncognitoMode.value = tab.isIncognito
            _urlInput.value = if (tab.url == "about:blank") "" else tab.url
            
            if (tab.url == "about:blank") {
                _currentScreen.value = Screen.Home
            } else {
                _currentScreen.value = Screen.Browser
            }
        }
    }

    fun closeTab(tabId: String) {
        viewModelScope.launch {
            repository.removeTabById(tabId)
            
            // If we closed the active tab, switch to another tab
            if (_activeTabId.value == tabId) {
                val remaining = allTabs.value.filter { it.id != tabId && it.isIncognito == _isIncognitoMode.value }
                if (remaining.isNotEmpty()) {
                    switchTab(remaining.first().id)
                } else {
                    createNewTab("Home", "about:blank")
                }
            }
        }
    }

    fun setIncognitoView(isIncognito: Boolean) {
        if (isIncognito && biometricLockEnabled.value) {
            _privateTabsLocked.value = true
        }
        _isIncognitoMode.value = isIncognito
        
        // Find or create active tab in this mode
        viewModelScope.launch {
            val sameModeTabs = allTabs.value.filter { it.isIncognito == isIncognito }
            if (sameModeTabs.isEmpty()) {
                createNewTab("Home", "about:blank")
            } else {
                val active = sameModeTabs.firstOrNull { it.isActive } ?: sameModeTabs.first()
                switchTab(active.id)
            }
        }
    }

    fun unlockPrivateTabs() {
        _privateTabsLocked.value = false
    }

    // --- Navigation & Search ---
    fun loadUrl(input: String) {
        if (input.isBlank()) return
        
        var targetUrl = input.trim()
        
        // Custom URL or Search Engine lookup
        val isUrl = targetUrl.contains(".") && !targetUrl.contains(" ") && !targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")
        if (isUrl) {
            targetUrl = "https://$targetUrl"
        } else if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
            targetUrl = "${searchEngine.value.searchUrl}${Uri.encode(targetUrl)}"
        }

        // Apply HTTPS upgrade if requested
        if (forceHttps.value && targetUrl.startsWith("http://")) {
            targetUrl = AdBlocker.upgradeToHttps(targetUrl)
        }

        _urlInput.value = targetUrl
        _currentScreen.value = Screen.Browser

        // Update active tab URL
        viewModelScope.launch {
            val activeId = _activeTabId.value
            if (activeId != null) {
                val activeTab = allTabs.value.firstOrNull { it.id == activeId }
                if (activeTab != null) {
                    repository.updateTab(activeTab.copy(url = targetUrl))
                }
            }
        }
    }

    fun onPageLoaded(url: String, title: String) {
        _urlInput.value = url
        _currentPageTitle.value = title
        
        // Save to History (if not incognito)
        if (!isIncognitoMode.value && url != "about:blank" && url.isNotEmpty()) {
            viewModelScope.launch {
                repository.addHistoryItem(HistoryItem(title = title, url = url))
                
                // Update active tab model with the correct title and url
                val activeId = _activeTabId.value
                if (activeId != null) {
                    val activeTab = allTabs.value.firstOrNull { it.id == activeId }
                    if (activeTab != null) {
                        repository.updateTab(activeTab.copy(title = title, url = url))
                    }
                }
            }
        }
    }

    fun setPageText(text: String) {
        _currentPageText.value = text
    }

    // --- Bookmarks & History ---
    fun toggleBookmark(url: String, title: String) {
        viewModelScope.launch {
            if (repository.isBookmarked(url)) {
                repository.removeBookmarkByUrl(url)
            } else {
                repository.addBookmark(Bookmark(title = title, url = url))
            }
        }
    }

    fun clearBrowsingData() {
        viewModelScope.launch {
            repository.clearHistory()
            repository.clearAllTabs()
            repository.resetShieldStats()
            
            // Clear WebView cookies & storage
            withContext(Dispatchers.Main) {
                CookieManager.getInstance().removeAllCookies(null)
                WebStorage.getInstance().deleteAllData()
            }
            
            // Re-initialize default tab
            createNewTab("Home", "about:blank")
        }
    }

    // --- Settings and Shields ---
    fun setShieldEnabled(enabled: Boolean) { _shieldEnabled.value = enabled }
    fun setBlockAds(enabled: Boolean) { _blockAds.value = enabled }
    fun setBlockTrackers(enabled: Boolean) { _blockTrackers.value = enabled }
    fun setForceHttps(enabled: Boolean) { _forceHttps.value = enabled }
    fun setBlockThirdPartyCookies(enabled: Boolean) { _blockThirdPartyCookies.value = enabled }
    fun setBlockScripts(enabled: Boolean) { _blockScripts.value = enabled }
    fun setDataSaver(enabled: Boolean) { _dataSaverMode.value = enabled }
    fun setSearchEngine(engine: SearchEngine) { _searchEngine.value = engine }
    fun setBiometricLock(enabled: Boolean) { _biometricLockEnabled.value = enabled }
    fun setDesktopMode(enabled: Boolean) { _desktopModeEnabled.value = enabled }
    fun setReaderMode(enabled: Boolean) { _readerModeEnabled.value = enabled }

    // --- AI Smart Features ---
    fun fetchSmartSearchSuggestions(query: String) {
        viewModelScope.launch {
            if (query.trim().length > 1) {
                _smartSuggestions.value = GeminiClient.getSmartSearchSuggestions(query)
            } else {
                _smartSuggestions.value = emptyList()
            }
        }
    }

    fun triggerAiSummary() {
        val title = _currentPageTitle.value
        val text = _currentPageText.value
        if (text.isBlank()) {
            _aiSummary.value = "Shield AI: No readable web content detected on this page to summarize."
            return
        }

        viewModelScope.launch {
            _aiLoading.value = true
            _aiSummary.value = GeminiClient.summarizeWebpage(title, text)
            _aiLoading.value = false
        }
    }

    fun clearAiSummary() {
        _aiSummary.value = null
    }

    fun translateCurrentPage(targetLanguage: String) {
        val text = _currentPageText.value
        if (text.isBlank()) {
            _aiTranslation.value = "Shield AI: No readable web content detected to translate."
            return
        }

        viewModelScope.launch {
            _aiLoading.value = true
            _aiTranslation.value = GeminiClient.translatePage(text, targetLanguage)
            _aiLoading.value = false
        }
    }

    fun clearAiTranslation() {
        _aiTranslation.value = null
    }

    fun sendReadingAssistantQuestion(question: String) {
        if (question.isBlank()) return
        
        val title = _currentPageTitle.value
        val text = _currentPageText.value
        
        val userMsg = ChatMessage(sender = "user", message = question)
        _aiReadingChat.value = _aiReadingChat.value + userMsg

        viewModelScope.launch {
            _aiLoading.value = true
            val aiResponseText = GeminiClient.askReadingAssistant(title, text, question)
            val aiMsg = ChatMessage(sender = "ai", message = aiResponseText)
            _aiReadingChat.value = _aiReadingChat.value + aiMsg
            _aiLoading.value = false
        }
    }

    fun clearReadingAssistantChat() {
        _aiReadingChat.value = emptyList()
    }

    // --- Quick Shortcuts ---
    fun addShortcut(title: String, url: String) {
        viewModelScope.launch {
            repository.addShortcut(QuickShortcut(title = title, url = url))
        }
    }

    fun removeShortcut(shortcut: QuickShortcut) {
        viewModelScope.launch {
            repository.removeShortcut(shortcut)
        }
    }

    // --- Downloads Simulator & Suspicious Detection ---
    fun triggerDownload(url: String) {
        val fileName = url.substringAfterLast("/").substringBefore("?").ifBlank { "downloaded_file" }
        val size = (100_000..5_000_000).random().toLong()
        val isSuspicious = SecurityCenter.isSuspiciousDownload(fileName)
        
        val newItem = DownloadItem(
            id = UUID.randomUUID().toString(),
            fileName = fileName,
            url = url,
            progress = 0,
            isComplete = false,
            sizeBytes = size,
            isSuspicious = isSuspicious
        )

        _downloads.value = listOf(newItem) + _downloads.value

        // Simulate progress download in a fast coroutine
        viewModelScope.launch {
            var progress = 0
            while (progress < 100) {
                kotlinx.coroutines.delay(200)
                progress += (10..30).random()
                if (progress > 100) progress = 100
                
                _downloads.value = _downloads.value.map {
                    if (it.id == newItem.id) it.copy(progress = progress, isComplete = progress == 100) else it
                }
            }
        }
    }
}

class BrowserViewModelFactory(
    private val application: Application,
    private val repository: BrowserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BrowserViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
