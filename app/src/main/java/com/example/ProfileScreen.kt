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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.data.User
import com.example.viewmodel.VocabViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(viewModel: VocabViewModel) {
    val user = viewModel.currentUser
    val vocabList by viewModel.vocabWords.collectAsState()
    val mContext = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dynamic banner
        Text(
            text = "個人會員中心",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (user == null) {
            AuthGateSection(viewModel = viewModel)
        } else {
            UserProfileDashboard(
                viewModel = viewModel,
                user = user,
                totalWordsCount = vocabList.size,
                masteredCount = vocabList.count { it.status == 1 }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AuthGateSection(viewModel: VocabViewModel) {
    var isLoginTab by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var showGoogleChooser by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_gate_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Branding Logo Banner
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(76.dp)
                    .padding(2.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_logo),
                        contentDescription = "Smart Brush Reader Logo",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Smart Brush Reader",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Header Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isLoginTab) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { isLoginTab = true }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "登入帳戶",
                        fontWeight = FontWeight.Bold,
                        color = if (isLoginTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (!isLoginTab) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { isLoginTab = false }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "自創帳冊",
                        fontWeight = FontWeight.Bold,
                        color = if (!isLoginTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isLoginTab) "歡迎登入智慧筆刷！" else "註冊一個屬於您的自訂帳戶",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // Notifications
            if (errorMsg != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (successMsg != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD4EDDA))
                ) {
                    Text(
                        text = successMsg ?: "",
                        color = Color(0xFF155724),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Email input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("電子郵件信箱") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email_field"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            if (!isLoginTab) {
                Spacer(modifier = Modifier.height(8.dp))
                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("用戶暱稱") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_name_field"),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("帳戶密碼") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Refresh else Icons.Default.Lock,
                            contentDescription = "密碼可見性"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password_field"),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Access Button
            Button(
                onClick = {
                    errorMsg = null
                    successMsg = null
                    if (isLoginTab) {
                        viewModel.loginUser(email, password) { ok, msg ->
                            if (ok) {
                                successMsg = "登入驗證成功！"
                            } else {
                                errorMsg = msg
                            }
                        }
                    } else {
                        viewModel.registerUser(email, name, password) { ok, msg ->
                            if (ok) {
                                successMsg = "自創帳戶建立完成！"
                            } else {
                                errorMsg = msg
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("auth_submit_btn"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isLoginTab) "登入智慧帳戶" else "建立自訂帳冊與密碼",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Third-party Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "快速安全認證途徑",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Google Login Button
            Button(
                onClick = { showGoogleChooser = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F2), contentColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, Color(0xFFDCDCDC), RoundedCornerShape(8.dp))
                    .testTag("google_auth_btn"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Custom representation of google colored icon
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color(0xFF4285F4), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("使用 Google 帳號授權登入", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }

    // Google Account Chooser Simulated Sheet Dialogue
    if (showGoogleChooser) {
        Dialog(onDismissRequest = { showGoogleChooser = false }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "選擇 Google 帳戶",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "以便繼續使用 智慧單字筆刷 服務並同步您的雲端個資學歷。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Account List option 1: Context account
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable {
                                showGoogleChooser = false
                                viewModel.loginWithGoogle("q0977271216@gmail.com", "極速英語通") { ok, msg ->
                                    if (!ok) errorMsg = msg
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF4285F4), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Q", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("極速英語通", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("q0977271216@gmail.com", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Simulated account 2
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable {
                                showGoogleChooser = false
                                viewModel.loginWithGoogle("vocab.brush.innovator@gmail.com", "單字美學家") { ok, msg ->
                                    if (!ok) errorMsg = msg
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF34A853), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("V", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("單字美學家 (預設測試)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("vocab.brush.innovator@gmail.com", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { showGoogleChooser = false }) {
                        Text("取消授權並返回", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun UserProfileDashboard(
    viewModel: VocabViewModel,
    user: User,
    totalWordsCount: Int,
    masteredCount: Int
) {
    val vocabList by viewModel.vocabWords.collectAsState()
    var editName by remember(user.name) { mutableStateOf(user.name) }
    var selectedCategory by remember(user.preferredCategory) { mutableStateOf(user.preferredCategory) }
    var selectedColorHex by remember(user.avatarColorHex) { mutableStateOf(user.avatarColorHex) }
    var targetWordsGoal by remember(user.vocabGoal) { mutableStateOf(user.vocabGoal.toFloat()) }

    val colorsPalette = listOf("#4285F4", "#6200EE", "#009688", "#E91E63", "#FF9800", "#3F51B5")

    // Profile Card Header Section
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Profile circular Avatar representation with dynamically chosen hex background color
            val bgParsedColor = try {
                Color(android.graphics.Color.parseColor(selectedColorHex))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(bgParsedColor, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = user.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = user.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Auth provider badge
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (user.authProvider == "GOOGLE") Color(0xFFE8F0FE) else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (user.authProvider == "GOOGLE") Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (user.authProvider == "GOOGLE") Color(0xFF1A73E8) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (user.authProvider == "GOOGLE") "Google 安全驗證關聯" else "自設密碼驗證帳戶",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (user.authProvider == "GOOGLE") Color(0xFF1A73E8) else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Level Badge Row
            val (levelLabel, levelColor) = when (user.vocabLevel) {
                "EASY" -> "初級學習者 (EASY)" to Color(0xFF2E7D32)
                "HARD" -> "進階挑戰者 (HARD)" to Color(0xFFC62828)
                else -> "中級提升者 (MEDIUM)" to Color(0xFFEF6C00)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = levelColor.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = levelColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "單字能力：$levelLabel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = levelColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "重新測驗",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable {
                            viewModel.submitVocabTestResult("PENDING")
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    var showGoalEditDialog by remember { mutableStateOf(false) }
    val readHistory by viewModel.readHistory.collectAsState()
    val totalReviews = remember(vocabList) { vocabList.sumOf { it.reviewCount } }
    val learningWords = remember(vocabList) { vocabList.count { it.status == 0 } }
    
    val actualReadingMins = (user.totalUsageTimeSeconds / 60)
    val actualReadingSecs = (user.totalUsageTimeSeconds % 60)
    val readingGoalMins = viewModel.dailyReadingGoalMinutes
    val readingProgress = if (readingGoalMins > 0) (actualReadingMins.toFloat() / readingGoalMins.toFloat()).coerceIn(0f, 1f) else 0f
    val masteryPercent = if (totalWordsCount > 0) (masteredCount * 100 / totalWordsCount) else 0

    // 🏆 滿版個人使用情況牆 (Full-Bleed Personal Usage Wall)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("personal_usage_wall")
            .onGloballyPositioned { coords ->
                viewModel.step4TargetRect = coords.boundsInWindow()
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🧱", fontSize = 18.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "滿版個人使用情況牆",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "即時反映您的全方位學習成果與每日閱讀時數",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (readingProgress >= 1f) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (readingProgress >= 1f) "🎉 今日已達標" else "進行中",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (readingProgress >= 1f) Color(0xFF15803D) else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🎯 4 大核心數據指標牆 (Four Core Metrics Cards)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Metric 1: 目標閱讀時間
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎯 目標閱讀時間", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "修改目標",
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { showGoalEditDialog = true }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$readingGoalMins 分鐘", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD97706))
                        }
                    }

                    // Metric 2: 當日已閱讀時間
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("⌛ 當日已閱讀時間", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (actualReadingMins > 0) "$actualReadingMins 分 $actualReadingSecs 秒" else "$actualReadingSecs 秒",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Metric 3: 總學習字數
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFCCFBF1)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF0D9488).copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("📚 總學習字數", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$totalWordsCount 個", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D9488))
                        }
                    }

                    // Metric 4: 已熟練字數
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4E6)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("🌟 已熟練字數", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBE123C))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$masteredCount 個", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE11D48))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ⏱️ 每日閱讀目標達成率進度條 (Daily Progress Bar)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "今日閱讀目標進度 (${(readingProgress * 100).toInt()}%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (readingProgress >= 1f) "🎉 已達標" else "還差 ${maxOf(0, readingGoalMins - actualReadingMins)} 分鐘",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (readingProgress >= 1f) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { readingProgress },
                        color = if (readingProgress >= 1f) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 📊 次要數據展現網格 (Secondary Stats Grid)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("單字掌握率", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$masteryPercent%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("歷史文章", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${readHistory.size} 篇", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("累積刷卡", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$totalReviews 次", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("學習中生字", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$learningWords 個", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 3. Restart Onboarding Tour CTA
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "新手教學與導覽",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "忘記功能如何使用？隨時重新閱讀聚光燈導覽！",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = { viewModel.restartOnboarding() },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("💡 重新導覽", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Goal Edit Dialog
    if (showGoalEditDialog) {
        var tempMinsText by remember { mutableStateOf(readingGoalMins.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalEditDialog = false },
            title = {
                Text("⏱️ 修改每日閱讀時間目標", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("請輸入預計每天閱讀英文的目標時間 (分鐘)：", fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 15, 20, 30).forEach { mins ->
                            FilterChip(
                                selected = tempMinsText == mins.toString(),
                                onClick = { tempMinsText = mins.toString() },
                                label = { Text("$mins 分鐘", fontSize = 12.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = tempMinsText,
                        onValueChange = { tempMinsText = it.filter { char -> char.isDigit() } },
                        label = { Text("自訂目標 (5 - 180 分鐘)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = tempMinsText.toIntOrNull() ?: 15
                        viewModel.updateDailyReadingGoal(mins)
                        showGoalEditDialog = false
                    }
                ) {
                    Text("儲存目標", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalEditDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Simplified User profile edit card (暱稱修改)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "✏️ 編輯個人基本資料",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Display Name
            OutlinedTextField(
                value = editName,
                onValueChange = { editName = it },
                label = { Text("用戶暱稱") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Save preferences button
            Button(
                onClick = {
                    viewModel.updateUserProfile(
                        name = editName,
                        vocabGoal = user.vocabGoal,
                        preferredCategory = user.preferredCategory,
                        avatarColorHex = user.avatarColorHex
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("儲存名稱設定")
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Log out button
    OutlinedButton(
        onClick = { viewModel.logoutUser() },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("logout_btn"),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("登出目前的會員帳戶 (" + user.name + ")", fontWeight = FontWeight.Bold)
        }
    }
}
