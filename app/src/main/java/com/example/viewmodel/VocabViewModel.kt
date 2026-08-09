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

    var showOnboardingOverlay by mutableStateOf(!sharedPrefs.getBoolean("has_completed_onboarding", false))
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
        val clean = query.trim().lowercase().replace(Regex("[^a-zA-Z-]"), "")
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
        // 1. Fetch raw news from GNews API
        val apiKey = com.example.BuildConfig.GNEWS_API_KEY
        val url = "https://gnews.io/api/v4/top-headlines?category=$category&lang=en&country=us&apikey=$apiKey"
        
        val gnewsResponse = withContext(Dispatchers.IO) {
            try {
                com.example.network.NewsClient.api.fetchNewsByUrl(url)
            } catch (e: Exception) {
                null
            }
        }

        // 2. Prepare the prompt for Gemini to generate 3 difficulty levels
        val headlinesList = gnewsResponse?.articles?.take(8)?.mapIndexed { idx, art ->
            "${idx + 1}. Title: ${art.title ?: ""}\nDescription: ${art.description ?: ""}"
        } ?: emptyList()

        val prompt = if (headlinesList.isNotEmpty()) {
            """
                We have retrieved some latest real-time news headlines/topics from GNews API for the category "$category":
                
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
                We were unable to retrieve the latest news headlines from GNews.
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

        // 3. Request Gemini to write the articles
        val geminiApiKey = com.example.BuildConfig.GEMINI_API_KEY
        val hasKey = geminiApiKey.isNotEmpty() && geminiApiKey != "MY_GEMINI_API_KEY"

        if (!com.example.network.ProxyGatewayConfig.isEnabled && !hasKey) {
            _newsFetchError.value = "請先在 [個人中心] 設定 Gemini API 金鑰，或在設定中啟用 API 代理伺服器，方能驅動 AI 生成全文時事。"
            _newsArticles.value = emptyList()
            return
        }

        val request = com.example.network.GeminiRequest(
            contents = listOf(
                com.example.network.Content(parts = listOf(com.example.network.Part(text = prompt)))
            ),
            generationConfig = com.example.network.GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.7f
            )
        )

        val geminiResponse = withContext(Dispatchers.IO) {
            if (com.example.network.ProxyGatewayConfig.isEnabled) {
                val proxyUrl = if (com.example.network.ProxyGatewayConfig.proxyBaseUrl.endsWith("/")) {
                    com.example.network.ProxyGatewayConfig.proxyBaseUrl + "v1beta/models/gemini-3.5-flash:generateContent"
                } else {
                    com.example.network.ProxyGatewayConfig.proxyBaseUrl + "/v1beta/models/gemini-3.5-flash:generateContent"
                }
                
                val headers = mutableMapOf<String, String>()
                if (com.example.network.ProxyGatewayConfig.customHeaderKey.isNotBlank() && com.example.network.ProxyGatewayConfig.customHeaderValue.isNotBlank()) {
                    headers[com.example.network.ProxyGatewayConfig.customHeaderKey] = com.example.network.ProxyGatewayConfig.customHeaderValue
                }
                com.example.network.GeminiClient.api.generateContentProxy(proxyUrl, headers, request)
            } else {
                com.example.network.GeminiClient.api.generateContent(geminiApiKey, request)
            }
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
    }

    // Fetches live news from GNews API, then uses Gemini API to rewrite topics into 5 high-quality, full-length reading articles
    fun fetchCategoryNews(category: String) {
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
            // Re-trigger news loading / generation under the newly set placement level
            triggerDailyInitCheck()
        }
    }

    // User session flows helper functions
    fun loadLoggedInUser() {
        val email = sharedPrefs.getString("logged_in_email", null)
        if (email != null) {
            viewModelScope.launch {
                val user = withContext(Dispatchers.IO) {
                    repository.getUserByEmail(email)
                }
                currentUser = user
            }
        } else {
            currentUser = null
        }
    }

    fun logoutUser() {
        sharedPrefs.edit().remove("logged_in_email").apply()
        currentUser = null
    }

    fun registerUser(email: String, name: String, passwordRaw: String, onResult: (Boolean, String) -> Unit) {
        if (email.isBlank() || name.isBlank() || passwordRaw.isBlank()) {
            onResult(false, "資料欄位不得為空！")
            return
        }
        viewModelScope.launch {
            val existing = withContext(Dispatchers.IO) { repository.getUserByEmail(email.trim().lowercase()) }
            if (existing != null) {
                onResult(false, "此信箱帳號已建立，請直接登入。")
                return@launch
            }
            val passHash = passwordRaw.hashCode().toString()
            val newUser = User(
                email = email.trim().lowercase(),
                name = name.trim(),
                passwordHash = passHash,
                authProvider = "CUSTOM"
            )
            withContext(Dispatchers.IO) {
                repository.insertUser(newUser)
            }
            sharedPrefs.edit().putString("logged_in_email", email.trim().lowercase()).apply()
            loadLoggedInUser()
            onResult(true, "註冊成功！")
        }
    }

    fun loginUser(email: String, passwordRaw: String, onResult: (Boolean, String) -> Unit) {
        if (email.isBlank() || passwordRaw.isBlank()) {
            onResult(false, "電子郵件與密碼不得為空！")
            return
        }
        viewModelScope.launch {
            val user = withContext(Dispatchers.IO) { repository.getUserByEmail(email.trim().lowercase()) }
            if (user == null) {
                onResult(false, "此信箱帳號尚未註冊過。")
                return@launch
            }
            if (user.authProvider == "GOOGLE") {
                onResult(false, "此帳號是以 Google 授權登入，請點擊 Google 登入按鈕類型。")
                return@launch
            }
            val passHash = passwordRaw.hashCode().toString()
            if (user.passwordHash == passHash) {
                val updatedUser = user.copy(loginCount = user.loginCount + 1)
                withContext(Dispatchers.IO) { repository.updateUser(updatedUser) }
                sharedPrefs.edit().putString("logged_in_email", email.trim().lowercase()).apply()
                loadLoggedInUser()
                onResult(true, "登入成功！")
            } else {
                onResult(false, "認證失敗，輸入的密碼不正確！")
            }
        }
    }

    fun loginWithGoogle(email: String, name: String, onResult: (Boolean, String) -> Unit) {
        if (email.isBlank()) {
            onResult(false, "Google 連線資料異常。")
            return
        }
        viewModelScope.launch {
            val existing = withContext(Dispatchers.IO) { repository.getUserByEmail(email.trim().lowercase()) }
            if (existing == null) {
                val newUser = User(
                    email = email.trim().lowercase(),
                    name = name.trim().ifBlank { email.substringBefore("@") },
                    authProvider = "GOOGLE",
                    avatarColorHex = "#4285F4", // Google brand blue color code
                    loginCount = 1
                )
                withContext(Dispatchers.IO) {
                    repository.insertUser(newUser)
                }
            } else {
                val updatedUser = existing.copy(loginCount = existing.loginCount + 1)
                withContext(Dispatchers.IO) { repository.updateUser(updatedUser) }
            }
            sharedPrefs.edit().putString("logged_in_email", email.trim().lowercase()).apply()
            loadLoggedInUser()
            onResult(true, "Google 授權登入成功！")
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
        }
    }

    fun recordArticleRead(title: String, content: String) {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getReadArticleByTitle(title)
            if (existing != null) {
                repository.insertReadArticle(existing.copy(lastReadTime = System.currentTimeMillis()))
            } else {
                repository.insertReadArticle(
                    com.example.data.ReadArticle(
                        title = title,
                        content = content,
                        lastReadTime = System.currentTimeMillis()
                    )
                )
            }
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
                repository.insertReadArticle(existing.copy(
                    lastReadPage = pageIndex,
                    lastReadTime = System.currentTimeMillis()
                ))
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
        }
    }

    // Request translation for brushed words
    fun translateWord(word: String, paragraphContext: String) {
        if (word.isBlank()) return
        
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
        }
    }

    fun startReviewSession(customWords: List<VocabWord>? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val allLocalWords = customWords ?: repository.getAllWords()
            if (allLocalWords.isEmpty()) {
                return@launch
            }
            // Prioritize learning/unmastered words (status == 0)
            val learningWords = allLocalWords.filter { it.status == 0 }
            val selectedWords = if (learningWords.isNotEmpty()) learningWords else allLocalWords

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
        }
    }

    fun toggleWordMastered(vocabWord: VocabWord) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = vocabWord.copy(status = if (vocabWord.status == 0) 1 else 0)
            repository.update(updated)
        }
    }

    fun clearTranslationState() {
        showTranslationCard = false
        _translationState.value = TranslationState.Idle
    }

    private fun ensureFirebaseAuth(onReady: (String) -> Unit) {
        val authObj = auth
        if (authObj == null) {
            val deviceUid = sharedPrefs.getString("anon_device_uid", null) ?: java.util.UUID.randomUUID().toString().also {
                sharedPrefs.edit().putString("anon_device_uid", it).apply()
            }
            onReady(deviceUid)
            return
        }

        try {
            val current = authObj.currentUser
            if (current != null) {
                onReady(current.uid)
            } else {
                authObj.signInAnonymously()
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user
                        if (user != null) {
                            onReady(user.uid)
                        } else {
                            val deviceUid = sharedPrefs.getString("anon_device_uid", null) ?: java.util.UUID.randomUUID().toString().also {
                                sharedPrefs.edit().putString("anon_device_uid", it).apply()
                            }
                            onReady(deviceUid)
                        }
                    }
                    .addOnFailureListener {
                        val deviceUid = sharedPrefs.getString("anon_device_uid", null) ?: java.util.UUID.randomUUID().toString().also {
                            sharedPrefs.edit().putString("anon_device_uid", it).apply()
                        }
                        onReady(deviceUid)
                    }
            }
        } catch (e: Exception) {
            val deviceUid = sharedPrefs.getString("anon_device_uid", null) ?: java.util.UUID.randomUUID().toString().also {
                sharedPrefs.edit().putString("anon_device_uid", it).apply()
            }
            onReady(deviceUid)
        }
    }

    fun initFirebaseClassSync() {
        val savedClassId = studentClassId
        if (!savedClassId.isNullOrBlank()) {
            listenToAssignments(savedClassId)
        }

        val db = firestore ?: return
        ensureFirebaseAuth { uid ->
            try {
                db.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        if (doc != null && doc.exists()) {
                            val cloudClassId = doc.getString("classId")
                            if (!cloudClassId.isNullOrBlank() && cloudClassId != studentClassId) {
                                studentClassId = cloudClassId
                                sharedPrefs.edit().putString("firebase_class_id", cloudClassId).apply()
                                listenToAssignments(cloudClassId)
                            }
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun joinClass(classIdInput: String, onResult: (Boolean, String?) -> Unit) {
        val cleanId = classIdInput.trim()
        if (cleanId.isEmpty()) {
            onResult(false, "班級代碼不能為空！")
            return
        }

        val db = firestore
        if (db == null) {
            studentClassId = cleanId
            sharedPrefs.edit().putString("firebase_class_id", cleanId).apply()
            listenToAssignments(cleanId)
            onResult(true, "已綁定班級代碼：$cleanId")
            return
        }

        ensureFirebaseAuth { uid ->
            val userData = hashMapOf(
                "name" to (currentUser?.name ?: "學生"),
                "role" to "student",
                "classId" to cleanId
            )

            try {
                db.collection("users").document(uid)
                    .set(userData, SetOptions.merge())
                    .addOnSuccessListener {
                        studentClassId = cleanId
                        sharedPrefs.edit().putString("firebase_class_id", cleanId).apply()
                        listenToAssignments(cleanId)
                        onResult(true, "已成功加入班級：$cleanId")
                    }
                    .addOnFailureListener { e ->
                        studentClassId = cleanId
                        sharedPrefs.edit().putString("firebase_class_id", cleanId).apply()
                        listenToAssignments(cleanId)
                        onResult(true, "已切換至班級：$cleanId")
                    }
            } catch (e: Exception) {
                studentClassId = cleanId
                sharedPrefs.edit().putString("firebase_class_id", cleanId).apply()
                listenToAssignments(cleanId)
                onResult(true, "已切換至班級：$cleanId")
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
