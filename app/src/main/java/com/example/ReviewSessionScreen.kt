package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.VocabDetail
import com.example.viewmodel.VocabViewModel
import kotlin.random.Random

enum class ReviewSubMode {
    FLASHCARD,
    QUIZ,
    SUMMARY
}

@Composable
fun ReviewSessionScreen(viewModel: VocabViewModel) {
    val words = viewModel.wordsToReview
    if (words.isEmpty()) {
        LaunchedEffect(Unit) {
            viewModel.isReviewModeActive = false
        }
        return
    }

    var subMode by remember { mutableStateOf(ReviewSubMode.FLASHCARD) }
    var currentIndex by remember { mutableStateOf(0) }
    
    // Flashcard stats
    var masteredInFlashcard by remember { mutableIntStateOf(0) }
    
    // Quiz stats & options
    var score by remember { mutableIntStateOf(0) }
    var selectedQuizOption by remember { mutableStateOf<Int?>(null) }
    var hasAnsweredQuiz by remember { mutableStateOf(false) }
    
    // Distractors list for each word in quiz
    val quizQuestions = remember(words) {
        generateQuizQuestions(words, viewModel.vocabWords.value.map { it.word to it.definition.substringAfter(".").substringBefore("-").trim() })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .testTag("review_session_container"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Review",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "本節閱讀生字複習 🖌️",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { viewModel.isReviewModeActive = false },
                    modifier = Modifier.testTag("review_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "結束複習",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tabs / Phase Progress Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReviewTabIndicator(
                    label = "1. 記憶閃卡",
                    isActive = subMode == ReviewSubMode.FLASHCARD,
                    isCompleted = subMode != ReviewSubMode.FLASHCARD,
                    modifier = Modifier.weight(1f)
                )
                ReviewTabIndicator(
                    label = "2. 複習挑戰",
                    isActive = subMode == ReviewSubMode.QUIZ,
                    isCompleted = subMode == ReviewSubMode.SUMMARY,
                    modifier = Modifier.weight(1f)
                )
            }

            // Active content area
            when (subMode) {
                ReviewSubMode.FLASHCARD -> {
                    val currentWord = words[currentIndex]
                    FlashcardSection(
                        word = currentWord,
                        currentIndex = currentIndex,
                        totalWords = words.size,
                        modifier = Modifier.weight(1f),
                        onNext = { isMastered ->
                            viewModel.recordWordReviewByText(currentWord.word)
                            if (isMastered) masteredInFlashcard++
                            if (currentIndex < words.size - 1) {
                                currentIndex++
                            } else {
                                // Transition to Quiz Mode!
                                currentIndex = 0
                                subMode = ReviewSubMode.QUIZ
                            }
                        }
                    )
                }
                ReviewSubMode.QUIZ -> {
                    val currentWord = words[currentIndex]
                    val currentQuestion = quizQuestions.getOrNull(currentIndex) ?: QuizQuestion(
                        word = currentWord,
                        options = listOf(currentWord.translation, "無", "空", "錯"),
                        correctIndex = 0
                    )

                    QuizSection(
                        question = currentQuestion,
                        currentIndex = currentIndex,
                        totalWords = words.size,
                        selectedOption = selectedQuizOption,
                        hasAnswered = hasAnsweredQuiz,
                        modifier = Modifier.weight(1f),
                        onSelectOption = { optionIdx ->
                            if (!hasAnsweredQuiz) {
                                selectedQuizOption = optionIdx
                                hasAnsweredQuiz = true
                                if (optionIdx == currentQuestion.correctIndex) {
                                    score++
                                }
                            }
                        },
                        onNext = {
                            viewModel.recordWordReviewByText(currentWord.word)
                            selectedQuizOption = null
                            hasAnsweredQuiz = false
                            if (currentIndex < words.size - 1) {
                                currentIndex++
                            } else {
                                subMode = ReviewSubMode.SUMMARY
                            }
                        }
                    )
                }
                ReviewSubMode.SUMMARY -> {
                    ReviewSummarySection(
                        totalWords = words.size,
                        masteredInFlashcard = masteredInFlashcard,
                        quizScore = score,
                        words = words,
                        modifier = Modifier.weight(1f),
                        onFinish = {
                            viewModel.isReviewModeActive = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewTabIndicator(
    label: String,
    isActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isActive -> MaterialTheme.colorScheme.primaryContainer
        isCompleted -> Color(0xFFE8F5E9)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }
    val textColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        isCompleted -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderModifier = if (isActive) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
    } else Modifier

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(borderModifier)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun FlashcardSection(
    word: VocabDetail,
    currentIndex: Int,
    totalWords: Int,
    modifier: Modifier = Modifier,
    onNext: (Boolean) -> Unit
) {
    var isFlipped by remember(word) { mutableStateOf(false) }
    
    // Flip rotation animation
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "CardFlipRotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Progress label
        Text(
            text = "生字記憶卡片 • ${currentIndex + 1} / $totalWords",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 3D Flip Card Container
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(1.2f)
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .clickable { isFlipped = !isFlipped }
                .graphicsLayer {
                    this.rotationY = rotationY
                    cameraDistance = 12f * density
                }
                .background(
                    Brush.verticalGradient(
                        colors = if (isFlipped) {
                            listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.surfaceVariant)
                        } else {
                            listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface)
                        }
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotationY <= 90f) {
                // Card FRONT
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = word.word,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    if (word.phonetic.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = word.phonetic,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (word.partOfSpeech.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text(word.partOfSpeech) }
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Flip",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "點擊卡片翻開譯文",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            } else {
                // Card BACK (rotated 180 deg to prevent mirrored rendering)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.rotationY = 180f
                        }
                ) {
                    Text(
                        text = word.translation,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = word.definition,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "點擊以翻回正面",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Action Buttons underneath
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onNext(false) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("needs_practice_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("需要再練習", fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = { onNext(true) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("mastered_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("我已記住", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QuizSection(
    question: QuizQuestion,
    currentIndex: Int,
    totalWords: Int,
    selectedOption: Int?,
    hasAnswered: Boolean,
    modifier: Modifier = Modifier,
    onSelectOption: (Int) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Progress label
        Text(
            text = "複習測驗挑戰 • ${currentIndex + 1} / $totalWords",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Question card
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "請問「${question.word.word}」的意思是？",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                if (question.word.phonetic.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "[ ${question.word.phonetic} ]",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Quiz Options List
        Column(
            modifier = Modifier.fillMaxWidth(0.95f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            question.options.forEachIndexed { idx, optionText ->
                val isSelected = selectedOption == idx
                val isCorrect = question.correctIndex == idx

                val containerColor = when {
                    hasAnswered && isCorrect -> Color(0xFFE8F5E9) // correct is green
                    hasAnswered && isSelected && !isCorrect -> Color(0xFFFFEBEE) // selected incorrect is red
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }

                val borderColor = when {
                    hasAnswered && isCorrect -> Color(0xFF2E7D32)
                    hasAnswered && isSelected && !isCorrect -> Color(0xFFC62828)
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outlineVariant
                }

                val contentColor = when {
                    hasAnswered && isCorrect -> Color(0xFF2E7D32)
                    hasAnswered && isSelected && !isCorrect -> Color(0xFFC62828)
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(containerColor)
                        .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable(enabled = !hasAnswered) { onSelectOption(idx) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Option letter
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ('A' + idx).toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = optionText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor,
                        modifier = Modifier.weight(1f)
                    )

                    if (hasAnswered) {
                        if (isCorrect) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Correct",
                                tint = Color(0xFF2E7D32)
                            )
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Incorrect",
                                tint = Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Next Question Button
        if (hasAnswered) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(48.dp)
                    .testTag("quiz_next_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (currentIndex < totalWords - 1) "下一題" else "看複習結算報告",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        } else {
            Spacer(modifier = Modifier.height(48.dp)) // Maintain layout height stability
        }
    }
}

@Composable
fun ReviewSummarySection(
    totalWords: Int,
    masteredInFlashcard: Int,
    quizScore: Int,
    words: List<VocabDetail>,
    modifier: Modifier = Modifier,
    onFinish: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Big Congratulations illustration placeholder
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
        }

        Text(
            text = "🎉 太棒了！複習完成！",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "您剛剛在閱讀時發現並標記了 $totalWords 個生字。以下是您的學習成果：",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Summary Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(0.95f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Flashcard mastered card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("閃卡記憶記住", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$masteredInFlashcard / $totalWords",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            // Quiz score card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("挑戰測驗答對", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$quizScore / $totalWords",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Summary word list
        Card(
            modifier = Modifier.fillMaxWidth(0.95f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📝 複習單字清單清單：",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                words.forEachIndexed { index, w ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}. ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = w.word,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${w.partOfSpeech}. ${w.translation}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (index < words.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Finish button
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(48.dp)
                .testTag("review_done_btn"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("完成複習並回到首頁", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// Helper structures & generators for Multiple Choice Quiz
data class QuizQuestion(
    val word: VocabDetail,
    val options: List<String>,
    val correctIndex: Int
)

fun generateQuizQuestions(
    words: List<VocabDetail>,
    existingVocabDbTranslations: List<Pair<String, String>>
): List<QuizQuestion> {
    val fallbackDistractorsList = listOf(
        "挑戰；懷疑", "創新的；革新的", "字彙；單字", "堅持不懈的；持續的", "效率高的；有能力的",
        "機會；時機", "完成；實現", "分析；解析", "有創造力的；實用的", "重要的；有意義的",
        "使人驚奇的；極好的", "大氣；氣氛", "精美的；敏銳的", "推廣；宣傳", "永續的；持續發展的",
        "便利的；省事的", "鼓舞人心的", "基礎設施；基本建設", "合作；協作", "適應；改編"
    )

    return words.map { targetWord ->
        // Accumulate a pool of potential distractor translation strings
        val distractorPool = (
            words.filter { !it.word.equals(targetWord.word, ignoreCase = true) }.map { it.translation } +
            existingVocabDbTranslations.filter { !it.first.equals(targetWord.word, ignoreCase = true) }.map { it.second } +
            fallbackDistractorsList
        ).filter { it.isNotBlank() && it != targetWord.translation }.distinct()

        // Randomly pick 3 distinct distractors from the pool
        val selectedDistractors = distractorPool.shuffled().take(3).toMutableList()
        while (selectedDistractors.size < 3) {
            selectedDistractors.add("單字翻譯選項 ${selectedDistractors.size + 1}")
        }

        // Shuffle correct option along with distractors
        val optionsList = mutableListOf(targetWord.translation)
        optionsList.addAll(selectedDistractors)
        optionsList.shuffle()

        QuizQuestion(
            word = targetWord,
            options = optionsList,
            correctIndex = optionsList.indexOf(targetWord.translation)
        )
    }
}
