package com.example.security

import android.net.Uri
import java.io.ByteArrayInputStream
import java.net.URL
import android.webkit.WebResourceResponse

object AdBlocker {

    // Simple robust list of common ad, tracker and analytics host components
    private val BLOCK_KEYWORDS = listOf(
        "doubleclick.net",
        "google-analytics.com",
        "googlesyndication.com",
        "googletagservices.com",
        "googleads",
        "pagead",
        "adservice",
        "adsystem",
        "scorecardresearch.com",
        "adnxs.com",
        "quantserve.com",
        "popads.net",
        "adcolony.com",
        "chartbeat.com",
        "mixpanel.com",
        "amplitude.com",
        "hotjar.com",
        "facebook.net/tr",
        "facebook.com/tr",
        "amazon-adsystem",
        "optimizely.com",
        "adroll.com",
        "criteo.com",
        "outbrain.com",
        "taboola.com",
        "disqus.com/forums",
        "ad-delivery",
        "analytics-delivery",
        "trafficmanager.net",
        "adserver",
        "trackers",
        "telemetry"
    )

    fun isAdOrTracker(url: String): Boolean {
        if (url.isEmpty()) return false
        val lowercaseUrl = url.lowercase()
        
        // Don't block search engines or critical browser actions
        if (lowercaseUrl.contains("search.brave.com") || lowercaseUrl.contains("google.com/search")) {
            return false
        }

        return BLOCK_KEYWORDS.any { keyword -> lowercaseUrl.contains(keyword) }
    }

    fun upgradeToHttps(url: String): String {
        if (url.startsWith("http://")) {
            // Only upgrade typical web page protocols, ignore local hosts or other special schemes
            val remainder = url.substring(7)
            if (!remainder.startsWith("localhost") && !remainder.startsWith("192.168.") && !remainder.startsWith("10.")) {
                return "https://$remainder"
            }
        }
        return url
    }

    /**
     * Creates an empty dummy response to cancel blocked requests
     */
    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream("".toByteArray())
        )
    }
}
