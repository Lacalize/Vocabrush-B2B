package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vocab_words")
data class VocabWord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val definition: String = "",
    val contextSentence: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: Int = 0, // 0 = learning, 1 = mastered
    val sourceArticle: String = "未分類",
    val reviewCount: Int = 0
)

@Entity(tableName = "read_history")
data class ReadArticle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val lastReadTime: Long = System.currentTimeMillis(),
    val lastReadPage: Int = 0
)

@Entity(tableName = "cached_news")
data class CachedNews(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val title: String,
    val description: String,
    val contentEasy: String,
    val contentMedium: String,
    val contentHard: String,
    val dateString: String, // format "YYYY-MM-DD"
    val author: String = "AI Gemini",
    val publishedAt: String = "Today"
)

@Entity(tableName = "cloud_cached_news")
data class CloudCachedNews(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val title: String,
    val description: String,
    val contentEasy: String,
    val contentMedium: String,
    val contentHard: String,
    val dateString: String, // format "YYYY-MM-DD"
    val author: String = "AI Gemini",
    val publishedAt: String = "Today"
)

@Dao
interface VocabDao {
    @Query("SELECT * FROM vocab_words ORDER BY timestamp DESC")
    fun getAllWordsFlow(): Flow<List<VocabWord>>

    @Query("SELECT * FROM vocab_words ORDER BY timestamp DESC")
    suspend fun getAllWords(): List<VocabWord>

    @Query("SELECT * FROM vocab_words WHERE word = :word LIMIT 1")
    suspend fun getWordByText(word: String): VocabWord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: VocabWord): Long

    @Update
    suspend fun updateWord(word: VocabWord)

    @Query("DELETE FROM vocab_words WHERE id = :id")
    suspend fun deleteWordById(id: Int)
    
    @Query("SELECT COUNT(*) FROM vocab_words")
    suspend fun getWordCount(): Int

    // Read history queries
    @Query("SELECT * FROM read_history ORDER BY lastReadTime DESC")
    fun getAllReadArticlesFlow(): Flow<List<ReadArticle>>

    @Query("SELECT * FROM read_history ORDER BY lastReadTime DESC")
    suspend fun getAllReadArticles(): List<ReadArticle>

    @Query("SELECT * FROM read_history WHERE title = :title LIMIT 1")
    suspend fun getReadArticleByTitle(title: String): ReadArticle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadArticle(article: ReadArticle): Long

    @Query("DELETE FROM read_history WHERE id = :id")
    suspend fun deleteReadArticleById(id: Int)

    // Cached news queries
    @Query("SELECT * FROM cached_news WHERE category = :category AND dateString = :dateString")
    suspend fun getCachedNewsByCategoryAndDate(category: String, dateString: String): List<CachedNews>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedNews(news: CachedNews): Long

    @Query("DELETE FROM cached_news WHERE dateString != :dateString")
    suspend fun deleteOldCachedNews(dateString: String)

    // Cloud cached news queries (Simulated Cloud DB)
    @Query("SELECT * FROM cloud_cached_news WHERE category = :category AND dateString = :dateString")
    suspend fun getCloudCachedNewsByCategoryAndDate(category: String, dateString: String): List<CloudCachedNews>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCloudCachedNews(news: CloudCachedNews): Long

    @Query("DELETE FROM cloud_cached_news WHERE dateString != :dateString")
    suspend fun deleteOldCloudCachedNews(dateString: String)
}

@Database(entities = [VocabWord::class, ReadArticle::class, CachedNews::class, CloudCachedNews::class, User::class], version = 10, exportSchema = false)
abstract class VocabDatabase : RoomDatabase() {
    abstract fun vocabDao(): VocabDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: VocabDatabase? = null

        fun getDatabase(context: Context): VocabDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VocabDatabase::class.java,
                    "vocab_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
