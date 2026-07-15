package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.BrowserDatabase
import com.example.data.BrowserRepository
import com.example.ui.BrowserViewModel
import com.example.ui.BrowserViewModelFactory
import com.example.ui.MainBrowserApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Pre-create WebView Code Cache directories to prevent Chromium E/chromium logcat errors
        try {
            val cacheDir = applicationContext.cacheDir
            val jsDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
            if (!jsDir.exists()) {
                jsDir.mkdirs()
            }
            val wasmDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
            if (!wasmDir.exists()) {
                wasmDir.mkdirs()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Initialize Database & Repository
        val database = BrowserDatabase.getDatabase(applicationContext)
        val repository = BrowserRepository(database.browserDao())
        
        // Initialize ViewModel via Factory
        val viewModelFactory = BrowserViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[BrowserViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MainBrowserApp(viewModel = viewModel)
        }
    }
}
