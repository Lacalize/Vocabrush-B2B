package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.VocabViewModel

data class TestQuestion(
    val id: Int,
    val word: String,
    val sentence: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val difficulty: String
)

val questions = listOf(
    TestQuestion(
        id = 1,
        word = "frequently",
        sentence = "He frequently visits the library to read novels.",
        options = listOf("Often (常常)", "Never (從不)", "Slowly (緩慢地)", "Sadly (悲傷地)"),
        correctAnswerIndex = 0,
        difficulty = "EASY"
    ),
    TestQuestion(
        id = 2,
        word = "generous",
        sentence = "She is generous and always willing to share her food with others.",
        options = listOf("Selfish (自私的)", "Kind and giving (大方的)", "Very lazy (懶惰的)", "Quiet (安靜的)"),
        correctAnswerIndex = 1,
        difficulty = "EASY"
    ),
    TestQuestion(
        id = 3,
        word = "annual",
        sentence = "We celebrate our annual family reunion in July.",
        options = listOf("Daily (每日的)", "Weekly (每週的)", "Yearly (每年的)", "Monthly (每月的)"),
        correctAnswerIndex = 2,
        difficulty = "EASY"
    ),
    TestQuestion(
        id = 4,
        word = "abandon",
        sentence = "The captain ordered the sailors to abandon the sinking ship.",
        options = listOf("Leave completely (放棄/離棄)", "Build (建造)", "Repair (修理)", "Control (控制)"),
        correctAnswerIndex = 0,
        difficulty = "MEDIUM"
    ),
    TestQuestion(
        id = 5,
        word = "diligent",
        sentence = "A diligent student usually reviews lessons and gets good grades.",
        options = listOf("Hard-working (勤奮的)", "Lazy (懶惰的)", "Wealthy (富有的)", "Sleepy (想睡的)"),
        correctAnswerIndex = 0,
        difficulty = "MEDIUM"
    ),
    TestQuestion(
        id = 6,
        word = "essential",
        sentence = "Fresh water is essential for the survival of all living things.",
        options = listOf("Optional (可有可無的)", "Dangerous (危險的)", "Extremely important (不可或缺的)", "Harmful (有害的)"),
        correctAnswerIndex = 2,
        difficulty = "MEDIUM"
    ),
    TestQuestion(
        id = 7,
        word = "benevolent",
        sentence = "The benevolent businessman donated millions to the children's hospital.",
        options = listOf("Greedy (貪婪的)", "Cruel (殘忍的)", "Kind and charitable (仁慈的/慈善的)", "Arrogant (傲慢的)"),
        correctAnswerIndex = 2,
        difficulty = "HARD"
    ),
    TestQuestion(
        id = 8,
        word = "scrutinize",
        sentence = "Customs officers scrutinize all passports very carefully.",
        options = listOf("Examine closely (仔細檢查)", "Ignore (忽略)", "Copy (複製)", "Lose (丟失)"),
        correctAnswerIndex = 0,
        difficulty = "HARD"
    ),
    TestQuestion(
        id = 9,
        word = "capricious",
        sentence = "The weather here is capricious; it can rain suddenly on a sunny afternoon.",
        options = listOf("Unpredictable (變化無常的)", "Stable and predictable (穩定的)", "Beautiful (美麗的)", "Cold (寒冷的)"),
        correctAnswerIndex = 0,
        difficulty = "HARD"
    ),
    TestQuestion(
        id = 10,
        word = "adversity",
        sentence = "He showed great courage and optimism in the face of adversity.",
        options = listOf("Wealth (財富)", "Good luck (好運)", "Hardship or difficulty (逆境/困境)", "Friendship (友誼)"),
        correctAnswerIndex = 2,
        difficulty = "HARD"
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VocabCapabilityTestScreen(viewModel: VocabViewModel) {
    var currentIndex by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<Int, Int>() }
    var isSubmitted by remember { mutableStateOf(false) }
    
    val currentQuestion = questions[currentIndex]
    val totalQuestions = questions.size

    val score = remember(isSubmitted) {
        if (isSubmitted) {
            questions.count { q ->
                answers[q.id - 1] == q.correctAnswerIndex
            }
        } else 0
    }

    val assignedLevel = remember(score) {
        when {
            score <= 4 -> "EASY"
            score in 5..7 -> "MEDIUM"
            else -> "HARD"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(16.dp)
    ) {
        if (!isSubmitted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Diagnostic Test Header & Logo
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "英語字彙能力診斷",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "為了給您量身定做適合的文章難度，請完成這 10 題診斷。我們將自動調整您的每日 AI 時事。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "進度：第 ${currentIndex + 1} / $totalQuestions 題",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "已答：${answers.size} / $totalQuestions 題",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / totalQuestions.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Question Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("placement_test_question_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(currentQuestion.difficulty) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = when (currentQuestion.difficulty) {
                                        "EASY" -> Color(0xFFE8F5E9)
                                        "MEDIUM" -> Color(0xFFFFF3E0)
                                        else -> Color(0xFFFFEBEE)
                                    },
                                    labelColor = when (currentQuestion.difficulty) {
                                        "EASY" -> Color(0xFF2E7D32)
                                        "MEDIUM" -> Color(0xFFEF6C00)
                                        else -> Color(0xFFC62828)
                                    }
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "單字：${currentQuestion.word}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Context sentence
                        Text(
                            text = "例句上下文：",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentQuestion.sentence,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Choices Section
                Text(
                    text = "請選擇最適合的單字解釋：",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                currentQuestion.options.forEachIndexed { optIndex, optText ->
                    val isSelected = answers[currentIndex] == optIndex
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(
                                1.5.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { answers[currentIndex] = optIndex },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { answers[currentIndex] = optIndex }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = optText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Navigation Row (Previous, Next, Submit)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("上一題")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    if (currentIndex < totalQuestions - 1) {
                        Button(
                            onClick = { currentIndex++ },
                            enabled = answers[currentIndex] != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("下一題")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    } else {
                        Button(
                            onClick = { isSubmitted = true },
                            enabled = answers.size == totalQuestions,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("送出分析能力")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Cancel or Log out
                OutlinedButton(
                    onClick = { viewModel.logoutUser() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("取消並登出帳戶", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            // Placement test submitted, show result card
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(45.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "測驗判定完成！",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "答對題數：$score / $totalQuestions 題",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val (levelTitle, levelDesc, levelColor) = when (assignedLevel) {
                            "EASY" -> Triple(
                                "初級學習者 (EASY)",
                                "適合日常入門、常用基礎字彙及簡明文法。我們已為您準備了難度適中的時事文章，讓您輕鬆閱讀無壓力！",
                                Color(0xFF2E7D32)
                            )
                            "MEDIUM" -> Triple(
                                "中級提升者 (MEDIUM)",
                                "適合具備基礎、希望擴展字彙深度的讀者。我們已為您準備了流暢且富含實用片語的時事教材，助您穩健提升！",
                                Color(0xFFEF6C00)
                            )
                            else -> Triple(
                                "進階挑戰者 (HARD)",
                                "適合具備良好學歷、欲挑戰外媒社論深度的讀者。我們將提供最道地、字彙極具挑戰性的時事文章！",
                                Color(0xFFC62828)
                            )
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = levelColor.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, levelColor.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🎯 判定等級：$levelTitle",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = levelColor,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = levelDesc,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                viewModel.submitVocabTestResult(assignedLevel)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("unlock_news_hub_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                "解鎖我的智慧筆刷時事之旅！ 🚀",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = { isSubmitted = false }
                        ) {
                            Text("重新作答")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
