package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Bookmark::class, HistoryItem::class, TabItem::class, ShieldStats::class, QuickShortcut::class, BrowserExtension::class],
    version = 2,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {

    abstract fun browserDao(): BrowserDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        fun getDatabase(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "shield_browser_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initialize default stats and quick shortcuts on database creation
                            val scope = CoroutineScope(Dispatchers.IO)
                            scope.launch {
                                val dao = getDatabase(context).browserDao()
                                dao.insertOrUpdateShieldStats(ShieldStats(id = 1))
                                
                                // Insert popular websites as default shortcuts
                                dao.insertShortcut(QuickShortcut(title = "Brave Search", url = "https://search.brave.com"))
                                dao.insertShortcut(QuickShortcut(title = "Google", url = "https://www.google.com"))
                                dao.insertShortcut(QuickShortcut(title = "YouTube", url = "https://www.youtube.com"))
                                dao.insertShortcut(QuickShortcut(title = "DuckDuckGo", url = "https://duckduckgo.com"))
                                dao.insertShortcut(QuickShortcut(title = "Wikipedia", url = "https://en.wikipedia.org"))
                                dao.insertShortcut(QuickShortcut(title = "GitHub", url = "https://github.com"))

                                // Insert awesome default extensions/userscripts
                                dao.insertExtension(
                                    BrowserExtension(
                                        name = "Dark Reader Mode",
                                        description = "Enforces an elegant dark mode styles on light websites, saving battery and your eyes.",
                                        version = "1.2",
                                        author = "Prosper Lab",
                                        jsCode = """
                                            (function() {
                                                if (document.getElementById('prosper-dark-reader')) return;
                                                var style = document.createElement('style');
                                                style.id = 'prosper-dark-reader';
                                                style.innerHTML = 'html, body { filter: invert(0.9) hue-rotate(180deg) !important; background-color: #121212 !important; } img, video, iframe, canvas, [style*="background-image"] { filter: invert(1) hue-rotate(180deg) !important; }';
                                                document.head.appendChild(style);
                                            })();
                                        """.trimIndent(),
                                        isEnabled = false, // starts disabled, user can enable it
                                        isBuiltIn = true
                                    )
                                )

                                dao.insertExtension(
                                    BrowserExtension(
                                        name = "Video Speed Booster (1.5x)",
                                        description = "Automatically speeds up all HTML5 web videos to 1.5x default speed for quick viewing.",
                                        version = "1.0",
                                        author = "Prosper Speed",
                                        jsCode = """
                                            (function() {
                                                var videos = document.getElementsByTagName('video');
                                                for (var i = 0; i < videos.length; i++) {
                                                    videos[i].playbackRate = 1.5;
                                                }
                                            })();
                                        """.trimIndent(),
                                        isEnabled = false,
                                        isBuiltIn = true
                                    )
                                )

                                dao.insertExtension(
                                    BrowserExtension(
                                        name = "Ad Element Sweeper",
                                        description = "Scans and hides typical sidebars, banner containers, and ad blocks for distraction-free reading.",
                                        version = "1.1",
                                        author = "Shield Security",
                                        jsCode = """
                                            (function() {
                                                var selectors = ['aside', '.sidebar', '#sidebar', '.ads', '.banner', 'div[class*="ad-"]'];
                                                selectors.forEach(function(sel) {
                                                    document.querySelectorAll(sel).forEach(function(el) {
                                                        el.style.display = 'none';
                                                    });
                                                });
                                            })();
                                        """.trimIndent(),
                                        isEnabled = false,
                                        isBuiltIn = true
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
