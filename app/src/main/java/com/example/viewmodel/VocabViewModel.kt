package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VocabDatabase
import com.example.data.VocabRepository
import com.example.data.VocabWord
import com.example.data.User
import com.example.data.Assignment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.example.network.VocabDetail
import com.example.network.VocabTranslationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

sealed class TranslationState {
    object Idle : TranslationState()
    object Loading : TranslationState()
    data class Success(val detail: VocabDetail) : TranslationState()
    data class Error(val message: String) : TranslationState()
}

sealed class AssignmentUiState {
    object Idle : AssignmentUiState()
    object Loading : AssignmentUiState()
    data class Success(val assignments: List<Assignment>) : AssignmentUiState()
    data class Error(val message: String) : AssignmentUiState()
}

class VocabViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VocabRepository

    // User session details
    var currentUser by mutableStateOf<User?>(null)
    private val sharedPrefs = application.getSharedPreferences("vocab_prefs", Context.MODE_PRIVATE)

    // Firebase Firestore & Auth Assignments state
    private val auth: FirebaseAuth? by lazy {
        try {
            if (com.google.firebase.FirebaseApp.getApps(getApplication()).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(getApplication())
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (com.google.firebase.FirebaseApp.getApps(getApplication()).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(getApplication())
            }
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    var studentClassId by mutableStateOf<String?>(sharedPrefs.getString("firebase_class_id", null))
        private set

    private var assignmentListenerRegistration: ListenerRegistration? = null

    private val _assignmentUiState = MutableStateFlow<AssignmentUiState>(AssignmentUiState.Idle)
    val assignmentUiState: StateFlow<AssignmentUiState> = _assignmentUiState.asStateFlow()

    // Onboarding & Target Goal States
    var dailyReadingGoalMinutes by mutableStateOf(sharedPrefs.getInt("daily_reading_goal_mins", 15))
        private set

    var hasCompletedOnboarding by mutableStateOf(sharedPrefs.getBoolean("has_completed_onboarding", false))
        private set

    var showOnboardingOverlay by mutableStateOf(false)
    var onboardingStep by mutableStateOf(0) // 0: Set Goal, 1: News CTA, 2: Custom Text CTA, 3: Flashcard Review, 4: Profile Stats
    var activeSubTab by mutableStateOf(0) // 0 = 即時新聞, 1 = 貼上/自訂教材

    var step1TargetRect by mutableStateOf<Rect?>(null)
    var step2TargetRect by mutableStateOf<Rect?>(null)
    var step3TargetRect by mutableStateOf<Rect?>(null)
    var step4TargetRect by mutableStateOf<Rect?>(null)

    fun updateDailyReadingGoal(minutes: Int) {
        val validMins = minutes.coerceIn(5, 180)
        dailyReadingGoalMinutes = validMins
        sharedPrefs.edit().putInt("daily_reading_goal_mins", validMins).apply()
    }

    fun nextOnboardingStep() {
        if (onboardingStep < 4) {
            onboardingStep++
            when (onboardingStep) {
                1 -> { currentTab = 0; activeSubTab = 0 }
                2 -> { currentTab = 0; activeSubTab = 1 }
                3 -> { currentTab = 1 }
                4 -> { currentTab = 2 }
            }
        } else {
            completeOnboarding()
        }
    }

    fun completeOnboarding() {
        showOnboardingOverlay = false
        hasCompletedOnboarding = true
        currentTab = 0
        activeSubTab = 0
        sharedPrefs.edit().putBoolean("has_completed_onboarding", true).apply()
    }

    fun restartOnboarding() {
        currentTab = 0
        onboardingStep = 0
        showOnboardingOverlay = true
    }

    // Daily launch checking state
    var isDailyInitLoading by mutableStateOf(true)
    var dailyInitMessage by mutableStateOf("正在準備您的智慧筆刷學時事系統...")

    // All users list for admin dashboard telemetry
    val allUsersList = mutableStateListOf<User>()

    fun loadAllUsers() {
        viewModelScope.launch {
            val users = withContext(Dispatchers.IO) {
                repository.getAllUsers()
            }
            allUsersList.clear()
            allUsersList.addAll(users)
        }
    }

    init {
        val database = VocabDatabase.getDatabase(application)
        repository = VocabRepository(database.vocabDao(), database.userDao())
        loadLoggedInUser()
        triggerDailyInitCheck()
        initFirebaseClassSync()
        auth?.currentUser?.uid?.let { uid ->
            startVocabCloudSync(uid)
            migrateLocalVocabDataToCloud(uid)
            migrateAndroidCollectionsToShared(uid)
        }

        // Periodic background timer tracking active reading duration (only increments when reading mode is active)
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000)
                val user = currentUser
                if (user != null && isReadingModeActive) {
                    val updated = user.copy(totalUsageTimeSeconds = user.totalUsageTimeSeconds + 5)
                    withContext(Dispatchers.IO) {
                        repository.updateUser(updated)
                    }
                    currentUser = updated
                }
            }
        }
    }

    // List of vocabulary words tracked from local database
    val vocabWords: StateFlow<List<VocabWord>> = repository.allWordsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // List of previously read articles tracked from local database
    val readHistory: StateFlow<List<com.example.data.ReadArticle>> = repository.allReadArticlesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Determines if the reading panel is active (or showing the Article Hub selection UI)
    var isReadingModeActive by mutableStateOf(false)

    // Track brushed words in the current reading session
    val sessionBrushedWords = mutableStateListOf<VocabDetail>()

    // Determines if the review dialog or review screen is showing
    var isReviewModeActive by mutableStateOf(false)

    // Store the words to be reviewed
    val wordsToReview = mutableStateListOf<VocabDetail>()

    // Custom reading font size configurations (value in sp)
    var readerFontSize by mutableStateOf(18f)

    // Current Translation API status UI
    private val _translationState = MutableStateFlow<TranslationState>(TranslationState.Idle)
    val translationState: StateFlow<TranslationState> = _translationState.asStateFlow()

    // Current screen layout tab index: 0 = Reading & Hub, 1 = Vocab Book
    var currentTab by mutableStateOf(0)

    // Current Reading Document text
    var documentTitle by mutableStateOf("")
    var documentText by mutableStateOf("")
    var assignmentTargetWords by mutableStateOf<List<String>>(emptyList())
    var initialPageToLoad by mutableStateOf(0)

    private var translationJob: kotlinx.coroutines.Job? = null

    // Text import modal state
    var showImportDialog by mutableStateOf(false)

    // Active translation overlay card visual status
    var showTranslationCard by mutableStateOf(false)

    // Public Dictionary state & metrics
    var savedAiCostsCount by mutableStateOf(sharedPrefs.getInt("saved_ai_costs_count", 0))
        private set

    var publicDictSearchResult by mutableStateOf<VocabDetail?>(null)
    var isPublicDictSearching by mutableStateOf(false)
    var publicDictSearchError by mutableStateOf<String?>(null)
    val publicDictSampleList = mutableStateListOf<VocabDetail>()

    fun fetchPublicDictSamples() {
        val db = firestore ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.collection("public_dictionary")
                    .limit(25)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot != null) {
                            val list = snapshot.documents.mapNotNull { doc ->
                                val w = doc.getString("word") ?: doc.id
                                val t = doc.getString("translation") ?: ""
                                val d = doc.getString("definition") ?: ""
                                val p = doc.getString("phonetic") ?: ""
                                val pos = doc.getString("partOfSpeech") ?: ""
                                if (t.isNotBlank()) VocabDetail(w, t, d, p, pos, "PUBLIC_DICT") else null
                            }
                            viewModelScope.launch(Dispatchers.Main) {
                                publicDictSampleList.clear()
                                publicDictSampleList.addAll(list)
                            }
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun searchPublicDictDirectly(query: String) {
        // Matches getFromPublicDictionary()'s own key derivation (trim+lowercase only) so this
        // search tool finds exactly what the real brush-lookup flow would find, phrases included.
        val clean = query.trim().lowercase()
        if (clean.isBlank()) return
        isPublicDictSearching = true
        publicDictSearchError = null
        publicDictSearchResult = null

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    VocabTranslationHelper.getFromPublicDictionary(firestore, clean)
                }
                if (result != null) {
                    publicDictSearchResult = result
                } else {
                    publicDictSearchError = "公共單字庫中尚未收錄 \"$clean\"。刷取單字時將會自動調用 AI 翻譯並存入！"
                }
            } catch (e: Exception) {
                publicDictSearchError = "查詢公共單字庫失敗：${e.localizedMessage}"
            } finally {
                isPublicDictSearching = false
            }
        }
    }

    // Dynamic news articles state fields
    private val _newsArticles = MutableStateFlow<List<com.example.network.NewsArticle>>(emptyList())
    val newsArticles: StateFlow<List<com.example.network.NewsArticle>> = _newsArticles.asStateFlow()

    private val _newsFetchError = MutableStateFlow<String?>(null)
    val newsFetchError: StateFlow<String?> = _newsFetchError.asStateFlow()

    private val _isNewsLoading = MutableStateFlow(false)
    val isNewsLoading: StateFlow<Boolean> = _isNewsLoading.asStateFlow()

    // Daily launch checking and pre-generation logic
    fun triggerDailyInitCheck() {
        viewModelScope.launch {
            isDailyInitLoading = true
            dailyInitMessage = "正在準備您的智慧筆刷學時事系統..."

            // Wait up to 1 second for currentUser to load if it's asynchronous
            var elapsed = 0
            while (currentUser == null && elapsed < 1000) {
                delay(100)
                elapsed += 100
            }

            val category = currentUser?.preferredCategory ?: "technology"
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val todayString = sdf.format(java.util.Date())

            try {
                // 1. Try Local Cache (第一層快取：本地 SQLite)
                dailyInitMessage = "🔍 正在檢查本地快取 (Local Cache)..."
                delay(300)
                val localCached = withContext(Dispatchers.IO) {
                    repository.getCachedNewsByCategoryAndDate(category, todayString)
                }

                if (localCached.isNotEmpty()) {
                    dailyInitMessage = "⚡️ 本地快取命中 (Local Cache Hit)！載入今日教材..."
                    _newsArticles.value = localCached.map {
                        com.example.network.NewsArticle(
                            source = null,
                            author = it.author,
                            title = it.title,
                            description = it.description,
                            content = null,
                            contentEasy = it.contentEasy,
                            contentMedium = it.contentMedium,
                            contentHard = it.contentHard,
                            url = "AI-Generated",
                            urlToImage = null,
                            image = null,
                            publishedAt = it.publishedAt
                        )
                    }
                    delay(500)
                } else {
                    // 2. Try Real Firestore Cloud Cache
                    dailyInitMessage = "📡 本地快取未命中，正在查詢 Firestore 全局共享新聞快取 (Community Articles)..."

                    var firestoreArticles = getFirestoreCloudCachedNews(category, todayString)

                    if (firestoreArticles.isEmpty()) {
                        val cloudCached = withContext(Dispatchers.IO) {
                            repository.getCloudCachedNewsByCategoryAndDate(category, todayString)
                        }
                        if (cloudCached.isNotEmpty()) {
                            firestoreArticles = cloudCached.map {
                                com.example.network.NewsArticle(
                                    source = null,
                                    author = it.author,
                                    title = it.title,
                                    description = it.description,
                                    content = null,
                                    contentEasy = it.contentEasy,
                                    contentMedium = it.contentMedium,
                                    contentHard = it.contentHard,
                                    url = "AI-Generated",
                                    urlToImage = null,
                                    image = null,
                                    publishedAt = it.publishedAt
                                )
                            }
                        }
                    }

                    if (firestoreArticles.isNotEmpty()) {
                        dailyInitMessage = "☁️ 雲端共享快取命中 (Firestore Cache Hit)！將教材同步至本地..."
                        withContext(Dispatchers.IO) {
                            repository.deleteOldCachedNews(todayString)
                            firestoreArticles.forEach {
                                repository.insertCachedNews(
                                    com.example.data.CachedNews(
                                        category = category,
                                        title = it.title,
                                        description = it.description ?: "",
                                        contentEasy = it.contentEasy ?: "",
                                        contentMedium = it.contentMedium ?: "",
                                        contentHard = it.contentHard ?: "",
                                        dateString = todayString,
                                        author = it.author ?: "AI Gemini",
                                        publishedAt = it.publishedAt ?: "Cloud Cache"
                                    )
                                )
                            }
                        }
                        _newsArticles.value = firestoreArticles
                        delay(400)
                    } else {
                        // 3. Cloud Cache Miss -> Generate with Gemini
                        dailyInitMessage = "🌐 全局快取未命中！正在透過 Gemini 生成今日 3 難度時事教材並同步至 Firestore 全局快取..."
                        generateAndCacheNewsForCategory(category, todayString)
                    }
                }
            } catch (e: Exception) {
                _newsFetchError.value = "今日啟動載入時發生錯誤：${e.localizedMessage}"
            } finally {
                isDailyInitLoading = false
            }
        }
    }

    private suspend fun generateAndCacheNewsForCategory(category: String, todayString: String) {
        // 1. Fetch raw news from RSS Feeds (Primary) or GNews API (Secondary / Fallback)
        var sourceLabel = "RSS Feeds"
        var headlinesList: List<String> = emptyList()

        if (com.example.network.NewsSourceConfig.activeProvider == com.example.network.NewsSourceProvider.RSS_FEED ||
            com.example.network.NewsSourceConfig.activeProvider == com.example.network.NewsSourceProvider.HYBRID_AUTO) {
            val rssItems = com.example.network.RssFeedService.fetchHeadlines(category, maxCount = 8)
            if (rssItems.isNotEmpty()) {
                sourceLabel = "Live RSS Feeds"
                headlinesList = rssItems.mapIndexed { idx, art ->
                    "${idx + 1}. [${art.sourceName}] Title: ${art.title}\nDescription: ${art.description}"
                }
            }
        }

        // Fallback to GNews API if RSS returned empty or if provider is explicitly set to GNEWS_API
        if (headlinesList.isEmpty() &&
            (com.example.network.NewsSourceConfig.activeProvider == com.example.network.NewsSourceProvider.GNEWS_API ||
             com.example.network.NewsSourceConfig.activeProvider == com.example.network.NewsSourceProvider.HYBRID_AUTO)) {
            val apiKey = com.example.BuildConfig.GNEWS_API_KEY
            val url = "https://gnews.io/api/v4/top-headlines?category=$category&lang=en&country=us&apikey=$apiKey"

            val gnewsResponse = withContext(Dispatchers.IO) {
                try {
                    com.example.network.NewsClient.api.fetchNewsByUrl(url)
                } catch (e: Exception) {
                    null
                }
            }

            val gnewsArticles = gnewsResponse?.articles?.take(8)
            if (!gnewsArticles.isNullOrEmpty()) {
                sourceLabel = "GNews API"
                headlinesList = gnewsArticles.mapIndexed { idx, art ->
                    "${idx + 1}. Title: ${art.title ?: ""}\nDescription: ${art.description ?: ""}"
                }
            }
        }

        // 2. Prepare the prompt for Gemini to generate 3 difficulty levels
        val prompt = if (headlinesList.isNotEmpty()) {
            """
                We have retrieved the latest real-time news headlines/topics from $sourceLabel for the category "$category":

                ${headlinesList.joinToString("\n\n")}

                Please act as a professional English journalist and educator. Based on these topics, write exactly 5 high-quality, engaging, full-length articles in English suitable for ESL vocabulary learning.
                For EACH topic, you MUST write three distinct versions of the article:
                - "contentEasy": Simplified English, easy vocabulary, shorter sentences, around 150-200 words (ESL A2-B1 level).
                - "contentMedium": Standard English, natural daily vocabulary, intermediate difficulty, around 200-250 words (ESL B2 level).
                - "contentHard": Advanced English, rich/challenging vocabulary, complex sentence structures, around 250-300 words (ESL C1-C2 level).

                You MUST respond with a JSON array of exactly 5 objects containing:
                - "title": a captivating educational headline in English
                - "description": a brief, interesting overview in English (1-2 sentences) of what this article covers
                - "contentEasy": the simplified version of the complete full-text
                - "contentMedium": the standard version of the complete full-text
                - "contentHard": the advanced version of the complete full-text

                JSON format:
                [
                  {
                    "title": "...",
                    "description": "...",
                    "contentEasy": "...",
                    "contentMedium": "...",
                    "contentHard": "..."
                  },
                  ...
                ]

                Do not output any markdown formatting, backticks, or "```json". Just return the raw JSON array.
            """.trimIndent()
        } else {
            """
                We were unable to retrieve external headlines.
                Please select 5 current, hot, and highly relevant educational topics in the category "$category" (e.g., if technology: AI, Space exploration, clean energy, etc.).
                Based on these topics, write exactly 5 high-quality, engaging, full-length articles in English suitable for ESL vocabulary learning.
                For EACH topic, you MUST write three distinct versions of the article:
                - "contentEasy": Simplified English, easy vocabulary, shorter sentences, around 150-200 words (ESL A2-B1 level).
                - "contentMedium": Standard English, natural daily vocabulary, intermediate difficulty, around 200-250 words (ESL B2 level).
                - "contentHard": Advanced English, rich/challenging vocabulary, complex sentence structures, around 250-300 words (ESL C1-C2 level).

                You MUST respond with a JSON array of exactly 5 objects containing:
                - "title": a captivating educational headline in English
                - "description": a brief, interesting overview in English (1-2 sentences) of what this article covers
                - "contentEasy": the simplified version of the complete full-text
                - "contentMedium": the standard version of the complete full-text
                - "contentHard": the advanced version of the complete full-text

                JSON format:
                [
                  {
                    "title": "...",
                    "description": "...",
                    "contentEasy": "...",
                    "contentMedium": "...",
                    "contentHard": "..."
                  },
                  ...
                ]

                Do not output any markdown formatting, backticks, or "```json". Just return the raw JSON array.
            """.trimIndent()
        }

        // 3. Request Gemini to write the articles via secure server gateway
        val request = com.example.network.GeminiRequest(
            contents = listOf(
                com.example.network.Content(parts = listOf(com.example.network.Part(text = prompt)))
            ),
            generationConfig = com.example.network.GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.7f
            )
        )

        try {
            val idToken = com.example.network.VocabTranslationHelper.fetchFirebaseIdToken()
            val geminiResponse = withContext(Dispatchers.IO) {
                val proxyUrl = if (com.example.network.ProxyGatewayConfig.proxyBaseUrl.endsWith("/")) {
                    com.example.network.ProxyGatewayConfig.proxyBaseUrl + "v1beta/models/gemini-3.5-flash-lite:generateContent"
                } else {
                    com.example.network.ProxyGatewayConfig.proxyBaseUrl + "/v1beta/models/gemini-3.5-flash-lite:generateContent"
                }

                val headers = mutableMapOf<String, String>()
                if (!idToken.isNullOrBlank()) {
                    headers["Authorization"] = "Bearer $idToken"
                }
                if (com.example.network.ProxyGatewayConfig.customHeaderKey.isNotBlank() && com.example.network.ProxyGatewayConfig.customHeaderValue.isNotBlank()) {
                    headers[com.example.network.ProxyGatewayConfig.customHeaderKey] = com.example.network.ProxyGatewayConfig.customHeaderValue
                }

                val maskedHeaders = headers.mapValues { (k, v) ->
                    if (k.equals("Authorization", ignoreCase = true) && v.length > 15) {
                        v.take(15) + "...[len ${v.length}]"
                    } else {
                        v
                    }
                }
                android.util.Log.d("VocabViewModel", "Calling article generation proxy URL: $proxyUrl | Headers: $maskedHeaders")
                com.example.network.GeminiClient.api.generateContentProxy(proxyUrl, headers, request)
            }

            val jsonText = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!jsonText.isNullOrBlank()) {
                val articlesList = mutableListOf<com.example.network.NewsArticle>()
                val jsonArray = org.json.JSONArray(jsonText.trim())

                // Delete old cached news first to keep DB small
                withContext(Dispatchers.IO) {
                    repository.deleteOldCachedNews(todayString)
                    repository.deleteOldCloudCachedNews(todayString)
                }

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val title = obj.optString("title")
                    val desc = obj.optString("description")
                    val contentEasy = obj.optString("contentEasy")
                    val contentMedium = obj.optString("contentMedium")
                    val contentHard = obj.optString("contentHard")

                    // Save to Simulated Cloud Cache and Local Cache (Cache-Aside Pattern)
                    withContext(Dispatchers.IO) {
                        repository.insertCloudCachedNews(
                            com.example.data.CloudCachedNews(
                                category = category,
                                title = title,
                                description = desc,
                                contentEasy = contentEasy,
                                contentMedium = contentMedium,
                                contentHard = contentHard,
                                dateString = todayString,
                                author = "AI Gemini",
                                publishedAt = "Just now by Gemini"
                            )
                        )

                        repository.insertCachedNews(
                            com.example.data.CachedNews(
                                category = category,
                                title = title,
                                description = desc,
                                contentEasy = contentEasy,
                                contentMedium = contentMedium,
                                contentHard = contentHard,
                                dateString = todayString,
                                author = "AI Gemini",
                                publishedAt = "Just now by Gemini"
                            )
                        )
                    }

                    articlesList.add(
                        com.example.network.NewsArticle(
                            source = null,
                            author = "AI Gemini",
                            title = title,
                            description = desc,
                            content = null,
                            contentEasy = contentEasy,
                            contentMedium = contentMedium,
                            contentHard = contentHard,
                            url = "AI-Generated",
                            urlToImage = null,
                            image = null,
                            publishedAt = "Just now by Gemini"
                        )
                    )
                }
                _newsArticles.value = articlesList
                saveToFirestoreCommunityArticles(category, todayString, articlesList)
            } else {
                _newsFetchError.value = "AI 生成時事文章失敗，傳回內容為空。請稍後重試。"
            }
        } catch (e: retrofit2.HttpException) {
            val code = e.code()
            val errorBody = e.response()?.errorBody()?.string()
            android.util.Log.e("VocabViewModel", "HTTP $code Error Body during article generation: $errorBody", e)
            val (title, detailMsg) = com.example.network.VocabTranslationHelper.parseHttpErrorDetails(code, errorBody)
            _newsFetchError.value = "$title\n$detailMsg"
        } catch (e: Exception) {
            android.util.Log.e("VocabViewModel", "Error generating articles", e)
            _newsFetchError.value = "AI 生成文章失敗：${e.localizedMessage ?: e.toString()}"
        }
    }

    // Fetches live news from GNews API, then uses Gemini API to rewrite topics into 5 high-quality, full-length reading articles
    fun fetchCategoryNews(category: String) {
        if (_isNewsLoading.value) return // Loading guard / anti-double-click lock
        _isNewsLoading.value = true
        _newsFetchError.value = null
        viewModelScope.launch {
            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val todayString = sdf.format(java.util.Date())

                // Try caching first
                // 1. Try Local Cache
                val localCached = withContext(Dispatchers.IO) {
                    repository.getCachedNewsByCategoryAndDate(category, todayString)
                }

                if (localCached.isNotEmpty()) {
                    _newsArticles.value = localCached.map {
                        com.example.network.NewsArticle(
                            source = null,
                            author = it.author,
                            title = it.title,
                            description = it.description,
                            content = null,
                            contentEasy = it.contentEasy,
                            contentMedium = it.contentMedium,
                            contentHard = it.contentHard,
                            url = "AI-Generated",
                            urlToImage = null,
                            image = null,
                            publishedAt = it.publishedAt
                        )
                    }
                    _isNewsLoading.value = false
                    return@launch
                }

                // 2. Try Firestore Cloud Cache
                var firestoreArticles = getFirestoreCloudCachedNews(category, todayString)
                if (firestoreArticles.isEmpty()) {
                    val cloudCached = withContext(Dispatchers.IO) {
                        repository.getCloudCachedNewsByCategoryAndDate(category, todayString)
                    }
                    if (cloudCached.isNotEmpty()) {
                        firestoreArticles = cloudCached.map {
                            com.example.network.NewsArticle(
                                source = null,
                                author = it.author,
                                title = it.title,
                                description = it.description,
                                content = null,
                                contentEasy = it.contentEasy,
                                contentMedium = it.contentMedium,
                                contentHard = it.contentHard,
                                url = "AI-Generated",
                                urlToImage = null,
                                image = null,
                                publishedAt = it.publishedAt
                            )
                        }
                    }
                }

                if (firestoreArticles.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        repository.deleteOldCachedNews(todayString)
                        firestoreArticles.forEach {
                            repository.insertCachedNews(
                                com.example.data.CachedNews(
                                    category = category,
                                    title = it.title,
                                    description = it.description ?: "",
                                    contentEasy = it.contentEasy ?: "",
                                    contentMedium = it.contentMedium ?: "",
                                    contentHard = it.contentHard ?: "",
                                    dateString = todayString,
                                    author = it.author ?: "AI Gemini",
                                    publishedAt = it.publishedAt ?: "Cloud Cache"
                                )
                            )
                        }
                    }
                    _newsArticles.value = firestoreArticles
                    _isNewsLoading.value = false
                    return@launch
                }

                // If not cached, let's fetch GNews + Gemini
                generateAndCacheNewsForCategory(category, todayString)
            } catch (e: Exception) {
                _newsFetchError.value = "處理時事文章時發生錯誤：${e.localizedMessage}"
            } finally {
                _isNewsLoading.value = false
            }
        }
    }

    // Loads selected news headline and content as standard reading document
    fun selectNewsArticle(article: com.example.network.NewsArticle) {
        val titleValue = "🌐 外媒時事新聞 | ${article.title}"
        val level = currentUser?.vocabLevel ?: "MEDIUM"
        val chosenContent = when (level) {
            "EASY" -> article.contentEasy ?: article.contentMedium ?: article.content ?: ""
            "HARD" -> article.contentHard ?: article.contentMedium ?: article.content ?: ""
            else -> article.contentMedium ?: article.content ?: ""
        }

        val combinedBody = buildString {
            if (!article.description.isNullOrBlank()) {
                append(article.description)
                append("\n\n")
            }
            if (chosenContent.isNotBlank()) {
                append(chosenContent)
            } else {
                append("（本篇時事新聞無更詳細段落內容，可點擊原始網址閱讀：${article.url}）")
            }
        }
        openArticle(titleValue, combinedBody)
    }

    // Submits Vocabulary Placement Test results and updates User's level
    fun submitVocabTestResult(level: String) {
        val user = currentUser ?: return
        viewModelScope.launch {
            val updated = user.copy(vocabLevel = level)
            withContext(Dispatchers.IO) {
                repository.updateUser(updated)
            }
            currentUser = updated

            // Sync vocabLevel directly to Firestore users/{uid}
            val uid = auth?.currentUser?.uid ?: user.uid
            if (uid.isNotBlank() && firestore != null) {
                firestore?.collection("users")?.document(uid)?.set(
                    mapOf("vocabLevel" to level),
                    SetOptions.merge()
                )
            }

            // After level test, guide user directly into spotlight onboarding tutorial
            if (!hasCompletedOnboarding) {
                onboardingStep = 0
                showOnboardingOverlay = true
            }
            // Re-trigger news loading / generation under the newly set placement level
            triggerDailyInitCheck()
        }
    }

    // User session flows helper functions
    fun loadLoggedInUser() {
        val authUser = auth?.currentUser
        if (authUser != null) {
            val uid = authUser.uid
            startVocabCloudSync(uid)
            migrateLocalVocabDataToCloud(uid)
            migrateAndroidCollectionsToShared(uid)
            val db = firestore
            if (db != null) {
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        if (doc != null && doc.exists()) {
                            val user = User(
                                uid = uid,
                                email = doc.getString("email") ?: (authUser.email ?: ""),
                                name = doc.getString("name") ?: (authUser.displayName ?: (authUser.email?.substringBefore("@") ?: "學習者")),
                                authProvider = doc.getString("authProvider") ?: "PASSWORD",
                                avatarColorHex = doc.getString("avatarColorHex") ?: "#6200EE",
                                vocabGoal = doc.getLong("vocabGoal")?.toInt() ?: 30,
                                vocabLevel = doc.getString("vocabLevel") ?: "PENDING",
                                role = doc.getString("role") ?: "",
                                classId = doc.getString("classId") ?: "",
                                preferredCategory = doc.getString("preferredCategory") ?: "technology",
                                totalUsageTimeSeconds = doc.getLong("totalUsageTimeSeconds") ?: 0L
                            )
                            viewModelScope.launch {
                                withContext(Dispatchers.IO) { repository.insertUser(user) }
                                currentUser = user
                                if (!user.classId.isNullOrBlank()) {
                                    studentClassId = user.classId
                                    sharedPrefs.edit().putString("firebase_class_id", user.classId).apply()
                                    listenToAssignments(user.classId)
                                }
                            }
                        } else {
                            viewModelScope.launch {
                                val cached = withContext(Dispatchers.IO) { repository.getUserByUid(uid) }
                                currentUser = cached ?: User(
                                    uid = uid,
                                    email = authUser.email ?: "",
                                    name = authUser.displayName ?: (authUser.email?.substringBefore("@") ?: "學習者")
                                )
                            }
                        }
                    }
                    .addOnFailureListener {
                        viewModelScope.launch {
                            val cached = withContext(Dispatchers.IO) { repository.getUserByUid(uid) }
                            currentUser = cached
                        }
                    }
            } else {
                viewModelScope.launch {
                    val cached = withContext(Dispatchers.IO) { repository.getUserByUid(uid) }
                    currentUser = cached
                }
            }
        } else {
            currentUser = null
        }
    }

    fun logoutUser() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        sharedPrefs.edit().remove("logged_in_uid").remove("logged_in_email").remove("firebase_class_id").apply()
        leaveClass()
        stopVocabCloudSync()
        // Room has no per-user scoping, so it's effectively "the currently logged-in account's cache" —
        // clear it on logout to avoid leaking one account's vocab/reading data to the next login on a shared device.
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllWords()
            repository.clearAllReadArticles()
        }
        currentUser = null
        currentTab = 0
        activeSubTab = 0
        isReadingModeActive = false
        isReviewModeActive = false
        showOnboardingOverlay = false
    }

    fun registerUser(email: String, name: String, passwordRaw: String, onResult: (Boolean, String) -> Unit) {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim()
        if (cleanEmail.isBlank() || cleanName.isBlank() || passwordRaw.isBlank()) {
            onResult(false, "請完整填寫信箱、姓名與密碼！")
            return
        }
        if (passwordRaw.length < 6) {
            onResult(false, "密碼長度至少需 6 個字元！")
            return
        }
        val authObj = auth
        if (authObj == null) {
            onResult(false, "Firebase 驗證模組初始化失敗，請檢查網路連線。")
            return
        }

        authObj.createUserWithEmailAndPassword(cleanEmail, passwordRaw)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid == null) {
                    onResult(false, "建立帳戶失敗，未取得有效身分 UID。")
                    return@addOnSuccessListener
                }
                startVocabCloudSync(uid)
                migrateLocalVocabDataToCloud(uid)
                migrateAndroidCollectionsToShared(uid)

                val profileData = hashMapOf<String, Any>(
                    "name" to cleanName,
                    "email" to cleanEmail,
                    "vocabGoal" to 30,
                    "vocabLevel" to "PENDING",
                    "authProvider" to "PASSWORD",
                    "avatarColorHex" to "#6200EE",
                    "totalUsageTimeSeconds" to 0L,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                val user = User(
                    uid = uid,
                    email = cleanEmail,
                    name = cleanName,
                    authProvider = "PASSWORD",
                    avatarColorHex = "#6200EE",
                    vocabGoal = 30,
                    vocabLevel = "PENDING",
                    role = "",
                    classId = "",
                    totalUsageTimeSeconds = 0L
                )

                val db = firestore
                if (db != null) {
                    db.collection("users").document(uid)
                        .set(profileData, SetOptions.merge())
                        .addOnSuccessListener {
                            viewModelScope.launch {
                                withContext(Dispatchers.IO) { repository.insertUser(user) }
                                currentUser = user
                                sharedPrefs.edit().putString("logged_in_uid", uid).apply()
                                onResult(true, "註冊成功！")
                            }
                        }
                        .addOnFailureListener {
                            viewModelScope.launch {
                                withContext(Dispatchers.IO) { repository.insertUser(user) }
                                currentUser = user
                                sharedPrefs.edit().putString("logged_in_uid", uid).apply()
                                onResult(true, "註冊成功！")
                            }
                        }
                } else {
                    viewModelScope.launch {
                        withContext(Dispatchers.IO) { repository.insertUser(user) }
                        currentUser = user
                        sharedPrefs.edit().putString("logged_in_uid", uid).apply()
                        onResult(true, "註冊成功！")
                    }
                }
            }
            .addOnFailureListener { e ->
                val msg = when {
                    e.message?.contains("The email address is already in use", ignoreCase = true) == true ->
                        "此信箱已被註冊，請直接切換至登入分頁！"
                    e.message?.contains("badly formatted", ignoreCase = true) == true ->
                        "電子郵件格式不正確，請檢查輸入！"
                    e.message?.contains("Password should be at least", ignoreCase = true) == true ->
                        "密碼強度不足，長度至少需 6 碼！"
                    else -> "註冊失敗：${e.localizedMessage ?: "請檢查網路連線"}"
                }
                onResult(false, msg)
            }
    }

    fun loginUser(email: String, passwordRaw: String, onResult: (Boolean, String) -> Unit) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || passwordRaw.isBlank()) {
            onResult(false, "電子郵件與密碼不得為空！")
            return
        }
        val authObj = auth
        if (authObj == null) {
            onResult(false, "Firebase 驗證模組尚未啟動，請檢查網路連線。")
            return
        }

        authObj.signInWithEmailAndPassword(cleanEmail, passwordRaw)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid == null) {
                    onResult(false, "登入失敗，無效之使用者身分。")
                    return@addOnSuccessListener
                }
                sharedPrefs.edit().putString("logged_in_uid", uid).apply()
                startVocabCloudSync(uid)
                migrateLocalVocabDataToCloud(uid)
                migrateAndroidCollectionsToShared(uid)
                val db = firestore
                if (db != null) {
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            val user = if (doc != null && doc.exists()) {
                                User(
                                    uid = uid,
                                    email = doc.getString("email") ?: cleanEmail,
                                    name = doc.getString("name") ?: cleanEmail.substringBefore("@"),
                                    authProvider = doc.getString("authProvider") ?: "PASSWORD",
                                    avatarColorHex = doc.getString("avatarColorHex") ?: "#6200EE",
                                    vocabGoal = doc.getLong("vocabGoal")?.toInt() ?: 30,
                                    vocabLevel = doc.getString("vocabLevel") ?: "PENDING",
                                    role = doc.getString("role") ?: "",
                                    classId = doc.getString("classId") ?: "",
                                    preferredCategory = doc.getString("preferredCategory") ?: "technology",
                                    totalUsageTimeSeconds = doc.getLong("totalUsageTimeSeconds") ?: 0L
                                )
                            } else {
                                User(
                                    uid = uid,
                                    email = cleanEmail,
                                    name = cleanEmail.substringBefore("@"),
                                    authProvider = "PASSWORD",
                                    vocabGoal = 30,
                                    vocabLevel = "PENDING"
                                )
                            }
                            viewModelScope.launch {
                                withContext(Dispatchers.IO) { repository.insertUser(user) }
                                currentUser = user
                                if (!user.classId.isNullOrBlank()) {
                                    studentClassId = user.classId
                                    sharedPrefs.edit().putString("firebase_class_id", user.classId).apply()
                                    listenToAssignments(user.classId)
                                }
                                onResult(true, "登入成功！")
                            }
                        }
                        .addOnFailureListener {
                            viewModelScope.launch {
                                val cached = withContext(Dispatchers.IO) { repository.getUserByUid(uid) }
                                currentUser = cached ?: User(
                                    uid = uid,
                                    email = cleanEmail,
                                    name = cleanEmail.substringBefore("@"),
                                    authProvider = "PASSWORD"
                                )
                                onResult(true, "登入成功！")
                            }
                        }
                } else {
                    viewModelScope.launch {
                        val cached = withContext(Dispatchers.IO) { repository.getUserByUid(uid) }
                        currentUser = cached ?: User(
                            uid = uid,
                            email = cleanEmail,
                            name = cleanEmail.substringBefore("@"),
                            authProvider = "PASSWORD"
                        )
                        onResult(true, "登入成功！")
                    }
                }
            }
            .addOnFailureListener { e ->
                val msg = when {
                    e.message?.contains("no user record", ignoreCase = true) == true ||
                    e.message?.contains("user-not-found", ignoreCase = true) == true ->
                        "找不到此信箱帳號，請先切換至註冊分頁建立新帳號！"
                    e.message?.contains("wrong-password", ignoreCase = true) == true ||
                    e.message?.contains("invalid-credential", ignoreCase = true) == true ||
                    e.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true ->
                        "帳號或密碼輸入不正確，請重新檢查！"
                    e.message?.contains("badly formatted", ignoreCase = true) == true ->
                        "電子郵件格式不正確！"
                    else -> "登入失敗：${e.localizedMessage ?: "請檢查網路連線"}"
                }
                onResult(false, msg)
            }
    }

    fun loginWithGoogle(email: String, name: String, onResult: (Boolean, String) -> Unit) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) {
            onResult(false, "Google 連線資料異常。")
            return
        }
        val authObj = auth
        if (authObj == null) {
            onResult(false, "Firebase 驗證模組尚未啟動")
            return
        }
        val defaultGooglePass = "GoogleAuth@2026"
        authObj.signInWithEmailAndPassword(cleanEmail, defaultGooglePass)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                sharedPrefs.edit().putString("logged_in_uid", uid).apply()
                loadLoggedInUser()
                onResult(true, "Google 授權登入成功！")
            }
            .addOnFailureListener {
                authObj.createUserWithEmailAndPassword(cleanEmail, defaultGooglePass)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: return@addOnSuccessListener
                        val profileData = hashMapOf<String, Any>(
                            "name" to name.trim().ifBlank { cleanEmail.substringBefore("@") },
                            "email" to cleanEmail,
                            "vocabGoal" to 30,
                            "vocabLevel" to "PENDING",
                            "authProvider" to "GOOGLE",
                            "avatarColorHex" to "#4285F4",
                            "totalUsageTimeSeconds" to 0L,
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                        firestore?.collection("users")?.document(uid)?.set(profileData, SetOptions.merge())
                        sharedPrefs.edit().putString("logged_in_uid", uid).apply()
                        loadLoggedInUser()
                        onResult(true, "Google 授權登入成功！")
                    }
                    .addOnFailureListener { err ->
                        onResult(false, "Google 授權登入失敗：${err.localizedMessage}")
                    }
            }
    }

    fun updateUserProfile(name: String, vocabGoal: Int, preferredCategory: String, avatarColorHex: String) {
        val user = currentUser ?: return
        viewModelScope.launch {
            val updated = user.copy(
                name = name.trim().ifBlank { user.name },
                vocabGoal = vocabGoal,
                preferredCategory = preferredCategory,
                avatarColorHex = avatarColorHex
            )
            withContext(Dispatchers.IO) {
                repository.updateUser(updated)
            }
            currentUser = updated

            // Sync updated profile to Firestore users/{uid}
            val uid = auth?.currentUser?.uid ?: user.uid
            if (uid.isNotBlank() && firestore != null) {
                firestore?.collection("users")?.document(uid)?.set(
                    mapOf(
                        "name" to updated.name,
                        "vocabGoal" to updated.vocabGoal,
                        "preferredCategory" to updated.preferredCategory,
                        "avatarColorHex" to updated.avatarColorHex
                    ),
                    SetOptions.merge()
                )
            }
        }
    }

    // ===================== Cloud sync: vocab words & reading progress =====================
    // Design: Room stays the offline-first local cache the UI already reads from; Firestore becomes
    // the durable, cross-device *and cross-app* source of truth (users/{uid}/vocabWords,
    // users/{uid}/readHistory - the same collections PWA already uses). Every local mutation also
    // pushes up to Firestore, and a real-time listener mirrors remote changes (from another device,
    // or from the PWA app on the same account, or the initial pull-down on a fresh install) back
    // into Room.
    //
    // 2026-09-23: this used to write to separate users/{uid}/androidVocabWords /
    // androidReadingProgress collections specifically to avoid Android and PWA clobbering each
    // other's differently-shaped records. Switched to the shared collections on request - the same
    // account's word list / reading history should be one thing, not split by which app touched it.
    // Fields each app doesn't know about (Android has no `phonetic`; PWA has no `contextSentence`)
    // are simply omitted from that app's own writes rather than written as blank - Firestore field
    // merges (SetOptions.merge() here, updateDoc() on the PWA side) leave whatever the other app
    // already set untouched. Doc ids are slugifyKey(word) / slugifyKey(title) on both sides now
    // (PWA switched from random addDoc() ids to the same scheme), so a word/article either app
    // creates first is the one the other app's later writes land on, instead of creating a duplicate.
    // migrateAndroidCollectionsToShared() below does a one-time move of any pre-existing
    // androidVocabWords/androidReadingProgress data for accounts that synced before this change.
    private var vocabWordsListenerRegistration: ListenerRegistration? = null
    private var readingProgressListenerRegistration: ListenerRegistration? = null

    private fun slugifyKey(text: String): String {
        val slug = text.trim().lowercase().replace(Regex("[^a-z0-9\\u4e00-\\u9fff]+"), "_").trim('_')
        return if (slug.isBlank()) "w_${kotlin.math.abs(text.hashCode())}" else slug.take(150)
    }

    // signInAnonymously() is used elsewhere (GeminiService.fetchFirebaseIdToken) purely as a
    // fallback to get *some* auth token for dictionary lookups when nobody is logged in yet — that
    // anonymous uid is per-install and must never be treated as a real, cross-device account.
    private fun isRealAccountSignedIn(): Boolean {
        val user = auth?.currentUser
        return user != null && !user.isAnonymous
    }

    private fun vocabWordToMap(word: VocabWord): Map<String, Any> = mapOf(
        "word" to word.word,
        "definition" to word.definition,
        "contextSentence" to word.contextSentence,
        "timestamp" to word.timestamp,
        "status" to word.status,
        "sourceArticle" to word.sourceArticle,
        "reviewCount" to word.reviewCount
    )

    private fun readArticleToMap(article: com.example.data.ReadArticle): Map<String, Any> = mapOf(
        "title" to article.title,
        "content" to article.content,
        "lastReadTime" to article.lastReadTime,
        "lastReadPage" to article.lastReadPage
    )

    private fun syncVocabWordUp(word: VocabWord) {
        if (!isRealAccountSignedIn()) return
        val uid = auth?.currentUser?.uid ?: return
        firestore?.collection("users")?.document(uid)?.collection("vocabWords")
            ?.document(slugifyKey(word.word))
            ?.set(vocabWordToMap(word), SetOptions.merge())
    }

    private fun syncVocabWordDelete(word: VocabWord) {
        if (!isRealAccountSignedIn()) return
        val uid = auth?.currentUser?.uid ?: return
        firestore?.collection("users")?.document(uid)?.collection("vocabWords")
            ?.document(slugifyKey(word.word))
            ?.delete()
    }

    private fun syncReadingProgressUp(article: com.example.data.ReadArticle) {
        if (!isRealAccountSignedIn()) return
        val uid = auth?.currentUser?.uid ?: return
        firestore?.collection("users")?.document(uid)?.collection("readHistory")
            ?.document(slugifyKey(article.title))
            ?.set(readArticleToMap(article), SetOptions.merge())
    }

    private fun syncReadingProgressDelete(article: com.example.data.ReadArticle) {
        if (!isRealAccountSignedIn()) return
        val uid = auth?.currentUser?.uid ?: return
        firestore?.collection("users")?.document(uid)?.collection("readHistory")
            ?.document(slugifyKey(article.title))
            ?.delete()
    }

    private suspend fun <T> awaitTask(task: com.google.android.gms.tasks.Task<T>): T? =
        suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }

    /**
     * Starts real-time listeners that mirror this account's cloud vocab words / reading progress
     * into the local Room cache. Safe to call repeatedly (e.g. on every login) — re-attaching just
     * replaces the previous listener registration. No-ops if not a real (non-anonymous) account.
     */
    fun startVocabCloudSync(uid: String) {
        val db = firestore ?: return
        if (!isRealAccountSignedIn()) return

        vocabWordsListenerRegistration?.remove()
        vocabWordsListenerRegistration = db.collection("users").document(uid)
            .collection("vocabWords")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                viewModelScope.launch(Dispatchers.IO) {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val word = doc.getString("word") ?: continue
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                            repository.getWordByText(word)?.let { repository.deleteById(it.id) }
                        } else {
                            val existing = repository.getWordByText(word)
                            val merged = VocabWord(
                                id = existing?.id ?: 0,
                                word = word,
                                definition = doc.getString("definition") ?: "",
                                contextSentence = doc.getString("contextSentence") ?: "",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                status = (doc.getLong("status") ?: 0L).toInt(),
                                sourceArticle = doc.getString("sourceArticle") ?: "未分類",
                                reviewCount = (doc.getLong("reviewCount") ?: 0L).toInt()
                            )
                            if (existing != null) repository.update(merged) else repository.insert(merged)
                        }
                    }
                }
            }

        readingProgressListenerRegistration?.remove()
        readingProgressListenerRegistration = db.collection("users").document(uid)
            .collection("readHistory")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                viewModelScope.launch(Dispatchers.IO) {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val title = doc.getString("title") ?: continue
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                            repository.getReadArticleByTitle(title)?.let { repository.deleteReadArticleById(it.id) }
                        } else {
                            val existing = repository.getReadArticleByTitle(title)
                            val merged = com.example.data.ReadArticle(
                                id = existing?.id ?: 0,
                                title = title,
                                content = doc.getString("content") ?: existing?.content ?: "",
                                lastReadTime = doc.getLong("lastReadTime") ?: System.currentTimeMillis(),
                                lastReadPage = (doc.getLong("lastReadPage") ?: 0L).toInt()
                            )
                            repository.insertReadArticle(merged)
                        }
                    }
                }
            }
    }

    fun stopVocabCloudSync() {
        vocabWordsListenerRegistration?.remove()
        vocabWordsListenerRegistration = null
        readingProgressListenerRegistration?.remove()
        readingProgressListenerRegistration = null
    }

    /**
     * One-time upload of this device's pre-existing local Room data for [uid], merged against
     * whatever's already in the cloud (e.g. from another device that logged into this same account
     * earlier). Gated by a per-(uid, device) flag so it only runs once; ongoing edits after that sync
     * incrementally via syncVocabWordUp/syncReadingProgressUp. Merge rule is a union that never
     * silently drops progress: higher reviewCount wins, mastered beats learning, most recent
     * lastReadTime wins for reading position.
     */
    fun migrateLocalVocabDataToCloud(uid: String) {
        if (!isRealAccountSignedIn()) return
        val db = firestore ?: return
        val flagKey = "vocab_cloud_migrated_$uid"
        if (sharedPrefs.getBoolean(flagKey, false)) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val wordsCol = db.collection("users").document(uid).collection("vocabWords")
                val remoteWordsBySlug = awaitTask(wordsCol.get())?.documents?.associateBy { it.id } ?: emptyMap()

                val progressCol = db.collection("users").document(uid).collection("readHistory")
                val remoteProgressBySlug = awaitTask(progressCol.get())?.documents?.associateBy { it.id } ?: emptyMap()

                val localWords = repository.getAllWords()
                val localArticles = repository.getAllReadArticles()

                val batch = db.batch()
                var writes = 0

                for (w in localWords) {
                    val slug = slugifyKey(w.word)
                    val remoteDoc = remoteWordsBySlug[slug]
                    val merged: Map<String, Any> = if (remoteDoc != null) {
                        val remoteTimestamp = remoteDoc.getLong("timestamp") ?: 0L
                        val useLocalDescriptive = w.timestamp >= remoteTimestamp
                        mapOf(
                            "word" to w.word,
                            "definition" to if (useLocalDescriptive) w.definition else (remoteDoc.getString("definition") ?: w.definition),
                            "contextSentence" to if (useLocalDescriptive) w.contextSentence else (remoteDoc.getString("contextSentence") ?: w.contextSentence),
                            "timestamp" to maxOf(w.timestamp, remoteTimestamp),
                            "status" to maxOf(w.status, (remoteDoc.getLong("status") ?: 0L).toInt()),
                            "sourceArticle" to if (useLocalDescriptive) w.sourceArticle else (remoteDoc.getString("sourceArticle") ?: w.sourceArticle),
                            "reviewCount" to maxOf(w.reviewCount, (remoteDoc.getLong("reviewCount") ?: 0L).toInt())
                        )
                    } else vocabWordToMap(w)
                    batch.set(wordsCol.document(slug), merged, SetOptions.merge())
                    writes++
                }

                for (a in localArticles) {
                    val slug = slugifyKey(a.title)
                    val remoteDoc = remoteProgressBySlug[slug]
                    val remoteTime = remoteDoc?.getLong("lastReadTime") ?: -1L
                    val merged: Map<String, Any> = if (remoteDoc != null && remoteTime >= a.lastReadTime) {
                        mapOf(
                            "title" to a.title,
                            "content" to (remoteDoc.getString("content") ?: a.content),
                            "lastReadTime" to remoteTime,
                            "lastReadPage" to (remoteDoc.getLong("lastReadPage") ?: a.lastReadPage.toLong())
                        )
                    } else readArticleToMap(a)
                    batch.set(progressCol.document(slug), merged, SetOptions.merge())
                    writes++
                }

                if (writes > 0) {
                    awaitTask(batch.commit())
                }
                sharedPrefs.edit().putBoolean(flagKey, true).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * One-time cleanup for accounts that already synced before vocabWords/readHistory were shared
     * with PWA (2026-09-23): moves whatever's sitting in the old users/{uid}/androidVocabWords and
     * users/{uid}/androidReadingProgress collections into the shared users/{uid}/vocabWords and
     * users/{uid}/readHistory collections, merges against anything already there (e.g. from PWA
     * usage of the same account, most-recent-timestamp/highest-progress wins per field, same rule
     * as migrateLocalVocabDataToCloud), then deletes the old docs so they don't linger as orphaned
     * duplicates. Both old and new collections key documents by the same slugifyKey(word/title), so
     * this is a direct doc-for-doc move, not a re-derivation. Independently gated/no-ops after the
     * first successful run, and no-ops immediately if the old collections are already empty.
     */
    fun migrateAndroidCollectionsToShared(uid: String) {
        if (!isRealAccountSignedIn()) return
        val db = firestore ?: return
        val flagKey = "android_collections_migrated_$uid"
        if (sharedPrefs.getBoolean(flagKey, false)) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val oldWordsCol = db.collection("users").document(uid).collection("androidVocabWords")
                val oldWords = awaitTask(oldWordsCol.get())?.documents ?: emptyList()
                val oldProgressCol = db.collection("users").document(uid).collection("androidReadingProgress")
                val oldProgress = awaitTask(oldProgressCol.get())?.documents ?: emptyList()

                if (oldWords.isEmpty() && oldProgress.isEmpty()) {
                    sharedPrefs.edit().putBoolean(flagKey, true).apply()
                    return@launch
                }

                val newWordsCol = db.collection("users").document(uid).collection("vocabWords")
                val newProgressCol = db.collection("users").document(uid).collection("readHistory")
                val newWordsById = awaitTask(newWordsCol.get())?.documents?.associateBy { it.id } ?: emptyMap()
                val newProgressById = awaitTask(newProgressCol.get())?.documents?.associateBy { it.id } ?: emptyMap()

                val batch = db.batch()

                for (old in oldWords) {
                    val existing = newWordsById[old.id]
                    val oldTimestamp = old.getLong("timestamp") ?: 0L
                    val existingTimestamp = existing?.getLong("timestamp") ?: -1L
                    val useOldDescriptive = oldTimestamp >= existingTimestamp
                    val merged = mapOf(
                        "word" to (old.getString("word") ?: old.id),
                        "definition" to if (useOldDescriptive) (old.getString("definition") ?: "") else (existing?.getString("definition") ?: old.getString("definition") ?: ""),
                        "contextSentence" to if (useOldDescriptive) (old.getString("contextSentence") ?: "") else (existing?.getString("contextSentence") ?: old.getString("contextSentence") ?: ""),
                        "timestamp" to maxOf(oldTimestamp, existingTimestamp),
                        "status" to maxOf((old.getLong("status") ?: 0L).toInt(), (existing?.getLong("status") ?: 0L).toInt()),
                        "sourceArticle" to if (useOldDescriptive) (old.getString("sourceArticle") ?: "") else (existing?.getString("sourceArticle") ?: old.getString("sourceArticle") ?: ""),
                        "reviewCount" to maxOf(old.getLong("reviewCount") ?: 0L, existing?.getLong("reviewCount") ?: 0L)
                    )
                    batch.set(newWordsCol.document(old.id), merged, SetOptions.merge())
                    batch.delete(oldWordsCol.document(old.id))
                }

                for (old in oldProgress) {
                    val existing = newProgressById[old.id]
                    val oldTime = old.getLong("lastReadTime") ?: 0L
                    val existingTime = existing?.getLong("lastReadTime") ?: -1L
                    val merged = if (existingTime > oldTime) {
                        mapOf(
                            "title" to (old.getString("title") ?: old.id),
                            "content" to (existing?.getString("content") ?: old.getString("content") ?: ""),
                            "lastReadTime" to existingTime,
                            "lastReadPage" to (existing?.getLong("lastReadPage") ?: old.getLong("lastReadPage") ?: 0L)
                        )
                    } else {
                        mapOf(
                            "title" to (old.getString("title") ?: old.id),
                            "content" to (old.getString("content") ?: existing?.getString("content") ?: ""),
                            "lastReadTime" to oldTime,
                            "lastReadPage" to (old.getLong("lastReadPage") ?: existing?.getLong("lastReadPage") ?: 0L)
                        )
                    }
                    batch.set(newProgressCol.document(old.id), merged, SetOptions.merge())
                    batch.delete(oldProgressCol.document(old.id))
                }

                awaitTask(batch.commit())
                sharedPrefs.edit().putBoolean(flagKey, true).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun recordArticleRead(title: String, content: String) {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getReadArticleByTitle(title)
            val saved = if (existing != null) {
                existing.copy(lastReadTime = System.currentTimeMillis())
            } else {
                com.example.data.ReadArticle(
                    title = title,
                    content = content,
                    lastReadTime = System.currentTimeMillis()
                )
            }
            repository.insertReadArticle(saved)
            syncReadingProgressUp(saved)
        }
    }

    fun openArticle(title: String, content: String, targetWords: List<String> = emptyList()) {
        documentTitle = title
        documentText = content
        assignmentTargetWords = targetWords
        sessionBrushedWords.clear()

        viewModelScope.launch {
            val existing = withContext(Dispatchers.IO) {
                repository.getReadArticleByTitle(title)
            }
            initialPageToLoad = existing?.lastReadPage ?: 0
            isReadingModeActive = true
            recordArticleRead(title, content)
        }
    }

    fun savePageProgress(title: String, pageIndex: Int) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getReadArticleByTitle(title)
            if (existing != null) {
                val updated = existing.copy(
                    lastReadPage = pageIndex,
                    lastReadTime = System.currentTimeMillis()
                )
                repository.insertReadArticle(updated)
                syncReadingProgressUp(updated)
            }
        }
    }

    fun exitReadingMode() {
        isReadingModeActive = false
        if (sessionBrushedWords.isNotEmpty()) {
            wordsToReview.clear()
            wordsToReview.addAll(sessionBrushedWords)
            isReviewModeActive = true
        }
    }

    fun deleteHistoryArticle(article: com.example.data.ReadArticle) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteReadArticleById(article.id)
            syncReadingProgressDelete(article)
        }
    }

    private var lastTranslationTime = 0L
    private var lastTranslatedWord = ""

    // Request translation for brushed words
    fun translateWord(word: String, paragraphContext: String) {
        if (word.isBlank()) return
        val now = System.currentTimeMillis()
        if (word == lastTranslatedWord && (now - lastTranslationTime) < 300) {
            return // Debounce rapid repeated taps on the same word
        }
        lastTranslationTime = now
        lastTranslatedWord = word

        showTranslationCard = true
        _translationState.value = TranslationState.Loading

        translationJob?.cancel() // Cancel previous job to prevent race conditions and network flooding
        translationJob = viewModelScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    VocabTranslationHelper.translateWordWithPublicDict(word, paragraphContext, firestore)
                }
                if (detail != null) {
                    if (detail.translation.contains("連線失敗") || detail.translation.contains("請先設定金鑰") || detail.translation.contains("發生異常")) {
                        _translationState.value = TranslationState.Error(detail.definition)
                    } else {
                        _translationState.value = TranslationState.Success(detail)

                        if (detail.source == "PUBLIC_DICT") {
                            savedAiCostsCount++
                            sharedPrefs.edit().putInt("saved_ai_costs_count", savedAiCostsCount).apply()
                        }

                        // Add to current session brushed words (if not already there)
                        if (sessionBrushedWords.none { it.word.equals(detail.word, ignoreCase = true) }) {
                            sessionBrushedWords.add(detail)
                        }

                        // Auto-save the successfully fetched word to the local database
                        saveWordToDb(detail, paragraphContext)
                    }
                } else {
                    _translationState.value = TranslationState.Error("找不到該字詞的翻譯。")
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _translationState.value = TranslationState.Error(e.localizedMessage ?: "翻譯連線失敗。")
                }
            }
        }
    }

    private fun saveWordToDb(detail: VocabDetail, sentence: String) {
        if (detail.translation.contains("連線失敗") || detail.translation.contains("請先設定金鑰") || detail.translation.contains("發生異常")) {
            return
        }
        val currentTitle = if (documentTitle.isBlank()) "自訂匯入文件" else documentTitle
        viewModelScope.launch(Dispatchers.IO) {
            // Check if word already exists in local database
            val existing = repository.getWordByText(detail.word)
            if (existing == null) {
                val newWord = VocabWord(
                    word = detail.word,
                    definition = "${detail.partOfSpeech}. ${detail.translation} - ${detail.definition}",
                    contextSentence = sentence,
                    status = 0,
                    sourceArticle = currentTitle,
                    reviewCount = 1
                )
                repository.insert(newWord)
                syncVocabWordUp(newWord)
            } else {
                val newCount = existing.reviewCount + 1
                val newStatus = if (newCount >= 5) 1 else existing.status
                // Update timestamp/sentence to keep it fresh
                val updated = existing.copy(
                    definition = "${detail.partOfSpeech}. ${detail.translation} - ${detail.definition}",
                    contextSentence = sentence,
                    timestamp = System.currentTimeMillis(),
                    sourceArticle = currentTitle,
                    reviewCount = newCount,
                    status = newStatus
                )
                repository.update(updated)
                syncVocabWordUp(updated)
            }
        }
    }

    fun recordWordReviewByText(wordText: String) {
        if (wordText.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getWordByText(wordText) ?: return@launch
            val updatedCount = existing.reviewCount + 1
            val newStatus = if (updatedCount >= 5) 1 else existing.status
            val updated = existing.copy(reviewCount = updatedCount, status = newStatus)
            repository.update(updated)
            syncVocabWordUp(updated)
        }
    }

    /**
     * Starts a review session. Pass [customWords] to review one specific article's words in full
     * (e.g. from the vocab book's per-article "複習這篇文章" button) — used as-is, no cap or shuffle.
     * With no [customWords] (the general "隨機複習" entry points), a vocab book that's grown large
     * would otherwise turn every review into reviewing everything at once, so this instead takes a
     * random sample of 10 words, prioritizing not-yet-mastered words first and only filling the rest
     * from mastered words if there aren't 10 still being learned.
     */
    fun startReviewSession(customWords: List<VocabWord>? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val selectedWords = if (customWords != null) {
                if (customWords.isEmpty()) return@launch
                customWords
            } else {
                val allLocalWords = repository.getAllWords()
                if (allLocalWords.isEmpty()) return@launch
                val learningWords = allLocalWords.filter { it.status == 0 }.shuffled()
                val masteredWords = allLocalWords.filter { it.status == 1 }.shuffled()
                (learningWords + masteredWords).take(10)
            }

            val details = selectedWords.map { w ->
                val parts = w.definition.split("-", limit = 2)
                val partDef = parts.getOrNull(0) ?: ""
                val detailedSentence = parts.getOrNull(1)?.trim() ?: ""

                val pos = if (partDef.contains(".")) partDef.substringBefore(".").trim() else "單字"
                val trans = if (partDef.contains(".")) partDef.substringAfter(".").trim() else partDef.trim()

                VocabDetail(
                    word = w.word,
                    partOfSpeech = pos.ifBlank { "n" },
                    translation = trans.ifBlank { w.word },
                    definition = detailedSentence,
                    phonetic = ""
                )
            }

            withContext(Dispatchers.Main) {
                wordsToReview.clear()
                wordsToReview.addAll(details)
                isReviewModeActive = true
            }
        }
    }

    fun deleteWord(vocabWord: VocabWord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteById(vocabWord.id)
            syncVocabWordDelete(vocabWord)
        }
    }

    fun toggleWordMastered(vocabWord: VocabWord) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = vocabWord.copy(status = if (vocabWord.status == 0) 1 else 0)
            repository.update(updated)
            syncVocabWordUp(updated)
        }
    }

    fun clearTranslationState() {
        showTranslationCard = false
        _translationState.value = TranslationState.Idle
    }

    /**
     * Resolves a real Firebase Auth uid and hands it to [onReady].
     * Reads directly from FirebaseAuth.currentUser. If not authenticated, hands null.
     */
    private fun ensureFirebaseAuth(onReady: (uid: String?) -> Unit) {
        val authObj = auth
        val current = authObj?.currentUser
        if (current != null) {
            onReady(current.uid)
        } else {
            onReady(null)
        }
    }

    /**
     * Re-syncs class membership from the server on app start.
     * Design: a user starts in no class at all. They only ever gain access to a class's
     * assignments by successfully submitting an existing, teacher-created class code via
     * joinClass(). This function must NOT start listening to assignments using the locally
     * cached classId until Firebase Auth has actually finished signing in — firestore.rules'
     * isClassMember() requires request.auth != null, so attaching the listener before auth
     * resolves reliably produces a spurious PERMISSION_DENIED error on every cold start.
     * The server's users/{uid}.classId is treated as the source of truth; if the server has
     * no classId (e.g. it was cleared by a teacher, or a previous join attempt never actually
     * persisted), the local cache is cleared too instead of trusting stale local state.
     */
    fun initFirebaseClassSync() {
        val db = firestore
        if (db == null) {
            // No Firestore configured (e.g. offline/dev build) — best effort with local cache only.
            val savedClassId = studentClassId
            if (!savedClassId.isNullOrBlank()) {
                listenToAssignments(savedClassId)
            }
            return
        }

        ensureFirebaseAuth { uid ->
            if (uid == null) {
                // Can't authenticate right now — don't attach a listener that's guaranteed to be denied.
                if (!studentClassId.isNullOrBlank()) {
                    _assignmentUiState.value = AssignmentUiState.Error("無法連線驗證身分，暫時無法同步班級作業，請檢查網路後重新開啟 App。")
                }
                return@ensureFirebaseAuth
            }

            try {
                db.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        val cloudClassId = if (doc != null && doc.exists()) doc.getString("classId") else null
                        if (!cloudClassId.isNullOrBlank()) {
                            studentClassId = cloudClassId
                            sharedPrefs.edit().putString("firebase_class_id", cloudClassId).apply()
                            listenToAssignments(cloudClassId)
                        } else if (!studentClassId.isNullOrBlank()) {
                            // Server has no class on record but we have a local cache — it's stale, clear it
                            // instead of listening with a classId the server will just deny.
                            leaveClass()
                        }
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        // Couldn't reach the server this time; fall back to the local cache so the student
                        // isn't locked out purely due to a transient network error.
                        val savedClassId = studentClassId
                        if (!savedClassId.isNullOrBlank()) {
                            listenToAssignments(savedClassId)
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Joins a class by code. Per design, this is the ONLY event that should grant access to a
     * class's assignments: the user must enter a class code that (a) actually exists and (b) was
     * created by a teacher. Two bugs from the previous implementation are fixed here:
     *  1. The class code was never checked against classes/{classId} before use — any string
     *     would "succeed" locally even if no such class existed.
     *  2. Every failure path (addOnFailureListener AND the catch block) still wrote the classId
     *     locally, started the assignments listener, and called onResult(true, ...) — i.e. it lied
     *     about success. That leaves local state pointing at a classId the server never actually
     *     recorded on users/{uid}, so isClassMember() in firestore.rules denies the assignments
     *     listener afterwards. This is almost certainly the "login error is missing classId" symptom.
     */
    fun joinClass(classIdInput: String, onResult: (Boolean, String?) -> Unit) {
        val cleanId = classIdInput.trim()
        if (cleanId.isEmpty()) {
            onResult(false, "班級代碼不能為空！")
            return
        }

        val db = firestore
        if (db == null) {
            // No Firestore configured (e.g. offline/dev build) — accept locally only, nothing to validate against.
            studentClassId = cleanId
            sharedPrefs.edit().putString("firebase_class_id", cleanId).apply()
            listenToAssignments(cleanId)
            onResult(true, "已綁定班級代碼：$cleanId")
            return
        }

        ensureFirebaseAuth { uid ->
            if (uid == null) {
                onResult(false, "無法建立登入連線，請檢查網路後再試一次。")
                return@ensureFirebaseAuth
            }

            // Step 1: verify the class code actually exists (classes/{classId} get is allowed by
            // firestore.rules specifically for this check).
            db.collection("classes").document(cleanId)
                .get()
                .addOnSuccessListener { classDoc ->
                    if (classDoc == null || !classDoc.exists()) {
                        onResult(false, "找不到此班級代碼，請確認代碼是否正確，或向老師確認班級是否已建立。")
                        return@addOnSuccessListener
                    }

                    // Step 2: class exists — now write classId onto the user's own profile document.
                    val userData = hashMapOf(
                        "name" to (currentUser?.name ?: "學生"),
                        "role" to "student",
                        "classId" to cleanId
                    )

                    db.collection("users").document(uid)
                        .set(userData, SetOptions.merge())
                        .addOnSuccessListener {
                            studentClassId = cleanId
                            sharedPrefs.edit().putString("firebase_class_id", cleanId).apply()
                            listenToAssignments(cleanId)
                            onResult(true, "已成功加入班級：$cleanId")
                        }
                        .addOnFailureListener { e ->
                            // Do NOT touch local state or start listening — the server write failed,
                            // so pretending success here is exactly what caused the stale-state bug.
                            onResult(false, "加入班級失敗，請稍後再試：${e.localizedMessage}")
                        }
                }
                .addOnFailureListener { e ->
                    onResult(false, "驗證班級代碼失敗，請檢查網路後再試一次：${e.localizedMessage}")
                }
        }
    }

    fun listenToAssignments(classId: String) {
        val cleanClassId = classId.trim()
        if (cleanClassId.isBlank()) return
        val db = firestore
        if (db == null) {
            _assignmentUiState.value = AssignmentUiState.Success(emptyList())
            return
        }
        assignmentListenerRegistration?.remove()
        _assignmentUiState.value = AssignmentUiState.Loading

        try {
            // Strictly listen to subcollection path: classes/{classCode}/assignments
            val collectionRef = db.collection("classes")
                .document(cleanClassId)
                .collection("assignments")

            val query = collectionRef.orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)

            assignmentListenerRegistration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _assignmentUiState.value = AssignmentUiState.Error("即時同步作業失敗：${error.localizedMessage}")
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        // The server no longer considers this device a member of the class (e.g. stale
                        // local cache from a join that never actually persisted). Reset locally so the
                        // student isn't stuck seeing a permanent error for a class they're not really in.
                        leaveClass()
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val title = doc.getString("title") ?: "未命名作業"
                            val content = doc.getString("content") ?: ""
                            @Suppress("UNCHECKED_CAST")
                            val rawWords = doc.get("targetWordList") as? List<*>
                            val targetWordList = rawWords?.mapNotNull { it?.toString() } ?: emptyList()
                            val dueDate = doc.getTimestamp("dueDate")
                            val createdAt = doc.getTimestamp("createdAt")

                            Assignment(
                                id = id,
                                classId = cleanClassId,
                                title = title,
                                content = content,
                                targetWordList = targetWordList,
                                dueDate = dueDate,
                                createdAt = createdAt
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.createdAt?.seconds ?: 0L }

                    _assignmentUiState.value = AssignmentUiState.Success(list)
                } else {
                    _assignmentUiState.value = AssignmentUiState.Success(emptyList())
                }
            }
        } catch (e: Exception) {
            _assignmentUiState.value = AssignmentUiState.Error("連線異常：${e.localizedMessage}")
        }
    }

    fun leaveClass() {
        assignmentListenerRegistration?.remove()
        assignmentListenerRegistration = null
        studentClassId = null
        sharedPrefs.edit().remove("firebase_class_id").apply()
        _assignmentUiState.value = AssignmentUiState.Idle

        // Best-effort: also clear the server-side record so the next initFirebaseClassSync() doesn't
        // just pull the same stale classId back in and re-trigger the same PERMISSION_DENIED loop.
        val db = firestore
        val uid = auth?.currentUser?.uid
        if (db != null && uid != null) {
            db.collection("users").document(uid)
                .update("classId", com.google.firebase.firestore.FieldValue.delete())
                .addOnFailureListener { e -> e.printStackTrace() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        assignmentListenerRegistration?.remove()
        assignmentListenerRegistration = null
    }

    /**
     * Queries real Firestore collection "community_articles" for shared news across all users
     */
    suspend fun getFirestoreCloudCachedNews(category: String, dateString: String): List<com.example.network.NewsArticle> {
        val db = firestore ?: return emptyList()
        return try {
            val snapshot = suspendCancellableCoroutine<com.google.firebase.firestore.QuerySnapshot?> { cont ->
                db.collection("community_articles")
                    .whereEqualTo("category", category)
                    .whereEqualTo("dateString", dateString)
                    .get()
                    .addOnSuccessListener { snp -> cont.resume(snp) }
                    .addOnFailureListener { cont.resume(null) }
            }
            if (snapshot != null && !snapshot.isEmpty) {
                snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val desc = doc.getString("description")
                    val contentEasy = doc.getString("contentEasy")
                    val contentMedium = doc.getString("contentMedium")
                    val contentHard = doc.getString("contentHard")
                    val author = doc.getString("author") ?: "AI Gemini (Cloud Shared)"
                    val publishedAt = doc.getString("publishedAt") ?: "Cloud Shared"
                    com.example.network.NewsArticle(
                        source = null,
                        author = author,
                        title = title,
                        description = desc,
                        content = null,
                        contentEasy = contentEasy,
                        contentMedium = contentMedium,
                        contentHard = contentHard,
                        url = "AI-Generated",
                        urlToImage = null,
                        image = null,
                        publishedAt = publishedAt
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("VocabViewModel", "Error querying community_articles from Firestore", e)
            emptyList()
        }
    }

    /**
     * Saves generated news articles to Firestore "community_articles" collection so all students share API cost
     */
    fun saveToFirestoreCommunityArticles(category: String, dateString: String, articles: List<com.example.network.NewsArticle>) {
        val db = firestore ?: return
        try {
            articles.forEachIndexed { index, art ->
                val docId = "${category}_${dateString}_$index".lowercase().replace(Regex("[^a-z0-9_]"), "")
                val data = hashMapOf(
                    "category" to category,
                    "dateString" to dateString,
                    "title" to art.title,
                    "description" to (art.description ?: ""),
                    "contentEasy" to (art.contentEasy ?: ""),
                    "contentMedium" to (art.contentMedium ?: ""),
                    "contentHard" to (art.contentHard ?: ""),
                    "author" to (art.author ?: "AI Gemini"),
                    "publishedAt" to (art.publishedAt ?: "Just now by Gemini"),
                    "createdAt" to System.currentTimeMillis()
                )
                db.collection("community_articles").document(docId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        android.util.Log.d("VocabViewModel", "Successfully published shared community article: $docId")
                    }
            }
        } catch (e: Exception) {
            android.util.Log.e("VocabViewModel", "Error writing community_articles to Firestore", e)
        }
    }
}
