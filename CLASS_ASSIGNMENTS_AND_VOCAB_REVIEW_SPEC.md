# 班級作業系統與智慧生字庫複習架構規格書 (Class Assignments & Vocab Review Spec)

本文件專為 Agent 及後端/前端工程師設計，詳盡規範本應用程式中「**班級代碼與作業收發系統**」以及「**智慧生字庫與多階段複習機制**」的資料契約、資料庫設計、核心業務邏輯與流程規範。

---

## 📌 目錄
1. [架構概覽與資料分層](#1-架構概覽與資料分層)
2. [模組一：班級代碼與作業收發系統](#2-模組一班級代碼與作業收發系統)
   - 2.1 Firestore 資料路徑與實體定義
   - 2.2 學生端綁定/加入班級流程 (`joinClass`)
   - 2.3 即時作業長連接監聽與派發 (`listenToAssignments`)
   - 2.4 作業派發與閱讀器重點單字聯動
3. [模組二：智慧生字庫與複習引擎](#3-模組二智慧生字庫與複習引擎)
   - 3.1 Room 本地資料庫實體 (`VocabWord`)
   - 3.2 閱讀即時加入生字庫與智慧計數升級
   - 3.3 生字庫狀態管理（學習中 vs 已掌握）
   - 3.4 雙階段複習流程（記憶閃卡 + 動態測驗）
4. [核心檔案與函式對照表](#4-核心檔案與函式對照表)

---

## 1. 架構概覽與資料分層

```
┌────────────────────────────────────────────────────────┐
│                   使用者介面 (Compose UI)                │
│   [ClassAssignmentsSection]      [VocabBook / Review]   │
└───────────────▲──────────────────────────▲─────────────┘
                │                          │
┌───────────────┴──────────────────────────┴─────────────┐
│                 VocabViewModel (狀態與邏輯層)             │
│   - assignmentUiState (StateFlow)                      │
│   - vocabWords (StateFlow)                             │
└───────────────▲──────────────────────────▲─────────────┘
                │                          │
   (雲端即時同步 Firestore)           (本地離線持久化 Room SQLite)
┌───────────────┴───────────────┐ ┌────────┴──────────────┐
│       Google Firestore        │ │     Android Room DB   │
│ - users/{uid}                 │ │ - vocab_words         │
│ - classes/{classId}/assignments│ │ - read_articles       │
└───────────────────────────────┘ └───────────────────────┘
```

---

## 2. 模組一：班級代碼與作業收發系統

### 2.1 Firestore 資料路徑與實體定義

#### A. 資料路徑
* **用戶班級狀態**：`users/{uid}` (包含欄位 `classId: String`, `role: "student"`, `name: String`)
* **班級作業子集合**：`classes/{classCode}/assignments/{assignmentId}`

#### B. 資料模型 (`app/src/main/java/com/example/data/Assignment.kt`)
```kotlin
package com.example.data

import com.google.firebase.Timestamp

data class Assignment(
    val id: String = "",                            // Firestore Document ID
    val classId: String = "",                       // 所屬班級代碼 (如: "ENG101")
    val title: String = "",                         // 作業文章標題
    val content: String = "",                       // 英文作業文章內容
    val targetWordList: List<String> = emptyList(), // 本篇作業之重點/考核生字清單
    val dueDate: Timestamp? = null,                 // 繳交截止時間
    val createdAt: Timestamp? = null                // 派發時間
)
```

---

### 2.2 學生端綁定/加入班級流程 (`joinClass`)
1. **輸入清理**：去除首尾空白，確認非空。
2. **本機保存**：寫入 `SharedPreferences` (`"firebase_class_id"`)，確保離線或 App 重啟後自動載入。
3. **雲端綁定**：透過 `FirebaseAuth` 取得學生 UID，將 `classId` 及基本資料以 `SetOptions.merge()` 寫入 Firestore `users/{uid}`。
4. **觸發監聽**：呼叫 `listenToAssignments(classId)` 啟動長連接。

```kotlin
// 實作參考：VocabViewModel.kt
fun joinClass(classIdInput: String, onResult: (Boolean, String?) -> Unit) {
    val cleanId = classIdInput.trim()
    if (cleanId.isEmpty()) {
        onResult(false, "班級代碼不能為空！")
        return
    }
    
    // 1. 本機偏好設定存儲
    studentClassId = cleanId
    sharedPrefs.edit().putString("firebase_class_id", cleanId).apply()
    
    // 2. 雲端同步
    val db = firestore
    if (db == null) {
        listenToAssignments(cleanId)
        onResult(true, "已綁定班級代碼：$cleanId (離線模式)")
        return
    }

    ensureFirebaseAuth { uid ->
        val userData = hashMapOf(
            "name" to (currentUser?.name ?: "學生"),
            "role" to "student",
            "classId" to cleanId
        )
        db.collection("users").document(uid)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                listenToAssignments(cleanId)
                onResult(true, "已成功加入班級：$cleanId")
            }
            .addOnFailureListener { e ->
                onResult(false, "加入失敗：${e.localizedMessage}")
            }
    }
}
```

---

### 2.3 即時作業長連接監聽與派發 (`listenToAssignments`)
* **狀態機設計 (`AssignmentUiState`)**：`Idle` ➔ `Loading` ➔ `Success(List<Assignment>)` / `Error(message)`
* **監聽機制**：建立 `addSnapshotListener` 監聽 `classes/{classCode}/assignments`，依 `createdAt` 降序排列。當老師端新增或修改作業時，學生端 UI 自動即時響應，無須手動下拉刷新。

```kotlin
// 實作參考：VocabViewModel.kt
fun listenToAssignments(classId: String) {
    val cleanClassId = classId.trim()
    if (cleanClassId.isBlank()) return
    val db = firestore ?: return
    
    assignmentListenerRegistration?.remove() // 移除舊監聽避免洩漏
    _assignmentUiState.value = AssignmentUiState.Loading

    val query = db.collection("classes")
        .document(cleanClassId)
        .collection("assignments")
        .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)

    assignmentListenerRegistration = query.addSnapshotListener { snapshot, error ->
        if (error != null) {
            _assignmentUiState.value = AssignmentUiState.Error("作業同步失敗：${error.localizedMessage}")
            return@addSnapshotListener
        }
        if (snapshot != null) {
            val list = snapshot.documents.mapNotNull { doc ->
                Assignment(
                    id = doc.id,
                    classId = cleanClassId,
                    title = doc.getString("title") ?: "未命名作業",
                    content = doc.getString("content") ?: "",
                    targetWordList = (doc.get("targetWordList") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                    dueDate = doc.getTimestamp("dueDate"),
                    createdAt = doc.getTimestamp("createdAt")
                )
            }.sortedByDescending { it.createdAt?.seconds ?: 0L }

            _assignmentUiState.value = AssignmentUiState.Success(list)
        }
    }
}
```

---

### 2.4 作業派發與閱讀器重點單字聯動
* 學生點擊作業項目時，呼叫 `openArticle("🏫 班級作業 | ${assignment.title}", assignment.content, assignment.targetWordList)`。
* 閱讀器接收 `targetWordList`，於文章內文以專屬強調色彩高亮顯示老師指定的重點單字，點擊即可立即查閱字典並加入生字庫。

---

## 3. 模組二：智慧生字庫與複習引擎

### 3.1 Room 本地資料庫實體 (`VocabWord`)
* **資料表名**：`vocab_words`
* **檔案路徑**：`app/src/main/java/com/example/data/VocabDatabase.kt`

```kotlin
@Entity(tableName = "vocab_words")
data class VocabWord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,                    // 單字原文 (Unique key 邏輯)
    val definition: String = "",         // 詞性、翻譯與中文釋義
    val contextSentence: String = "",    // 擷取時之文章情境例句
    val phonetic: String = "",           // 國際音標
    val category: String = "新聞常用",     // 來源類別
    val status: Int = 0,                 // 學習狀態：0 = 學習中 (Learning), 1 = 已熟練 (Mastered)
    val reviewCount: Int = 0,            // 累計複習/刷卡次數
    val sourceArticle: String = "",      // 出處文章標題
    val timestamp: Long = System.currentTimeMillis()
)
```

---

### 3.2 閱讀即時加入生字庫與智慧計數升級

當使用者在閱讀器點選單字並點擊「加入生字庫」時，觸發 `addWordFromReading`：
* **新單字**：建立全新實體，`status = 0`，初始 `reviewCount = 1`。
* **已存在單字**：更新為最新的情境例句，`reviewCount` 自動累加 1。
* **5 次自動熟練機制**：當 `reviewCount >= 5` 時，系統自動判定該字已熟練，將 `status` 切換為 `1`。

```kotlin
// 實作參考：VocabViewModel.kt
fun addWordFromReading(detail: VocabDetail, sentence: String, documentTitle: String) {
    viewModelScope.launch(Dispatchers.IO) {
        val existing = repository.getWordByText(detail.word)
        if (existing == null) {
            val newWord = VocabWord(
                word = detail.word,
                definition = "${detail.partOfSpeech}. ${detail.translation} - ${detail.definition}",
                contextSentence = sentence,
                phonetic = detail.phonetic,
                status = 0,
                sourceArticle = documentTitle,
                reviewCount = 1
            )
            repository.insert(newWord)
        } else {
            val newCount = existing.reviewCount + 1
            val newStatus = if (newCount >= 5) 1 else existing.status
            val updated = existing.copy(
                definition = "${detail.partOfSpeech}. ${detail.translation} - ${detail.definition}",
                contextSentence = sentence,
                phonetic = detail.phonetic.ifBlank { existing.phonetic },
                timestamp = System.currentTimeMillis(),
                reviewCount = newCount,
                status = newStatus
            )
            repository.update(updated)
        }
    }
}
```

---

### 3.3 生字庫狀態管理與篩選 (`VocabBookScreen`)
1. **即時響應流**：透過 `repository.allWordsFlow` 轉為 `StateFlow<List<VocabWord>>`。
2. **三向分類過濾**：
   - **全部 (All)**：`vocabList.size`
   - **學習中 (Learning)**：`vocabList.filter { it.status == 0 }`
   - **已掌握 (Mastered)**：`vocabList.filter { it.status == 1 }`
3. **即時狀態手動切換 (`toggleWordMastered`)**：
   使用者可點擊星星圖示直接將單字手動切換 `status`（0 ⇄ 1）。

---

### 3.4 雙階段複習流程 (`ReviewSessionScreen`)

複習流程依序分為兩大階段與最終結算：

```
[點擊開始複習] ──(預設提取 status == 0 優先)
       │
       ▼
【階段 1：雙面記憶閃卡 (Flashcards)】
  ├─ 正面：單字、音標、情境例句 (隱藏釋義)
  ├─ 點擊翻轉：顯示中文翻譯、詞性與詳細解釋
  └─ 操作：「標記已掌握」或「下一個」
       └─ 觸發 `recordWordReviewByText` (reviewCount + 1, 達 5 次自動 mastered)
       │
       ▼ (所有閃卡完成後自動切入)
【階段 2：即時複習測驗 (Quiz Challenge)】
  ├─ 系統從複習清單中動態生成「4 選 1 中文意思單選題」
  ├─ 題庫干擾選項：自動抽取生字庫內其他單字之釋義做為隨機干擾項
  └─ 即時提供答題回饋 (綠色正確 / 紅色錯誤與解析)
       │
       ▼
【階段 3：複習成果結算 (Summary Dashboard)】
  └─ 顯示本次複習單字數、掌握個數、測驗正確率及激勵勳章
```

---

## 4. 核心檔案與函式對照表

| 功能模組 | 檔案路徑 | 核心類別 / 關鍵函式 |
| :--- | :--- | :--- |
| **作業資料結構** | `app/src/main/java/com/example/data/Assignment.kt` | `data class Assignment` |
| **作業 UI 呈現與派發** | `app/src/main/java/com/example/ui/ClassAssignmentsSection.kt` | `ClassAssignmentsSection()` |
| **班級綁定與長連接** | `app/src/main/java/com/example/viewmodel/VocabViewModel.kt` | `joinClass()`, `listenToAssignments()` |
| **生字庫 Room Entity & DAO** | `app/src/main/java/com/example/data/VocabDatabase.kt` | `VocabWord`, `VocabDao` |
| **生字庫 Repository** | `app/src/main/java/com/example/data/VocabRepository.kt` | `allWordsFlow`, `insert()`, `update()` |
| **生字庫 UI (列表/過濾/搜尋)** | `app/src/main/java/com/example/MainActivity.kt` | `VocabBookScreen()`, `VocabWordCard()` |
| **生字新增與自動熟練邏輯** | `app/src/main/java/com/example/viewmodel/VocabViewModel.kt` | `addWordFromReading()`, `recordWordReviewByText()` |
| **雙階段複習介面 (閃卡+測驗)** | `app/src/main/java/com/example/ReviewSessionScreen.kt` | `ReviewSessionScreen()`, `FlashcardSection()`, `QuizSection()` |
