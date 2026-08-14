# 個人資料與使用情況牆（Personal Usage Wall）數據架構與邏輯文件

本文件詳細整理「個人中心」頁面中**滿版個人使用情況牆**與**編輯個人基本資料**的所有數據來源、儲存機制、計算公式及對應之程式碼段落。

---

## 📌 目錄
1. [架構概覽與數據流](#1-架構概覽與數據流)
2. [指標數據對照表](#2-指標數據對照表)
3. [核心資料表結構 (Entities)](#3-核心資料表結構-entities)
4. [核心邏輯與程式碼段落](#4-核心邏輯與程式碼段落)
   - [4.1 閱讀時間自動計時機制](#41-閱讀時間自動計時機制)
   - [4.2 目標閱讀時間設定與進度計算](#42-目標閱讀時間設定與進度計算)
   - [4.3 單字掌握率與刷卡統計](#43-單字掌握率與刷卡統計)
   - [4.4 歷史文章記錄流](#44-歷史文章記錄流)
   - [4.5 用戶暱稱編輯與更新](#45-用戶暱稱編輯與更新)

---

## 1. 架構概覽與數據流

```
[使用者互動 / 閱讀模式]
       │
       ▼
[VocabViewModel] ──(每 5 秒累加計時 / 狀態更新)──► [VocabRepository]
       │                                                 │
       ▼                                                 ▼
[StateFlow / LiveState] ◄───(Flow 即時響應收集)─── [Room SQLite Database]
       │                                            ├─ users (用戶時數與設定)
       ▼                                            ├─ vocab_words (單字與刷卡次數)
[ProfileScreen (Compose UI)]                        └─ read_articles (歷史閱讀紀錄)
 (滿版個人使用情況牆)
```

---

## 2. 指標數據對照表

| UI 介面指標名稱 | 計算邏輯 / 衍生公式 | 儲存位置 (Storage) | 預設值 |
| :--- | :--- | :--- | :--- |
| **🎯 目標閱讀時間** | `viewModel.dailyReadingGoalMinutes` | `SharedPreferences` (`"daily_reading_goal_mins"`) | `15` 分鐘 |
| **⌛ 當日已閱讀時間** | `user.totalUsageTimeSeconds / 60` 分 `user.totalUsageTimeSeconds % 60` 秒 | Room: `users` 表 (`totalUsageTimeSeconds`) | `0` 秒 |
| **📚 總學習字數** | `vocabList.size` | Room: `vocab_words` 表 | `0` 個 |
| **🌟 已熟練字數** | `vocabList.count { it.status == 1 }` | Room: `vocab_words` 表 (`status == 1`) | `0` 個 |
| **今日閱讀目標進度** | `(actualReadingMins / readingGoalMins) * 100%` | 即時動態運算 (`readingProgress`) | `0%` |
| **單字掌握率** | `(masteredCount * 100 / totalWordsCount)%` | 即時動態運算 (`masteryPercent`) | `0%` |
| **歷史文章** | `viewModel.readHistory.size` | Room: `read_articles` 表 | `0` 篇 |
| **累積刷卡** | `vocabList.sumOf { it.reviewCount }` | Room: `vocab_words` 表 (`reviewCount` 加總) | `0` 次 |
| **學習中生字** | `vocabList.count { it.status == 0 }` | Room: `vocab_words` 表 (`status == 0`) | `0` 個 |
| **✏️ 用戶暱稱** | `user.name` | Room: `users` 表 (`name`) | `單字美學家` |

---

## 3. 核心資料表結構 (Entities)

### (1) 用戶資訊表 (`app/src/main/java/com/example/data/User.kt`)
```kotlin
@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val name: String,
    val passwordHash: String = "",
    val avatarColorHex: String = "#6200EE",
    val authProvider: String = "CUSTOM", // "CUSTOM" or "GOOGLE"
    val registrationDate: Long = System.currentTimeMillis(),
    val vocabGoal: Int = 10,
    val preferredCategory: String = "technology",
    val vocabLevel: String = "PENDING",
    val role: String = "student",
    val loginCount: Int = 1,
    val totalUsageTimeSeconds: Long = 0 // 累計閱讀時數（秒）
)
```

### (2) 單字表 (`app/src/main/java/com/example/data/VocabDatabase.kt`)
```kotlin
@Entity(tableName = "vocab_words")
data class VocabWord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val translation: String,
    val definition: String = "",
    val phonetic: String = "",
    val category: String = "新聞常用",
    val exampleSentence: String = "",
    val exampleTranslation: String = "",
    val status: Int = 0,             // 0 = 學習中, 1 = 已熟練
    val reviewCount: Int = 0,        // 累積刷卡/複習次數
    val addedDate: Long = System.currentTimeMillis()
)
```

---

## 4. 核心邏輯與程式碼段落

### 4.1 閱讀時間自動計時機制
* **檔案路徑**：`app/src/main/java/com/example/viewmodel/VocabViewModel.kt`
* **運作邏輯**：ViewModel 初始化時啟動協程監聽。當使用者處於閱讀模式（`isReadingModeActive == true`）時，每 5 秒背景自動累加 5 秒並同步至 Room 資料庫。

```kotlin
// 位於 VocabViewModel.kt
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
```

---

### 4.2 目標閱讀時間設定與進度計算
* **設定與儲存** (`VocabViewModel.kt`)：
```kotlin
fun updateDailyReadingGoal(minutes: Int) {
    val validMins = minutes.coerceIn(5, 180) // 限制範圍 5 ~ 180 分鐘
    dailyReadingGoalMinutes = validMins
    sharedPrefs.edit().putInt("daily_reading_goal_mins", validMins).apply()
}
```

* **UI 進度百分比計算** (`ProfileScreen.kt`)：
```kotlin
val actualReadingMins = (user.totalUsageTimeSeconds / 60)
val actualReadingSecs = (user.totalUsageTimeSeconds % 60)
val readingGoalMins = viewModel.dailyReadingGoalMinutes

// 計算進度條比例 (0.0 ~ 1.0)
val readingProgress = if (readingGoalMins > 0) {
    (actualReadingMins.toFloat() / readingGoalMins.toFloat()).coerceIn(0f, 1f)
} else 0f

// 計算剩餘/超額時間
val remainingMins = (readingGoalMins - actualReadingMins).coerceAtLeast(0)
```

---

### 4.3 單字掌握率與刷卡統計
* **檔案路徑**：`app/src/main/java/com/example/ProfileScreen.kt` & `VocabViewModel.kt`
* **統計與掌握率計算** (`ProfileScreen.kt`)：
```kotlin
val totalWordsCount = vocabList.size
val masteredCount = remember(vocabList) { vocabList.count { it.status == 1 } }
val learningWords = remember(vocabList) { vocabList.count { it.status == 0 } }
val totalReviews = remember(vocabList) { vocabList.sumOf { it.reviewCount } }

val masteryPercent = if (totalWordsCount > 0) (masteredCount * 100 / totalWordsCount) else 0
```

* **刷卡累加與熟練度判定邏輯** (`VocabViewModel.kt`)：
```kotlin
fun markWordReviewed(wordId: Long, isMastered: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
        val existing = repository.getWordById(wordId) ?: return@launch
        val updatedCount = existing.reviewCount + 1
        // 刷卡達 5 次或使用者主動標記為熟練時，status 轉為 1 (已熟練)
        val newStatus = if (isMastered || updatedCount >= 5) 1 else 0
        val updated = existing.copy(reviewCount = updatedCount, status = newStatus)
        repository.updateWord(updated)
    }
}
```

---

### 4.4 歷史文章記錄流
* **檔案路徑**：`app/src/main/java/com/example/viewmodel/VocabViewModel.kt`
* **數據收集**：
```kotlin
val readHistory: StateFlow<List<com.example.data.ReadArticle>> = repository.allReadArticlesFlow
```
在 Profile 頁面直接透過 `readHistory.size` 取得閱讀文章總篇數。

---

### 4.5 用戶暱稱編輯與更新
* **UI 點擊儲存** (`ProfileScreen.kt`)：
```kotlin
Button(
    onClick = {
        viewModel.updateUserProfile(
            name = editName,
            vocabGoal = user.vocabGoal,
            preferredCategory = user.preferredCategory,
            avatarColorHex = user.avatarColorHex
        )
    },
    modifier = Modifier.fillMaxWidth()
) {
    Text("儲存名稱設定")
}
```

* **ViewModel 更新持久化** (`VocabViewModel.kt`)：
```kotlin
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
```
