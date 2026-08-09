package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

@JsonClass(generateAdapter = true)
data class VocabDetail(
    @Json(name = "word") val word: String,
    @Json(name = "translation") val translation: String,
    @Json(name = "definition") val definition: String = "",
    @Json(name = "phonetic") val phonetic: String = "",
    @Json(name = "partOfSpeech") val partOfSpeech: String = "",
    @Json(name = "source") val source: String = "GEMINI" // "PUBLIC_DICT" or "GEMINI"
)

interface GeminiApi {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST
    suspend fun generateContentProxy(
        @retrofit2.http.Url url: String,
        @retrofit2.http.HeaderMap headers: Map<String, String>,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object ProxyGatewayConfig {
    const val isEnabled = true
    const val proxyBaseUrl = "https://vocab-news-api.q0977271216-b8f.workers.dev/"
    const val customHeaderKey = "X-Api-Proxy-Auth"
    const val customHeaderValue = ""
}

object GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }
}

object VocabTranslationHelper {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val vocabAdapter = moshi.adapter(VocabDetail::class.java)

    // In-memory cache for ultra-fast zero-latency repeat lookup within same session
    private val inMemoryCache = java.util.concurrent.ConcurrentHashMap<String, VocabDetail>()

    /**
     * Query Firestore public dictionary first to avoid calling Gemini API.
     */
    suspend fun getFromPublicDictionary(db: FirebaseFirestore?, word: String): VocabDetail? {
        val cleanWord = word.trim().lowercase().replace(Regex("[^a-zA-Z-]"), "")
        if (cleanWord.isEmpty()) return null

        // Level 1: In-Memory L1 Cache
        val memoryHit = inMemoryCache[cleanWord]
        if (memoryHit != null) {
            Log.d("VocabTranslation", "In-memory L1 cache HIT for '$cleanWord'!")
            return memoryHit
        }

        if (db == null) return null

        return try {
            val snapshot = suspendCancellableCoroutine<DocumentSnapshot?> { cont ->
                db.collection("public_dictionary").document(cleanWord).get()
                    .addOnSuccessListener { doc -> cont.resume(doc) }
                    .addOnFailureListener { cont.resume(null) }
            }

            if (snapshot != null && snapshot.exists()) {
                val trans = snapshot.getString("translation") ?: ""
                if (trans.isNotBlank()) {
                    val matchedWord = snapshot.getString("word") ?: cleanWord
                    val def = snapshot.getString("definition") ?: ""
                    val phonetic = snapshot.getString("phonetic") ?: ""
                    val pos = snapshot.getString("partOfSpeech") ?: ""
                    Log.d("VocabTranslation", "Public dictionary HIT for '$cleanWord'!")
                    val detail = VocabDetail(
                        word = matchedWord,
                        translation = trans,
                        definition = def,
                        phonetic = phonetic,
                        partOfSpeech = pos,
                        source = "PUBLIC_DICT"
                    )
                    inMemoryCache[cleanWord] = detail
                    detail
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("VocabTranslation", "Error querying public_dictionary", e)
            null
        }
    }

    /**
     * Save a new translation to Firestore public_dictionary so future queries across users hit cache.
     */
    fun saveToPublicDictionary(db: FirebaseFirestore?, detail: VocabDetail) {
        val cleanWord = detail.word.trim().lowercase().replace(Regex("[^a-zA-Z-]"), "")
        if (cleanWord.isEmpty()) return
        if (detail.translation.isBlank() || 
            detail.translation.contains("連線失敗") || 
            detail.translation.contains("金鑰") || 
            detail.translation.contains("錯誤")
        ) return

        inMemoryCache[cleanWord] = detail

        if (db == null) return

        try {
            val data = hashMapOf(
                "word" to detail.word.ifBlank { cleanWord },
                "translation" to detail.translation,
                "definition" to detail.definition,
                "phonetic" to detail.phonetic,
                "partOfSpeech" to detail.partOfSpeech
            )
            db.collection("public_dictionary").document(cleanWord)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("VocabTranslation", "Successfully cached '$cleanWord' into public_dictionary")
                }
                .addOnFailureListener { e ->
                    Log.e("VocabTranslation", "Failed to cache '$cleanWord' into public_dictionary", e)
                }
        } catch (e: Exception) {
            Log.e("VocabTranslation", "Exception saving to public_dictionary", e)
        }
    }

    /**
     * Entry point that checks Firestore public_dictionary FIRST, then falls back to Gemini API.
     */
    suspend fun translateWordWithPublicDict(
        word: String,
        contextSentence: String,
        db: FirebaseFirestore?
    ): VocabDetail? {
        // Step 1: Check Firestore Public Dictionary
        val publicDictMatch = getFromPublicDictionary(db, word)
        if (publicDictMatch != null) {
            return publicDictMatch
        }

        // Step 2: Fallback to Gemini AI translation
        val geminiDetail = translateWord(word, contextSentence)
        if (geminiDetail != null && 
            !geminiDetail.translation.contains("連線失敗") && 
            !geminiDetail.translation.contains("金鑰")
        ) {
            // Upload result to Firestore public dictionary
            saveToPublicDictionary(db, geminiDetail)
        }
        return geminiDetail
    }

    suspend fun translateWord(word: String, contextSentence: String): VocabDetail? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

        if (!ProxyGatewayConfig.isEnabled && !hasKey) {
            Log.e("VocabTranslation", "Gemini API Key is not set or placeholder!")
            return VocabDetail(
                word = word,
                translation = "AI 翻譯服務未設定 🔒",
                definition = "無法翻譯字詞。系統未偵測到有效的翻譯服務連線設定，請聯絡開發人員或系統管理員。",
                phonetic = ""
            )
        }

        val cleanedWord = word.trim()
            .replace(Regex("[^a-zA-Z-\\s]"), "")
            .replace(Regex("\\s+"), " ")
        if (cleanedWord.isEmpty()) return null

        val prompt = """
            Analyze the English word or phrase "$cleanedWord" in the context of: "$contextSentence".
            Return a brief JSON response in Traditional Chinese (Taiwan).
            
            JSON schema:
            {
              "word": "$cleanedWord",
              "translation": "brief traditional chinese translation/meaning",
              "definition": "",
              "phonetic": "phonetic symbol (KK phonetic / IPA)",
              "partOfSpeech": "noun, verb, adjective, phrase, etc."
            }
            
            Strict requirements:
            1. Keep the "definition" field EXACTLY empty (just ""). Do not generate detailed definitions or examples.
            2. Just return raw JSON, no code blocks or conversational text.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        // Exponential backoff retry loop (max 3 attempts for 429/503)
        var maxAttempts = 3
        var currentAttempt = 0
        var lastException: Exception? = null

        while (currentAttempt < maxAttempts) {
            currentAttempt++
            try {
                val response = if (ProxyGatewayConfig.isEnabled) {
                    try {
                        val url = if (ProxyGatewayConfig.proxyBaseUrl.endsWith("/")) {
                            ProxyGatewayConfig.proxyBaseUrl + "v1beta/models/gemini-2.5-flash:generateContent"
                        } else {
                            ProxyGatewayConfig.proxyBaseUrl + "/v1beta/models/gemini-2.5-flash:generateContent"
                        }
                        
                        val headers = mutableMapOf<String, String>()
                        if (ProxyGatewayConfig.customHeaderKey.isNotBlank() && ProxyGatewayConfig.customHeaderValue.isNotBlank()) {
                            headers[ProxyGatewayConfig.customHeaderKey] = ProxyGatewayConfig.customHeaderValue
                        }
                        
                        Log.d("VocabTranslation", "Calling proxy gateway (Attempt $currentAttempt) url: $url")
                        GeminiClient.api.generateContentProxy(url, headers, request)
                    } catch (proxyEx: Exception) {
                        if (hasKey) {
                            Log.w("VocabTranslation", "Proxy failed, falling back to direct Gemini API with key", proxyEx)
                            GeminiClient.api.generateContent(apiKey, request)
                        } else {
                            throw proxyEx
                        }
                    }
                } else {
                    GeminiClient.api.generateContent(apiKey, request)
                }

                val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (jsonText != null) {
                    Log.d("VocabTranslation", "Received JSON: $jsonText")
                    var cleanedJson = jsonText.trim()
                    if (cleanedJson.startsWith("```json")) {
                        cleanedJson = cleanedJson.removePrefix("```json")
                    } else if (cleanedJson.startsWith("```")) {
                        cleanedJson = cleanedJson.removePrefix("```")
                    }
                    if (cleanedJson.endsWith("```")) {
                        cleanedJson = cleanedJson.removeSuffix("```")
                    }
                    cleanedJson = cleanedJson.trim()
                    return vocabAdapter.fromJson(cleanedJson)
                } else {
                    return null
                }
            } catch (e: retrofit2.HttpException) {
                lastException = e
                val code = e.code()
                Log.e("VocabTranslation", "HTTP Error during translation attempt $currentAttempt: $code", e)
                if ((code == 429 || code == 503) && currentAttempt < maxAttempts) {
                    val backoffTimeMs = (1000L * (1 shl (currentAttempt - 1))) + (kotlin.random.Random.nextLong(100, 500))
                    Log.d("VocabTranslation", "Rate limited ($code), retrying in ${backoffTimeMs}ms...")
                    kotlinx.coroutines.delay(backoffTimeMs)
                } else {
                    if (code == 429) {
                        return VocabDetail(
                            word = cleanedWord,
                            translation = "金鑰額度已達上限 (HTTP 429) ⚠️",
                            definition = "Gemini API 回傳了 Too Many Requests (429) 錯誤。這表示該 API 的呼叫頻率或額度已達到每分鐘/每日的上限，請稍候幾分鐘再試。",
                            phonetic = ""
                        )
                    } else {
                        return VocabDetail(
                            word = cleanedWord,
                            translation = "伺服器回傳錯誤 ($code) ❌",
                            definition = "Gemini API 呼叫失敗，狀態碼為 $code。這可能是伺服器連線問題，請稍後重試或聯絡開發人員。",
                            phonetic = ""
                        )
                    }
                }
            } catch (e: Exception) {
                lastException = e
                Log.e("VocabTranslation", "Error translating word attempt $currentAttempt", e)
                if (currentAttempt < maxAttempts) {
                    val backoffTimeMs = (500L * currentAttempt) + (kotlin.random.Random.nextLong(100, 300))
                    kotlinx.coroutines.delay(backoffTimeMs)
                } else {
                    return VocabDetail(
                        word = cleanedWord,
                        translation = "連線失敗 📡",
                        definition = "發生異常連線錯誤，請檢查您的網路連線。\n詳細日誌: ${e.localizedMessage ?: e.toString()}",
                        phonetic = ""
                    )
                }
            }
        }

        return VocabDetail(
            word = cleanedWord,
            translation = "連線失敗 📡",
            definition = "超過重試次數，請稍後重試。\n詳細日誌: ${lastException?.localizedMessage ?: lastException?.toString()}",
            phonetic = ""
        )
    }
}
