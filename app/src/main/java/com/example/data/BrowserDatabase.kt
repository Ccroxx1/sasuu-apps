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
    entities = [Bookmark::class, HistoryItem::class, TabItem::class, ShieldStats::class, QuickShortcut::class],
    version = 1,
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
