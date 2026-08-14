package com.example.network

import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * 📰 RSS 新聞來源模式切換設定
 */
enum class NewsSourceProvider {
    RSS_FEED,       // 優先使用免費開放的 RSS Feeds
    GNEWS_API,      // 使用 GNews API
    HYBRID_AUTO     // 優先 RSS，若失敗自動降級到 GNews API
}

object NewsSourceConfig {
    // 預設切換為 RSS 優先模式（節省 API 費用，同時具備 GNews 與純 AI 雙重備援）
    var activeProvider: NewsSourceProvider = NewsSourceProvider.HYBRID_AUTO
}

data class RssHeadline(
    val title: String,
    val description: String,
    val link: String? = null,
    val sourceName: String = "RSS Feed",
    val pubDate: String? = null
)

object RssFeedService {
    private const val TAG = "RssFeedService"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // 依分類配置高可靠度、免 API Key 的主流外媒英文 RSS Feeds (包含備援來源)
    private val categoryFeeds: Map<String, List<Pair<String, String>>> = mapOf(
        "technology" to listOf(
            "BBC Tech" to "https://feeds.bbci.co.uk/news/technology/rss.xml",
            "NYT Tech" to "https://rss.nytimes.com/services/xml/rss/Technology.xml",
            "The Verge" to "https://www.theverge.com/rss/index.xml"
        ),
        "business" to listOf(
            "BBC Business" to "https://feeds.bbci.co.uk/news/business/rss.xml",
            "NYT Business" to "https://rss.nytimes.com/services/xml/rss/Business.xml",
            "CNBC Business" to "https://www.cnbc.com/id/10001147/device/rss/rss.html"
        ),
        "science" to listOf(
            "BBC Science" to "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml",
            "NYT Science" to "https://rss.nytimes.com/services/xml/rss/Science.xml",
            "Phys.org" to "https://phys.org/rss-feed/"
        ),
        "health" to listOf(
            "BBC Health" to "https://feeds.bbci.co.uk/news/health/rss.xml",
            "NYT Health" to "https://rss.nytimes.com/services/xml/rss/Health.xml",
            "NPR Health" to "https://feeds.npr.org/1128/rss.xml"
        ),
        "entertainment" to listOf(
            "BBC Entertainment" to "https://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml",
            "NYT Arts" to "https://rss.nytimes.com/services/xml/rss/Arts.xml",
            "Variety" to "https://variety.com/feed/"
        )
    )

    /**
     * 根據分類取得即時 RSS 英文新聞標題與摘要（自動輪詢備援 Feed）
     */
    suspend fun fetchHeadlines(category: String, maxCount: Int = 8): List<RssHeadline> = withContext(Dispatchers.IO) {
        val feeds = categoryFeeds[category.lowercase()] ?: categoryFeeds["technology"] ?: emptyList()
        
        for ((sourceName, feedUrl) in feeds) {
            try {
                Log.d(TAG, "Fetching RSS from $sourceName ($feedUrl) for category: $category")
                val request = Request.Builder()
                    .url(feedUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; VocabBrush/1.0; NewsReader)")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val xmlBody = response.body?.string()
                    if (!xmlBody.isNullOrBlank()) {
                        val parsed = parseRssXml(xmlBody, sourceName, maxCount)
                        if (parsed.isNotEmpty()) {
                            Log.d(TAG, "Successfully fetched ${parsed.size} articles from $sourceName")
                            return@withContext parsed
                        }
                    }
                } else {
                    Log.w(TAG, "HTTP error ${response.code} from $sourceName ($feedUrl)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse RSS from $sourceName ($feedUrl): ${e.message}")
            }
        }
        
        Log.e(TAG, "All RSS feeds failed for category: $category")
        emptyList()
    }

    /**
     * 使用 Android 原生 XmlPullParser 解析 RSS / Atom Feed XML
     */
    private fun parseRssXml(xml: String, sourceName: String, maxCount: Int): List<RssHeadline> {
        val headlines = mutableListOf<RssHeadline>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inItem = false
            var currentTitle: String? = null
            var currentDesc: String? = null
            var currentLink: String? = null
            var currentPubDate: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT && headlines.size < maxCount) {
                val tagName = parser.name?.lowercase()
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName == "item" || tagName == "entry") {
                            inItem = true
                            currentTitle = null
                            currentDesc = null
                            currentLink = null
                            currentPubDate = null
                        } else if (inItem) {
                            when (tagName) {
                                "title" -> currentTitle = cleanHtml(parser.nextText())
                                "description", "summary" -> currentDesc = cleanHtml(parser.nextText())
                                "link" -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    currentLink = if (!href.isNullOrBlank()) href else parser.nextText().trim()
                                }
                                "pubdate", "published", "updated" -> currentPubDate = parser.nextText().trim()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "item" || tagName == "entry") {
                            if (!currentTitle.isNullOrBlank()) {
                                headlines.add(
                                    RssHeadline(
                                        title = currentTitle,
                                        description = currentDesc ?: "",
                                        link = currentLink,
                                        sourceName = sourceName,
                                        pubDate = currentPubDate
                                    )
                                )
                            }
                            inItem = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing XML body: ${e.message}", e)
        }
        return headlines
    }

    private val HTML_TAG_PATTERN = Pattern.compile("<[^>]+>")

    private fun cleanHtml(html: String?): String {
        if (html == null) return ""
        val withoutTags = HTML_TAG_PATTERN.matcher(html).replaceAll("")
        return withoutTags
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}
