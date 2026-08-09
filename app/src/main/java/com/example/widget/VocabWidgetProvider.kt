package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.View
import android.widget.RemoteViews
import com.example.R
import com.example.MainActivity
import com.example.data.VocabDatabase
import com.example.data.VocabWord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

class VocabWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Build widget view for each active ID
        for (appWidgetId in appWidgetIds) {
            loadNewQuestion(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // Try to find if any action applies to all widgets
            return
        }

        val prefs = context.getSharedPreferences("vocab_widget_prefs", Context.MODE_PRIVATE)

        when (action) {
            ACTION_ANSWER -> {
                val selectedIdx = intent.getIntExtra(EXTRA_SELECTED_INDEX, -1)
                val correctIdx = prefs.getInt("correct_idx_$appWidgetId", -1)
                val isAnswered = prefs.getBoolean("answered_$appWidgetId", false)

                if (!isAnswered && selectedIdx != -1 && correctIdx != -1) {
                    // Save state as answered
                    prefs.edit().putBoolean("answered_$appWidgetId", true).apply()

                    val word = prefs.getString("word_$appWidgetId", "") ?: ""
                    val correctTranslation = prefs.getString("correct_trans_$appWidgetId", "") ?: ""
                    
                    // Increment review count for the word in local DB & auto-mark as mastered if count >= 5
                    if (word.isNotBlank()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val db = VocabDatabase.getDatabase(context)
                            val existing = db.vocabDao().getWordByText(word)
                            if (existing != null) {
                                val newCount = existing.reviewCount + 1
                                val newStatus = if (newCount >= 5) 1 else existing.status
                                db.vocabDao().updateWord(existing.copy(reviewCount = newCount, status = newStatus))
                            }
                        }
                    }

                    val isCorrect = selectedIdx == correctIdx
                    val feedbackText = if (isCorrect) {
                        "🎉 答對了！恭喜！"
                    } else {
                        "😢 答錯了～「$word」的意思是：\n$correctTranslation"
                    }

                    // Refactor RemoteViews to show feedback state
                    val views = RemoteViews(context.packageName, R.layout.vocab_widget_layout)
                    views.setTextViewText(R.id.widget_word, word)
                    views.setViewVisibility(R.id.quiz_choices_container, View.GONE)
                    views.setViewVisibility(R.id.feedback_container, View.VISIBLE)
                    views.setTextViewText(R.id.feedback_text, feedbackText)
                    
                    // Highlight selected index visually if needed (simply showing correction is cleaner)
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    
                    // Setup click for Next Button in feedback
                    val nextIntent = Intent(context, VocabWidgetProvider::class.java).apply {
                        this.action = ACTION_NEXT
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    val nextPendingIntent = PendingIntent.getBroadcast(
                        context,
                        appWidgetId + 500,
                        nextIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_next, nextPendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
            ACTION_NEXT -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                loadNewQuestion(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun loadNewQuestion(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            // Fetch words from database
            val db = VocabDatabase.getDatabase(context)
            val dbWords = db.vocabDao().getAllWords()
            
            // Generate list of possible questions by padding actual database entries with fallback classics
            val availableChoices = mutableListOf<QuizWord>()
            for (w in dbWords) {
                availableChoices.add(QuizWord(w.word, w.definition))
            }
            
            // Add high-frequency fallbacks to ensure there are always choices
            val fallbacks = listOf(
                QuizWord("challenge", "挑戰；懷疑"),
                QuizWord("innovative", "創新的；革新的"),
                QuizWord("vocabulary", "字彙；單字"),
                QuizWord("persistent", "堅持不懈的；持續的"),
                QuizWord("efficient", "效率高的；有能力的"),
                QuizWord("opportunity", "機會；時機"),
                QuizWord("accomplish", "完成；實現"),
                QuizWord("analyze", "分析；解析"),
                QuizWord("creative", "有創造力的；實用的"),
                QuizWord("significant", "重要的；有意義的")
            )
            
            for (f in fallbacks) {
                if (availableChoices.none { it.word.lowercase() == f.word.lowercase() }) {
                    availableChoices.add(f)
                }
            }

            // Pick a target word random
            val targetIndex = Random.nextInt(availableChoices.size)
            val target = availableChoices[targetIndex]

            // Pick 3 distractor words
            val otherWords = availableChoices.filter { it.word != target.word }
            val distractors = otherWords.shuffled().take(3).toMutableList()
            while (distractors.size < 3) {
                distractors.add(QuizWord("placeholder", "無關的單字解釋"))
            }

            // Shuffle target options
            val quizOptions = mutableListOf<QuizWord>()
            quizOptions.add(target)
            quizOptions.addAll(distractors)
            quizOptions.shuffle()

            val correctIndex = quizOptions.indexOf(target)

            // Save variables to SharedPreferences
            val prefs = context.getSharedPreferences("vocab_widget_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("word_$appWidgetId", target.word)
                putString("correct_trans_$appWidgetId", target.translation)
                putInt("correct_idx_$appWidgetId", correctIndex)
                putBoolean("answered_$appWidgetId", false)
                apply()
            }

            // Construct views
            val views = RemoteViews(context.packageName, R.layout.vocab_widget_layout)
            views.setTextViewText(R.id.widget_word, target.word)
            views.setViewVisibility(R.id.quiz_choices_container, View.VISIBLE)
            views.setViewVisibility(R.id.feedback_container, View.GONE)

            val optionViewIds = listOf(R.id.option_a, R.id.option_b, R.id.option_c, R.id.option_d)
            val labels = listOf("A", "B", "C", "D")

            for (i in 0..3) {
                val optionText = "${labels[i]}. ${quizOptions[i].translation}"
                views.setTextViewText(optionViewIds[i], optionText)

                // Setup PendingIntent for option click
                val answerIntent = Intent(context, VocabWidgetProvider::class.java).apply {
                    this.action = ACTION_ANSWER
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(EXTRA_SELECTED_INDEX, i)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + i,
                    answerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(optionViewIds[i], pendingIntent)
            }

            // Setting up general widget click to open Main Activity
            val openActivityIntent = Intent(context, MainActivity::class.java)
            val openActivityPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                openActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, openActivityPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private data class QuizWord(val word: String, val translation: String)

    companion object {
        const val ACTION_ANSWER = "com.example.vocabreader.ACTION_ANSWER"
        const val ACTION_NEXT = "com.example.vocabreader.ACTION_NEXT"
        const val EXTRA_SELECTED_INDEX = "extra_selected_index"
    }
}
