package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun generateText(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Shield AI Error: Gemini API key is not configured in the Secrets panel."
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = systemInstruction?.let { Content(parts = listOf(Part(text = it))) }
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No response from Shield AI."
        } catch (e: Exception) {
            "Shield AI Error: ${e.message}"
        }
    }

    suspend fun summarizeWebpage(title: String, bodyText: String): String {
        val prompt = """
            Summarize the following webpage content in an elegant, structured, and informative bulleted summary.
            Title: $title
            Content:
            ${bodyText.take(6000)}
        """.trimIndent()
        val systemInstruction = "You are an expert reading assistant. Provide key takeaways, highly polished and concise."
        return generateText(prompt, systemInstruction)
    }

    suspend fun askReadingAssistant(pageTitle: String, pageText: String, question: String): String {
        val prompt = """
            The user is asking a question about the webpage they are reading.
            Webpage Title: $pageTitle
            Webpage Content:
            ${pageText.take(5000)}
            
            Question: $question
        """.trimIndent()
        val systemInstruction = "You are a helpful companion reading assistant built into Shield Browser. Answer the user's question accurately based on the webpage text provided."
        return generateText(prompt, systemInstruction)
    }

    suspend fun translatePage(pageText: String, targetLanguage: String): String {
        val prompt = """
            Translate the following webpage content into $targetLanguage. Keep formatting where appropriate.
            Content:
            ${pageText.take(5000)}
        """.trimIndent()
        val systemInstruction = "You are a professional web translator. Translate accurately while preserving a natural and modern tone."
        return generateText(prompt, systemInstruction)
    }

    suspend fun getSmartSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || query.length < 2) {
            return@withContext emptyList()
        }

        val prompt = "Generate 4 brief search suggestions for a web search engine based on this partial query: \"$query\". Return as a comma-separated list, nothing else."
        val systemInstruction = "You are a fast search suggest engine. Output only the suggestions, separated by commas. No numbering, no extra words."
        val result = generateText(prompt, systemInstruction)
        
        if (result.startsWith("Shield AI Error")) {
            emptyList()
        } else {
            result.split(",")
                .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                .filter { it.isNotEmpty() && !it.equals(query, ignoreCase = true) }
        }
    }
}
