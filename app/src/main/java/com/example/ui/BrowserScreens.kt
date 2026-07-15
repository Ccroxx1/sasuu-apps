package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.security.AdBlocker
import com.example.security.SecurityCenter
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainBrowserApp(viewModel: BrowserViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isIncognito by viewModel.isIncognitoMode.collectAsState()

    // Determine Theme Colors based on Incognito mode
    val primaryColor = if (isIncognito) Color(0xFF8B5CF6) else Color(0xFFFF5A1F) // Violet vs Brave Orange
    val surfaceColor = if (isIncognito) Color(0xFF1E1B4B) else Color(0xFF0F172A) // Deep Indigo vs Slate Dark
    val onSurfaceColor = Color.White
    
    val colorScheme = if (isIncognito) {
        darkColorScheme(
            primary = primaryColor,
            background = Color(0xFF0F0B21),
            surface = surfaceColor,
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = onSurfaceColor,
            secondary = Color(0xFFA78BFA)
        )
    } else {
        darkColorScheme(
            primary = primaryColor,
            background = Color(0xFF030712),
            surface = surfaceColor,
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = onSurfaceColor,
            secondary = Color(0xFFF97316)
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        Screen.Home -> HomeScreen(viewModel = viewModel)
                        Screen.Browser -> BrowserScreen(viewModel = viewModel)
                        Screen.TabManager -> TabManagerScreen(viewModel = viewModel)
                        Screen.Settings -> SettingsScreen(viewModel = viewModel)
                        Screen.SecurityCenterScreen -> SecurityCenterView(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

// --- JAVASCRIPT INTERFACE FOR HTML CONTENT EXTRACTION ---
class WebTextExtractor(private val onExtracted: (String, String) -> Unit) {
    @JavascriptInterface
    fun processHTML(text: String, title: String) {
        onExtracted(text, title)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(viewModel: BrowserViewModel) {
    val isIncognito by viewModel.isIncognitoMode.collectAsState()
    val stats by viewModel.shieldStats.collectAsState()
    val shortcuts by viewModel.shortcuts.collectAsState()
    val suggestions by viewModel.smartSuggestions.collectAsState()
    val urlInput by viewModel.urlInput.collectAsState()
    val shieldEnabled by viewModel.shieldEnabled.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var showAddShortcutDialog by remember { mutableStateOf(false) }

    // Pulsing animation for the Shield
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val shieldScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (shieldEnabled) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row with Incognito toggle and settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { viewModel.navigateTo(Screen.SecurityCenterScreen) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "Security Status",
                    tint = if (isIncognito) Color(0xFF10B981) else Color(0xFFFF5A1F),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Shield Secure",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.setIncognitoView(!isIncognito) },
                    modifier = Modifier.testTag("incognito_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isIncognito) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Incognito Mode",
                        tint = if (isIncognito) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = { viewModel.navigateTo(Screen.Settings) },
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Shield / Brand Logo Display
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(140.dp)
                .clickable { viewModel.setShieldEnabled(!shieldEnabled) }
        ) {
            // Glow effect
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .rotate(if (shieldEnabled) shieldScale * 15f else 0f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (shieldEnabled) {
                                if (isIncognito) listOf(Color(0xFF8B5CF6).copy(alpha = 0.4f), Color.Transparent)
                                else listOf(Color(0xFFFF5A1F).copy(alpha = 0.4f), Color.Transparent)
                            } else {
                                listOf(Color.Gray.copy(alpha = 0.2f), Color.Transparent)
                            }
                        )
                    )
            )

            // Dynamic Shield icon using custom drawable
            Image(
                bitmap = ImageBitmap.imageResource(id = R.drawable.ic_shield_logo),
                contentDescription = "Shield Logo",
                modifier = Modifier
                    .size(100.dp)
                    .rotate(if (shieldEnabled) (shieldScale - 1f) * 100f else 0f),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "SHIELD",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            letterSpacing = (-0.5).sp,
            color = Color.White
        )

        Text(
            text = "PRIVACY FIRST BROWSER",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            color = Color(0xFFD0BCFF)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Real-time Privacy Stats Dashboard (Sophisticated Dark layout)
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            border = BorderStroke(1.dp, Color(0xFF49454F)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shield Protection",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE6E1E5)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100))
                            .background(Color(0xFF381E72))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (shieldEnabled) "ACTIVE" else "DISABLED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEADDFF)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // 2x2 statistics grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Column 1 (Left)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Stat 1: Ads Blocked
                        Column {
                            val countStr = if (stats.adsBlocked + stats.trackersBlocked > 1000) {
                                String.format("%.1fk", (stats.adsBlocked + stats.trackersBlocked) / 1000.0)
                            } else {
                                "${stats.adsBlocked + stats.trackersBlocked}"
                            }
                            Text(
                                text = countStr,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.White
                            )
                            Text(
                                text = "ADS BLOCKED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF938F99),
                                letterSpacing = 1.sp
                            )
                        }

                        // Stat 3: Time Saved
                        Column {
                            val timeVal = if (stats.timeSavedSeconds > 60) {
                                String.format("%.0fm", stats.timeSavedSeconds / 60.0)
                            } else {
                                "${stats.timeSavedSeconds}s"
                            }
                            Text(
                                text = timeVal,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.White
                            )
                            Text(
                                text = "TIME SAVED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF938F99),
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Column 2 (Right)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Stat 2: Data Saved
                        Column(horizontalAlignment = Alignment.End) {
                            val dataStr = if (stats.bandwidthSavedMb > 1024) {
                                String.format("%.1f GB", stats.bandwidthSavedMb / 1024.0)
                            } else {
                                String.format("%.1f MB", stats.bandwidthSavedMb)
                            }
                            Text(
                                text = dataStr,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.White
                            )
                            Text(
                                text = "DATA SAVED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF938F99),
                                letterSpacing = 1.sp
                            )
                        }

                        // Stat 4: HTTPS Upgraded
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "98.2%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Light,
                                color = Color(0xFFD0BCFF)
                            )
                            Text(
                                text = "HTTPS UPGRADED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF938F99),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // URL / Search Bar
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { viewModel.updateUrlInput(it) },
                placeholder = {
                    Text(
                        text = "Search or type URL...",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (shieldEnabled) Icons.Default.Shield else Icons.Default.ShieldMoon,
                        contentDescription = "Shield State",
                        tint = if (shieldEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                },
                trailingIcon = {
                    if (urlInput.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateUrlInput("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        viewModel.loadUrl(urlInput)
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_bar")
            )
        }

        // Search Suggestions from Gemini AI
        if (suggestions.isNotEmpty() && urlInput.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Suggests",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Shield AI suggestions",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    suggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateUrlInput(suggestion)
                                    viewModel.loadUrl(suggestion)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Suggest",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = suggestion,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick-Access Shortcuts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FAVORITES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            IconButton(
                onClick = { showAddShortcutDialog = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Favorite",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Favorites Grid
        Box(modifier = Modifier.fillMaxWidth()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 4,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                shortcuts.forEach { shortcut ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(72.dp)
                            .combinedClickable(
                                onClick = { viewModel.loadUrl(shortcut.url) },
                                onLongClick = {
                                    viewModel.removeShortcut(shortcut)
                                    Toast
                                        .makeText(context, "Removed shortcut", Toast.LENGTH_SHORT)
                                        .show()
                                }
                              )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF49454F))
                        ) {
                            val initial = shortcut.title.firstOrNull()?.uppercase() ?: "W"
                            Text(
                                text = initial,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = shortcut.title,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color(0xFF938F99),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // AI Assist Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    Toast.makeText(context, "AI Smart Summary is available on any loaded webpage under Menu Options!", Toast.LENGTH_LONG).show()
                }
        ) {
            Row(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF381E72), Color(0xFF4F378B))
                        )
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Icon",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI SMART SUMMARY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Summarize any web page in seconds with Gemini.",
                        fontSize = 10.sp,
                        color = Color(0xFFEADDFF)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Go",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Navigation Buttons at bottom
        Button(
            onClick = { viewModel.navigateTo(Screen.TabManager) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Tab, contentDescription = "Tabs")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "View Open Tabs")
        }
    }

    // Add Shortcut Dialog
    if (showAddShortcutDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddShortcutDialog = false },
            title = { Text("Add Favorite Shortcut") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newUrl,
                        onValueChange = { newUrl = it },
                        label = { Text("URL") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitle.isNotBlank() && newUrl.isNotBlank()) {
                            viewModel.addShortcut(newTitle, newUrl)
                            showAddShortcutDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddShortcutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatBox(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    maxItemsInEachRow: Int = 4,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        maxItemsInEachRow = maxItemsInEachRow
    ) {
        content()
    }
}

// --- MAIN WEBVIEW SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val activeTabId by viewModel.activeTabId.collectAsState()
    val allTabs by viewModel.allTabs.collectAsState()
    val shieldEnabled by viewModel.shieldEnabled.collectAsState()
    val isIncognito by viewModel.isIncognitoMode.collectAsState()
    val urlInput by viewModel.urlInput.collectAsState()
    val desktopMode by viewModel.desktopModeEnabled.collectAsState()
    val blockScripts by viewModel.blockScripts.collectAsState()

    val currentTab = allTabs.firstOrNull { it.id == activeTabId }
    val scope = rememberCoroutineScope()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var pageProgress by remember { mutableIntStateOf(0) }
    var isSecure by remember { mutableStateOf(true) }

    // Dropdowns & Bottom sheets
    var showMenu by remember { mutableStateOf(false) }
    var showShieldPanel by remember { mutableStateOf(false) }
    
    // AI Feature Panel states
    var showAiSummaryPanel by remember { mutableStateOf(false) }
    var showAiChatPanel by remember { mutableStateOf(false) }
    var showTranslationPanel by remember { mutableStateOf(false) }

    // Malware alert state
    var showMaliciousAlertUrl by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Browser Header / Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                Icon(imageVector = Icons.Default.Home, contentDescription = "Home")
            }

            // Secure/Insecure lock symbol, Address box, and reload
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = if (isIncognito) Icons.Default.VisibilityOff else if (isSecure) Icons.Default.Lock else Icons.Default.Warning,
                        contentDescription = "Security Status",
                        tint = if (isIncognito) Color(0xFF8B5CF6) else if (isSecure) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = urlInput.replace("https://", "").replace("http://", ""),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.navigateTo(Screen.Home) }
                    )
                    IconButton(
                        onClick = { webViewInstance?.reload() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Brave Shield Icon on Web bar
            IconButton(
                onClick = { showShieldPanel = true },
                modifier = Modifier.testTag("shield_bar_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Shield Active Panel",
                    tint = if (shieldEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Horizontal Page Load Progress Bar
        if (pageProgress in 1..99) {
            LinearProgressIndicator(
                progress = pageProgress / 100f,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }

        // Main Web View Window Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White)
        ) {
            if (currentTab != null && currentTab.url != "about:blank") {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            // Optimize Settings
                            settings.apply {
                                javaScriptEnabled = !blockScripts
                                domStorageEnabled = true
                                databaseEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                allowFileAccess = true
                                mediaPlaybackRequiresUserGesture = false
                            }

                            // Support cookies blocking
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, !shieldEnabled)

                            webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val reqUrl = request?.url?.toString() ?: ""
                                    
                                    // AdBlocker interception
                                    if (shieldEnabled && AdBlocker.isAdOrTracker(reqUrl)) {
                                        if (reqUrl.contains("analytics") || reqUrl.contains("tracker")) {
                                            viewModel.reportTrackerBlocked()
                                        } else {
                                            viewModel.reportAdBlocked()
                                        }
                                        return AdBlocker.createEmptyResponse()
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val loadingUrl = request?.url?.toString() ?: ""
                                    
                                    // Phishing & Malicious Alert check
                                    if (SecurityCenter.isMaliciousUrl(loadingUrl)) {
                                        showMaliciousAlertUrl = loadingUrl
                                        return true // Cancel loading
                                    }
                                    return false
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    pageProgress = 0
                                    url?.let {
                                        isSecure = it.startsWith("https://")
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    pageProgress = 100
                                    url?.let {
                                        viewModel.onPageLoaded(it, view?.title ?: "Shield Web")
                                        
                                        // Extract page content for AI reading assistant
                                        view?.evaluateJavascript(
                                            "(function() { return document.body.innerText; })();"
                                        ) { text ->
                                            val cleanText = text?.removeSurrounding("\"")?.replace("\\n", "\n") ?: ""
                                            viewModel.setPageText(cleanText)
                                        }
                                    }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    pageProgress = newProgress
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    val currentUrl = view?.url ?: ""
                                    if (currentUrl.isNotEmpty() && currentUrl != "about:blank") {
                                        viewModel.onPageLoaded(currentUrl, title ?: "Shield Web")
                                    }
                                }
                            }

                            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                                viewModel.triggerDownload(url)
                                Toast.makeText(ctx, "Download Intercepted by Shield Safeguard!", Toast.LENGTH_SHORT).show()
                            }

                            webViewInstance = this
                            
                            // Set initial load
                            loadUrl(currentTab.url)
                        }
                    },
                    update = { view ->
                        // Dynamically apply desktop mode setting
                        val desktopUserAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        val defaultUserAgent = WebSettings.getDefaultUserAgent(view.context)
                        
                        view.settings.userAgentString = if (desktopMode) desktopUserAgent else defaultUserAgent
                        view.settings.javaScriptEnabled = !blockScripts
                        
                        // Navigate WebView if URL changed outside
                        if (currentTab != null && view.url != currentTab.url) {
                            view.loadUrl(currentTab.url)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Empty page", color = Color.Gray)
                }
            }

            // High-Impact Malware Warning Alert Overlay
            showMaliciousAlertUrl?.let { maliciousUrl ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.GppBad,
                                contentDescription = "Threat Detected",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "DANGEROUS WEBSITE BLOCKED",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Shield Protection classified the site \"$maliciousUrl\" as suspicious (Phishing, scam or malicious malware risk).",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    showMaliciousAlertUrl = null
                                    viewModel.navigateTo(Screen.Home)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Return to Safety")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    showMaliciousAlertUrl = null
                                    webViewInstance?.loadUrl(maliciousUrl)
                                }
                            ) {
                                Text("Proceed Anyway (Unsafe)", color = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation Command Bar
        HorizontalDivider(color = Color(0xFF49454F), thickness = 1.dp)
        BottomAppBar(
            containerColor = Color(0xFF211F26),
            modifier = Modifier.height(64.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { webViewInstance?.goBack() },
                    enabled = webViewInstance?.canGoBack() == true
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFE6E1E5))
                }

                IconButton(
                    onClick = { webViewInstance?.goForward() },
                    enabled = webViewInstance?.canGoForward() == true
                ) {
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Forward", tint = Color(0xFFE6E1E5))
                }

                IconButton(
                    onClick = {
                        val currentUrl = webViewInstance?.url ?: ""
                        val currentTitle = webViewInstance?.title ?: "Page"
                        viewModel.toggleBookmark(currentUrl, currentTitle)
                        Toast.makeText(webViewInstance?.context, "Bookmark Saved", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = "Bookmark", tint = Color(0xFFE6E1E5))
                }

                // Tab Badge showing count of tabs (styled as active pill)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFEADDFF))
                        .clickable { viewModel.navigateTo(Screen.TabManager) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("tab_switcher_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tab,
                            contentDescription = "Tabs",
                            tint = Color(0xFF21005D),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${allTabs.filter { it.isIncognito == isIncognito }.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D)
                        )
                    }
                }

                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.testTag("menu_button")
                ) {
                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFFE6E1E5))
                }
            }
        }
    }

    // Interactive Shield Panel BottomSheet-style Dialog
    if (showShieldPanel) {
        AlertDialog(
            onDismissRequest = { showShieldPanel = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Protection",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shield Shields Protection")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Customize protections for ${urlInput.replace("https://", "").replace("http://", "").substringBefore("/")}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Shield (Ad-Blocker)")
                        Switch(
                            checked = shieldEnabled,
                            onCheckedChange = { viewModel.setShieldEnabled(it) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Block JavaScript Scripts")
                        Switch(
                            checked = blockScripts,
                            onCheckedChange = { viewModel.setBlockScripts(it) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShieldPanel = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Standard Browser Option Drawer Menu
    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = { Text("Menu Options") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMenu = false
                                viewModel.setDesktopMode(!desktopMode)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Computer, contentDescription = "Desktop Site")
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(if (desktopMode) "Switch to Mobile Site" else "Request Desktop Site")
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMenu = false
                                showAiSummaryPanel = true
                                viewModel.triggerAiSummary()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Summary", tint = Color(0xFFFBBF24))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Shield AI Summarize Page")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMenu = false
                                showAiChatPanel = true
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Forum, contentDescription = "AI Chat", tint = Color(0xFF3B82F6))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("AI Reading Assistant")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMenu = false
                                showTranslationPanel = true
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Translate, contentDescription = "Translate", tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Translate Page Content")
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMenu = false
                                viewModel.navigateTo(Screen.Settings)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Settings")
                    }
                }
            },
            confirmButton = {}
        )
    }

    // --- AI DIALOG OVERLAYS ---
    if (showAiSummaryPanel) {
        val aiLoading by viewModel.aiLoading.collectAsState()
        val summary by viewModel.aiSummary.collectAsState()

        AlertDialog(
            onDismissRequest = {
                showAiSummaryPanel = false
                viewModel.clearAiSummary()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color(0xFFFBBF24))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shield AI Webpage Summary")
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (aiLoading) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Shield AI is reading the page...")
                        }
                    } else {
                        Text(
                            text = summary ?: "No webpage summary available.",
                            fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAiSummaryPanel = false
                        viewModel.clearAiSummary()
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (showTranslationPanel) {
        val aiLoading by viewModel.aiLoading.collectAsState()
        val translation by viewModel.aiTranslation.collectAsState()
        var selectedLang by remember { mutableStateOf("Spanish") }

        AlertDialog(
            onDismissRequest = {
                showTranslationPanel = false
                viewModel.clearAiTranslation()
            },
            title = { Text("Shield AI Web Translation") },
            text = {
                Column {
                    if (translation == null) {
                        Text("Select Target Language:")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            listOf("Spanish", "French", "German", "Japanese", "Chinese").forEach { lang ->
                                FilterChip(
                                    selected = selectedLang == lang,
                                    onClick = { selectedLang = lang },
                                    label = { Text(lang) },
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.translateCurrentPage(selectedLang) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Translate with Shield AI")
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (aiLoading) {
                                CircularProgressIndicator()
                            } else {
                                Text(translation ?: "")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTranslationPanel = false
                        viewModel.clearAiTranslation()
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }

    if (showAiChatPanel) {
        val aiLoading by viewModel.aiLoading.collectAsState()
        val chatMessages by viewModel.aiReadingChat.collectAsState()
        var questionInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAiChatPanel = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Forum, contentDescription = "AI", tint = Color(0xFF3B82F6))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Reading Assistant")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(8.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chatMessages) { msg ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (msg.sender == "user") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = if (msg.sender == "user") "You" else "Shield Assistant",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = msg.message, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        if (aiLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = questionInput,
                        onValueChange = { questionInput = it },
                        placeholder = { Text("Ask about this webpage...") },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    viewModel.sendReadingAssistantQuestion(questionInput)
                                    questionInput = ""
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAiChatPanel = false
                        viewModel.clearReadingAssistantChat()
                    }
                ) {
                    Text("Clear & Close")
                }
            }
        )
    }
}

// --- DUAL TAB MANAGER SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabManagerScreen(viewModel: BrowserViewModel) {
    val allTabs by viewModel.allTabs.collectAsState()
    val isIncognito by viewModel.isIncognitoMode.collectAsState()
    val privateLocked by viewModel.privateTabsLocked.collectAsState()
    
    val displayTabs = allTabs.filter { it.isIncognito == isIncognito }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tabs Manager") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.createNewTab() },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("new_tab_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Tab")
            }
        },
        bottomBar = {
            // Mode selector (Standard vs Incognito Tab view)
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = !isIncognito,
                    onClick = { viewModel.setIncognitoView(false) },
                    icon = { Icon(imageVector = Icons.Default.Public, contentDescription = "Standard Tabs") },
                    label = { Text("Standard") },
                    modifier = Modifier.testTag("standard_tabs_mode")
                )
                NavigationBarItem(
                    selected = isIncognito,
                    onClick = { viewModel.setIncognitoView(true) },
                    icon = { Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = "Private Tabs") },
                    label = { Text("Private") },
                    modifier = Modifier.testTag("private_tabs_mode")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isIncognito && privateLocked) {
                // Biometric Lock Screen Simulation
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Private Tabs Locked",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Biometric lock or face authentication is enabled to guard incognito sessions.",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.unlockPrivateTabs() },
                        modifier = Modifier.testTag("biometric_unlock_button")
                    ) {
                        Text("Unlock Private Tabs")
                    }
                }
            } else {
                if (displayTabs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No active tabs in this mode. Tap + to open.", color = Color.Gray)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayTabs) { tab ->
                            Card(
                                border = if (tab.isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clickable { viewModel.switchTab(tab.id) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tab.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.closeTab(tab.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close Tab",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = tab.url.replace("https://", "").replace("http://", ""),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (tab.isIncognito) Color(0xFF5B21B6) else Color(0xFFC2410C)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (tab.isIncognito) "Private" else "Standard",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SETTINGS CONFIGURATION SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: BrowserViewModel) {
    val blockAds by viewModel.blockAds.collectAsState()
    val blockTrackers by viewModel.blockTrackers.collectAsState()
    val forceHttps by viewModel.forceHttps.collectAsState()
    val blockThirdPartyCookies by viewModel.blockThirdPartyCookies.collectAsState()
    val blockScripts by viewModel.blockScripts.collectAsState()
    val searchEngine by viewModel.searchEngine.collectAsState()
    val biometricLock by viewModel.biometricLockEnabled.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shield Core Settings") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "SEARCH SETTING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                SearchEngineSelector(
                    selectedEngine = searchEngine,
                    onEngineSelected = { viewModel.setSearchEngine(it) }
                )
            }

            item { HorizontalDivider() }

            item {
                Text(
                    text = "PRIVACY & AD-BLOCKING SHIELDS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                SwitchSettingRow(
                    title = "Block Ads / Popups",
                    subtitle = "Blocks invasive commercials, interstitial banners, and redirects.",
                    checked = blockAds,
                    onCheckedChange = { viewModel.setBlockAds(it) }
                )
                Spacer(modifier = Modifier.height(12.dp))

                SwitchSettingRow(
                    title = "Strict Tracker Prevention",
                    subtitle = "Stops cookies, fingerprinting tracking, and invisible telemetry beacons.",
                    checked = blockTrackers,
                    onCheckedChange = { viewModel.setBlockTrackers(it) }
                )
                Spacer(modifier = Modifier.height(12.dp))

                SwitchSettingRow(
                    title = "Force HTTPS Upgrades",
                    subtitle = "Automatically secures connections before sending sensitive request packets.",
                    checked = forceHttps,
                    onCheckedChange = { viewModel.setForceHttps(it) }
                )
                Spacer(modifier = Modifier.height(12.dp))

                SwitchSettingRow(
                    title = "Block Third-Party Cookies",
                    subtitle = "Restricts advertisement companies from building cross-site track records.",
                    checked = blockThirdPartyCookies,
                    onCheckedChange = { viewModel.setBlockThirdPartyCookies(it) }
                )
                Spacer(modifier = Modifier.height(12.dp))

                SwitchSettingRow(
                    title = "Disable JavaScript (Block Scripts)",
                    subtitle = "For maximum safety, blocks dynamic scripts which might carry browser fingerprinting exploits.",
                    checked = blockScripts,
                    onCheckedChange = { viewModel.setBlockScripts(it) }
                )
            }

            item { HorizontalDivider() }

            item {
                Text(
                    text = "SECURITY LOCKS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                SwitchSettingRow(
                    title = "Lock Private Tabs",
                    subtitle = "Lock private (incognito) browsing sessions with Android Biometrics.",
                    checked = biometricLock,
                    onCheckedChange = { viewModel.setBiometricLock(it) }
                )
            }

            item { HorizontalDivider() }

            item {
                Text(
                    text = "CLEAR DATA & PRIVACY DUMP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.clearBrowsingData()
                        Toast.makeText(context, "Successfully Cleared Cache, History & Cookies!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wipe and Reset All Browser Data")
                }
            }
        }
    }
}

@Composable
fun SearchEngineSelector(
    selectedEngine: SearchEngine,
    onEngineSelected: (SearchEngine) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            SearchEngine.values().forEach { engine ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEngineSelected(engine) }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = engine.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = engine.searchUrl.substringBefore("="),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                    RadioButton(
                        selected = selectedEngine == engine,
                        onClick = { onEngineSelected(engine) }
                    )
                }
                if (engine != SearchEngine.BING) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun SwitchSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f),
                lineHeight = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// --- SECURITY CENTER AND DOWNLOADS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCenterView(viewModel: BrowserViewModel) {
    val downloads by viewModel.downloads.collectAsState()
    val shieldEnabled by viewModel.shieldEnabled.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shield Security Center") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shield Guard",
                            tint = if (shieldEnabled) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (shieldEnabled) "YOUR CONNECTION IS PROTECTED" else "PROTECTION SUSPENDED",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (shieldEnabled) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Shield Browser uses a unique integrated sandboxed ad and tracker blocker alongside Google's safe web API to isolate and block threats.",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "DOWNLOADS MANAGER & SAFE-GUARD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }

            if (downloads.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No files downloaded yet.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(downloads) { download ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (download.isSuspicious) Color(0xFF451A03) else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = download.fileName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = download.url,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (download.isSuspicious) {
                                    Icon(
                                        imageVector = Icons.Default.ReportProblem,
                                        contentDescription = "Threat Warning",
                                        tint = Color(0xFFF97316)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (download.isSuspicious) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF78350F).copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Warning",
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Suspicious executable file isolated to prevent malware execution.",
                                            fontSize = 10.sp,
                                            color = Color(0xFFFBBF24)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            LinearProgressIndicator(
                                progress = download.progress / 100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (download.isComplete) "Complete" else "Downloading: ${download.progress}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format("%.2f MB", download.sizeBytes / (1024.0 * 1024.0)),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
