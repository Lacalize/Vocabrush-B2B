# AI 時事文章生成規格與使用者快取共用機制說明書 (Article Generation & Cloud Cache Spec)

本文件提供給後端/Agent 開發者，作為理解、維護與擴展本應用程式中「**AI 英語時事文章生成**」以及「**多層快取與用戶共享機制（Cache-Aside Pattern）**」的標準規範。

---

## 📌 目錄
1. [文章生成規格 (Generation Specifications)](#1-文章生成規格-generation-specifications)
   - 1.1 呼叫模型與版本 (Model & Endpoint)
   - 1.2 難度版本規範 (CEFR Multi-Level Grading)
   - 1.3 生成件數與長度 (Quantity & Word Count)
   - 1.4 支援主題種類 (Categories)
2. [快取與用戶共用架構 (Multi-Tier Caching & Cloud Sharing)](#2-快取與用戶共用架構-multi-tier-caching--cloud-sharing)
   - 2.1 三層快取查詢優先序 (L1 -> L2 -> L3)
   - 2.2 雲端跨用戶共享設計 (Firestore Community Articles)
   - 2.3 快取寫入與過期清理機制 (TTL & Eviction)
3. [資料結構與 Prompt 範例 (Data Schema & Gemini Prompt)](#3-資料結構與-prompt-範例-data-schema--gemini-prompt)
   - 3.1 JSON Output Schema
   - 3.2 Firestore Document 欄位結構
   - 3.3 Prompt 模板結構
4. [核心程式碼定位與函數對照 (Code Reference)](#4-核心程式碼定位與函數對照-code-reference)

---

## 1. 文章生成規格 (Generation Specifications)

### 1.1 呼叫模型與版本 (Model & Endpoint)
* **目前採用模型**：`gemini-3.5-flash-lite`（成本極低、反應速度快且支援 JSON 結構化輸出）。
* **API 代理端點**：
  * 主要路徑：`https://vocab-news-api.q0977271216-b8f.workers.dev/v1beta/models/gemini-3.5-flash-lite:generateContent`
  * 官方備援：`https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key={GEMINI_API_KEY}`
* **請求配置 (GenerationConfig)**：
  * `responseMimeType`: `"application/json"`
  * `temperature`: `0.7`

### 1.2 難度版本規範 (CEFR Multi-Level Grading)
每篇主題文章均由 Gemini 同步產出 **3 種不同難度** 的英文全文版本，以適應不同英文程度的學習者：

| 難度等級 | 欄位名稱 (`JSON Key`) | 對應 CEFR 等級 | 字彙與語法規範 | 目標字數 |
| :--- | :--- | :--- | :--- | :--- |
| **初級 (Easy)** | `contentEasy` | **A2 - B1** | 簡化日常英文、短句結構、常用高頻單字 | 150 ~ 200 字 |
| **中級 (Medium)** | `contentMedium` | **B2** | 標準外媒英文、自然生活與職場詞彙、中等複合句 | 200 ~ 250 字 |
| **高級 (Hard)** | `contentHard` | **C1 - C2** | 進階書面英文、豐富專業修辭與複雜句型 | 250 ~ 300 字 |

### 1.3 生成件數與長度 (Quantity & Word Count)
* **每次產出件數**：固定為 **5 篇** 獨立時事文章（包含 Captivating Headline 與 1~2 句簡述）。
* **資料來源 (三軌備援機制)**：
  1. **首選 (即時 RSS Feeds 聚合 - 0 費用)**：透過 `RssFeedService` 直接解析 BBC、NYT、The Verge、CNBC、Phys.org 等主流外媒 RSS XML，即時抓取最新頭條與摘要，完全不消耗第三方新聞 API 費用。
  2. **次選 (GNews API 備援)**：當 RSS 來源受限或連線異常時，自動切換至 GNews API。
  3. **三選 (純 AI 生成備援)**：若網路聚合皆未取得，由 Gemini 針對該分類直接構思 5 個教育性時事主題進行創作。

### 1.4 支援主題種類 (Categories)
系統支援以下 5 大類別新聞主題（支援中英文 Mapping）：

| 分類名稱 | 代碼 (`category`) | 涵蓋領域範例 |
| :--- | :--- | :--- |
| **科技科技 (Technology)** | `technology` | 人工智慧 (AI)、太空探索、綠能科技、量子運算、軟硬體發明 |
| **商業財經 (Business)** | `business` | 全球市場脈動、新創融資、各國經濟政策、企業管理與趨勢 |
| **科學探索 (Science)** | `science` | 天文物理、生物醫學、環境氣候變遷、海洋研究、前沿科學 |
| **健康生活 (Health)** | `health` | 心理健康、營養學、運動生理、現代醫學突破、生活習慣改善 |
| **休閒娛樂 (Entertainment)** | `entertainment` | 影視文化、音樂藝術、體育盛事、數位娛樂與電競趨勢 |

---

## 2. 快取與用戶共用架構 (Multi-Tier Caching & Cloud Sharing)

為了**大幅降低 Gemini API 呼叫次數與費用**，同時提升文章秒開的體驗，系統採用了 **Cache-Aside + 雲端共享架構**：

```
[使用者選擇新聞分類 (例如 Technology)]
                     │
                     ▼
  【第 1 層：本機 Local SQLite 快取】  ──(命中 Hit)──► [直接呈現 5 篇完整文章 (0 延遲)]
                     │ (未命中 Miss)
                     ▼
  【第 2 層：Firestore 雲端共享快取】  ──(命中 Hit)──► [回存本機 SQLite] ──► [直接呈現 (0 Token 消耗)]
                     │ (未命中 Miss)
                     ▼
  【第 3 層：呼叫 Gemini AI 進行生成】 ──► [產生 5 篇 x 3 難度版本]
                                                   │
                                                   ├─► [寫入本機 SQLite 快取]
                                                   └─► [寫入 Firestore 共享池 (供全體用戶讀取)]
```

### 2.1 三層快取查詢優先序 (L1 -> L2 -> L3)
1. **L1 (本機 SQLite Cache - `cached_news` 表)**：
   * 查詢鍵值：`category` + `dateString` (格式 `yyyy-MM-dd`)。
   * 優點：裝置本地秒開，無需網路連線與 Token 消耗。
2. **L2 (雲端全域共享 Cache - Firestore `community_articles` 集合)**：
   * 查詢鍵值：`whereEqualTo("category", category).whereEqualTo("dateString", dateString)`。
   * 邏輯：只要**同日任何一位用戶**曾點擊並生成過該類別的新聞，文章便會被快取在雲端；其他所有用戶當天點擊該類別時，直接從雲端讀取，**完全不需重複呼叫 Gemini API**。
3. **L3 (Gemini 3.5 Flash-Lite API 實時生成)**：
   * 僅在 L1 與 L2 皆無資料時（當天第一位訪問該類別的使用者）觸發。

### 2.2 雲端跨用戶共享設計 (Firestore Community Articles)
* **Firestore Collection 名稱**：`community_articles`
* **Document ID 命名規則**：`{category}_{dateString}_{index}`（如 `technology_2026-08-14_0` 至 `technology_2026-08-14_4`）。
* **寫入策略**：使用 `SetOptions.merge()` 確保併發寫入時冪等不覆蓋錯誤。

### 2.3 快取寫入與過期清理機制 (TTL & Eviction)
* **有效週期 (TTL)**：以 **自然日（Date String: `yyyy-MM-dd`）** 為週期，每日換日後自動更新。
* **本機空間維護**：在執行新文章寫入前，系統會自動調用 `deleteOldCachedNews(todayString)` 清除過往日期的舊快取，避免佔用手機儲存空間。

---

## 3. 資料結構與 Prompt 範例 (Data Schema & Gemini Prompt)

### 3.1 JSON Output Schema
Gemini 接收 System/User Prompt 後，必須輸出以下結構的 Raw JSON Array（嚴禁 Markdown 標記）：

```json
[
  {
    "title": "Captivating Headline in English",
    "description": "A brief overview in English (1-2 sentences).",
    "contentEasy": "Simplified English full text (150-200 words, A2-B1 level)...",
    "contentMedium": "Standard English full text (200-250 words, B2 level)...",
    "contentHard": "Advanced English full text (250-300 words, C1-C2 level)..."
  }
]
```

### 3.2 Firestore Document 欄位結構 (`community_articles`)
```json
{
  "category": "technology",
  "dateString": "2026-08-14",
  "title": "NASA Unveils Next-Generation Clean Propulsion",
  "description": "NASA researchers have announced a breakthrough in ion thrusters for deep space missions.",
  "contentEasy": "NASA scientists have created a new engine...",
  "contentMedium": "Researchers at NASA have developed an innovative propulsion system...",
  "contentHard": "In a momentous breakthrough for astronautics, NASA engineers have unveiled...",
  "author": "AI Gemini",
  "publishedAt": "Just now by Gemini",
  "createdAt": 1786674208000
}
```

### 3.3 Prompt 模板精要 (Prompt Structure)
```text
Please select 5 current, hot, and highly relevant educational topics in the category "{category}".
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

Do not output any markdown formatting, backticks, or "```json". Just return the raw JSON array.
```

---

## 4. 核心程式碼定位與函數對照 (Code Reference)

| 模組功能 | 檔案路徑 | 關鍵方法 / 變數 |
| :--- | :--- | :--- |
| **RSS 即時外媒新聞抓取 (免 API Key)** | `app/src/main/java/com/example/network/RssFeedService.kt` | `RssFeedService.fetchHeadlines(category)` |
| **文章抓取與快取調度** | `app/src/main/java/com/example/viewmodel/VocabViewModel.kt` | `fetchCategoryNews(category: String)` |
| **GNews API 舊版/備援網路客戶端** | `app/src/main/java/com/example/network/NewsService.kt` | `NewsClient.api.fetchNewsByUrl(url)` |
| **Gemini 提示詞建構與呼叫** | `app/src/main/java/com/example/viewmodel/VocabViewModel.kt` | `generateArticlesFromHeadlines()` |
| **Firestore 雲端快取讀取** | `app/src/main/java/com/example/viewmodel/VocabViewModel.kt` | `getFirestoreCloudCachedNews(category, dateString)` |
| **Firestore 雲端快取發布** | `app/src/main/java/com/example/viewmodel/VocabViewModel.kt` | `saveToFirestoreCommunityArticles(...)` |
| **本機 SQLite 快取 DAO** | `app/src/main/java/com/example/data/VocabDatabase.kt` | `CachedNewsDao` (`insertCachedNews`, `getCachedNewsByCategoryAndDate`) |
| **新聞資料實體結構** | `app/src/main/java/com/example/network/NewsService.kt` | `data class NewsArticle` |
| **閱讀難度切換邏輯** | `app/src/main/java/com/example/viewmodel/VocabViewModel.kt` | `selectNewsArticle(article: NewsArticle)` |
