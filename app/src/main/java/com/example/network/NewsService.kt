package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class NewsSource(
    @Json(name = "id") val id: String?,
    @Json(name = "name") val name: String?
)

@JsonClass(generateAdapter = true)
data class NewsArticle(
    @Json(name = "source") val source: NewsSource? = null,
    @Json(name = "author") val author: String? = null,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "urlToImage") val urlToImage: String? = null,
    @Json(name = "image") val image: String? = null, // added for GNews
    @Json(name = "publishedAt") val publishedAt: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "contentEasy") val contentEasy: String? = null,
    @Json(name = "contentMedium") val contentMedium: String? = null,
    @Json(name = "contentHard") val contentHard: String? = null
)

@JsonClass(generateAdapter = true)
data class NewsResponse(
    @Json(name = "status") val status: String?, // Nullable as GNews v4 doesn't provide status
    @Json(name = "totalResults") val totalResults: Int?,
    @Json(name = "totalArticles") val totalArticles: Int?, // GNews total articles
    @Json(name = "articles") val articles: List<NewsArticle>?
)

interface NewsApi {
    @GET
    suspend fun fetchNewsByUrl(@Url url: String): NewsResponse
}

object NewsClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: NewsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://saurav.tech/NewsAPI/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(NewsApi::class.java)
    }
}
