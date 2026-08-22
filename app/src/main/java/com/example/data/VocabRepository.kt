package com.example.data

import kotlinx.coroutines.flow.Flow

class VocabRepository(
    private val vocabDao: VocabDao,
    private val userDao: UserDao
) {
    val allWordsFlow: Flow<List<VocabWord>> = vocabDao.getAllWordsFlow()

    suspend fun getAllWords(): List<VocabWord> = vocabDao.getAllWords()

    suspend fun getWordByText(word: String): VocabWord? = vocabDao.getWordByText(word)

    suspend fun insert(word: VocabWord): Long = vocabDao.insertWord(word)

    suspend fun update(word: VocabWord) = vocabDao.updateWord(word)

    suspend fun deleteById(id: Int) = vocabDao.deleteWordById(id)

    // Read history operations
    val allReadArticlesFlow: Flow<List<ReadArticle>> = vocabDao.getAllReadArticlesFlow()

    suspend fun getAllReadArticles(): List<ReadArticle> = vocabDao.getAllReadArticles()

    suspend fun getReadArticleByTitle(title: String): ReadArticle? = vocabDao.getReadArticleByTitle(title)

    suspend fun insertReadArticle(article: ReadArticle): Long = vocabDao.insertReadArticle(article)

    suspend fun deleteReadArticleById(id: Int) = vocabDao.deleteReadArticleById(id)

    // Cached news operations
    suspend fun getCachedNewsByCategoryAndDate(category: String, dateString: String): List<CachedNews> =
        vocabDao.getCachedNewsByCategoryAndDate(category, dateString)

    suspend fun insertCachedNews(news: CachedNews): Long =
        vocabDao.insertCachedNews(news)

    suspend fun deleteOldCachedNews(dateString: String) =
        vocabDao.deleteOldCachedNews(dateString)

    // Cloud cached news operations
    suspend fun getCloudCachedNewsByCategoryAndDate(category: String, dateString: String): List<CloudCachedNews> =
        vocabDao.getCloudCachedNewsByCategoryAndDate(category, dateString)

    suspend fun insertCloudCachedNews(news: CloudCachedNews): Long =
        vocabDao.insertCloudCachedNews(news)

    suspend fun deleteOldCloudCachedNews(dateString: String) =
        vocabDao.deleteOldCloudCachedNews(dateString)

    // User operations
    suspend fun getUserByUid(uid: String): User? = userDao.getUserByUid(uid)
    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)
    suspend fun insertUser(user: User): Long = userDao.insertUser(user)
    suspend fun updateUser(user: User) = userDao.updateUser(user)
    suspend fun getUserCount(): Int = userDao.getUserCount()
    suspend fun getAllUsers(): List<User> = userDao.getAllUsers()
}
