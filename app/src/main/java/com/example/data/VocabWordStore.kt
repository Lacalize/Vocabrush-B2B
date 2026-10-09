package com.example.data

import android.content.Context
import com.example.network.VocabDetail
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * The one place a looked-up word gets written into the user's vocab book (Room + the shared
 * users/{uid}/vocabWords Firestore collection). Used by both the in-app brush flow
 * (VocabViewModel.saveWordToDb) and the system text-selection "Vocabrush 翻譯" popup
 * (QuickLookupActivity), so a word saved from either path behaves identically in review.
 */
object VocabWordStore {

    /** Must stay identical to slugifyKey() in Vocabrush-PWA/firebase-service.js. */
    fun slugifyKey(text: String): String {
        val slug = text.trim().lowercase().replace(Regex("[^a-z0-9\\u4e00-\\u9fff]+"), "_").trim('_')
        return if (slug.isBlank()) "w_${kotlin.math.abs(text.hashCode())}" else slug.take(150)
    }

    fun isFailedTranslation(detail: VocabDetail): Boolean =
        detail.translation.contains("連線失敗") ||
            detail.translation.contains("請先設定金鑰") ||
            detail.translation.contains("發生異常")

    fun vocabWordToMap(word: VocabWord): Map<String, Any> = mapOf(
        "word" to word.word,
        "definition" to word.definition,
        "contextSentence" to word.contextSentence,
        "timestamp" to word.timestamp,
        "status" to word.status,
        "sourceArticle" to word.sourceArticle,
        "reviewCount" to word.reviewCount
    )

    /** Pushes a word up to Firestore - only for a real (non-anonymous) signed-in account. */
    fun syncUp(word: VocabWord, auth: FirebaseAuth?, firestore: FirebaseFirestore?) {
        val user = auth?.currentUser ?: return
        if (user.isAnonymous) return
        firestore?.collection("users")?.document(user.uid)?.collection("vocabWords")
            ?.document(slugifyKey(word.word))
            ?.set(vocabWordToMap(word), SetOptions.merge())
    }

    /**
     * Inserts the word, or - if it's already in the book - bumps its review count (mastered at 5)
     * and refreshes its definition/context. Call from a background dispatcher.
     */
    suspend fun upsertLookedUpWord(
        context: Context,
        detail: VocabDetail,
        sentence: String,
        sourceTitle: String,
        auth: FirebaseAuth?,
        firestore: FirebaseFirestore?
    ): VocabWord? {
        if (isFailedTranslation(detail)) return null
        val db = VocabDatabase.getDatabase(context.applicationContext)
        val repository = VocabRepository(db.vocabDao(), db.userDao())
        val definition = "${detail.partOfSpeech}. ${detail.translation} - ${detail.definition}"

        val existing = repository.getWordByText(detail.word)
        val saved = if (existing == null) {
            VocabWord(
                word = detail.word,
                definition = definition,
                contextSentence = sentence,
                status = 0,
                sourceArticle = sourceTitle,
                reviewCount = 1
            ).also { repository.insert(it) }
        } else {
            val newCount = existing.reviewCount + 1
            existing.copy(
                definition = definition,
                contextSentence = sentence,
                timestamp = System.currentTimeMillis(),
                sourceArticle = sourceTitle,
                reviewCount = newCount,
                status = if (newCount >= 5) 1 else existing.status
            ).also { repository.update(it) }
        }
        syncUp(saved, auth, firestore)
        return saved
    }
}
