package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.coroutineScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Slider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.VocabWord
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.ClassAssignmentsSection
import com.example.update.ForceUpdateDialog
import com.example.update.ForceUpdateManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.TranslationState
import com.example.viewmodel.VocabViewModel

class MainActivity : ComponentActivity() {
    private lateinit var forceUpdateManager: ForceUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize Firebase Remote Config Force Update System
        forceUpdateManager = ForceUpdateManager(this)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val updateInfo by forceUpdateManager.updateInfo.collectAsStateWithLifecycle()

                if (updateInfo.isUpdateRequired) {
                    ForceUpdateDialog(
                        apkUrl = updateInfo.apkUrl,
                        onUpdateClick = { url ->
                            forceUpdateManager.launchUpdateIntent(this@MainActivity, url)
                        }
                    )
                }

                VocabBrushApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::forceUpdateManager.isInitialized) {
            forceUpdateManager.fetchAndActivate()
        }
    }
}

@Composable
fun VocabBrushApp() {
    val viewModel: VocabViewModel = viewModel()
    val vocabList by viewModel.vocabWords.collectAsState()
    val translationState by viewModel.translationState.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        bottomBar = {
            val user = viewModel.currentUser
            // Only show bottom navigation bar when user is logged in and not in pending placement test
            if (user != null && user.vocabLevel != "PENDING") {
                if (!viewModel.isReadingModeActive) { // Hide the bottom tab menu completely when active reading mode is turned on for immersive feel!
                    NavigationBar(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        NavigationBarItem(
                            selected = viewModel.currentTab == 0,
                            onClick = { 
                                viewModel.currentTab = 0 
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "閱讀器") },
                            label = { Text("筆刷閱讀器") },
                            modifier = Modifier.testTag("tab_reader")
                        )
                        NavigationBarItem(
                            selected = viewModel.currentTab == 1,
                            onClick = { viewModel.currentTab = 1 },
                            icon = { Icon(Icons.Default.Star, contentDescription = "生字庫") },
                            label = { Text("智慧生字庫") },
                            modifier = Modifier
                                .testTag("tab_book")
                                .onGloballyPositioned { coords ->
                                    viewModel.step3TargetRect = coords.boundsInWindow()
                                }
                        )
                        NavigationBarItem(
                            selected = viewModel.currentTab == 2,
                            onClick = { viewModel.currentTab = 2 },
                            icon = { Icon(Icons.Default.Person, contentDescription = "個人中心") },
                            label = { Text("個人中心") },
                            modifier = Modifier
                                .testTag("tab_profile")
                                .onGloballyPositioned { coords ->
                                    viewModel.step4TargetRect = coords.boundsInWindow()
                                }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                )
        ) {
            val user = viewModel.currentUser
            if (user == null) {
                // Step 1: Login / Register Screen for first-time / non-authenticated users
                ProfileScreen(viewModel = viewModel)
            } else if (user.vocabLevel == "PENDING") {
                // Step 2: Vocabulary Capability / Placement Test Screen
                VocabCapabilityTestScreen(viewModel = viewModel)
            } else {
                // Step 4: Main Application Pages (Reader, Vocab Book, Profile Dashboard)
                when (viewModel.currentTab) {
                    0 -> {
                        if (viewModel.isReadingModeActive) {
                            androidx.compose.runtime.key(viewModel.documentTitle) {
                                ReaderScreen(viewModel = viewModel)
                            }
                        } else {
                            ArticleHubScreen(viewModel = viewModel)
                        }
                    }
                    1 -> VocabBookScreen(viewModel = viewModel, vocabList = vocabList)
                    2 -> ProfileScreen(viewModel = viewModel)
                }
            }

            if (viewModel.isReviewModeActive) {
                ReviewSessionScreen(viewModel = viewModel)
            }

            // Step 3: Spotlight Onboarding Tour & Goal Setting Overlay (shown after placement test)
            if (viewModel.isDailyInitLoading) {
                DailyInitLoadingScreen(viewModel = viewModel)
            } else if (viewModel.showOnboardingOverlay) {
                OnboardingSpotlightOverlay(viewModel = viewModel)
            }

            // Global floating bottom translucent card for AI translates
            AnimatedVisibility(
                visible = viewModel.showTranslationCard,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .widthIn(max = 600.dp)
            ) {
                TranslationSheet(
                    state = translationState,
                    onDismiss = { viewModel.clearTranslationState() }
                )
            }
        }
    }
}

private fun checkStrokeRectOverlap(rect: Rect, points: List<Offset>): Boolean {
    if (points.isEmpty()) return false
    val inflated = rect.inflate(18f)
    if (points.any { inflated.contains(it) }) return true
    for (i in 0 until points.size - 1) {
        val p1 = points[i]
        val p2 = points[i + 1]
        val mid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
        if (inflated.contains(mid)) return true
        val q1 = Offset(p1.x * 0.75f + p2.x * 0.25f, p1.y * 0.75f + p2.y * 0.25f)
        if (inflated.contains(q1)) return true
        val q3 = Offset(p1.x * 0.25f + p2.x * 0.75f, p1.y * 0.25f + p2.y * 0.75f)
        if (inflated.contains(q3)) return true
    }
    return false
}

@OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(viewModel: VocabViewModel) {
    var isBrushMode by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Drag parameters
    val strokePoints = remember { mutableStateListOf<Offset>() }
    val wordCoordinatesMap = remember { mutableStateMapOf<Int, LayoutCoordinates>() }
    var parentCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    // Split text into pages of a clean, readable length (180 words per page)
    val paragraphs = remember(viewModel.documentText) {
        chunkTextIntoPages(viewModel.documentText, 180)
    }

    // Precalculate word index offsets for each page paragraph
    val pageWordOffsets = remember(paragraphs) {
        val offsets = IntArray(paragraphs.size)
        var currentOffset = 0
        for (i in paragraphs.indices) {
            offsets[i] = currentOffset
            val wordsCount = paragraphs[i].split(Regex("\\s+")).filter { it.isNotBlank() }.size
            currentOffset += wordsCount
        }
        offsets
    }

    var showConfigPanel by remember { mutableStateOf(false) }
    var showHelpTip by remember { mutableStateOf(false) } // Hidden by default for an immersive, clean reading screen

    // Horizontal Pager State loaded with initial page
    val pagerState = rememberPagerState(
        initialPage = viewModel.initialPageToLoad.coerceIn(0, maxOf(0, paragraphs.size - 1)),
        pageCount = { paragraphs.size }
    )

    // Clear strokes/bounds on page turn to keep coordinate states pristine, and save reading progress
    LaunchedEffect(pagerState.currentPage) {
        strokePoints.clear()
        wordCoordinatesMap.clear()
        viewModel.savePageProgress(viewModel.documentTitle, pagerState.currentPage)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 2.dp, bottom = 2.dp, start = 2.dp, end = 2.dp)
    ) {
        // Top Document Action Bar (Spacious and elegant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { viewModel.exitReadingMode() },
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(50))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回文章清單",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = viewModel.documentTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "雙指或左滑翻頁 📖 智慧筆刷學時事 🖌️",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Toggle Help Tips Button
            IconButton(onClick = { showHelpTip = !showHelpTip }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "操作提示",
                    tint = if (showHelpTip) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Custom Configuration Panel Trigger
            IconButton(onClick = { showConfigPanel = !showConfigPanel }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "字型與配置",
                    tint = if (showConfigPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalButton(
                isHighlighted = isBrushMode,
                onClick = { isBrushMode = !isBrushMode },
                text = if (isBrushMode) "刷 🖌️" else "讀 📖"
            )
        }

        // Collapsible Reading Layout Config Drawer Row
        AnimatedVisibility(visible = showConfigPanel) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "⚙️ 閱讀個人化配置",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "字體大小: ${viewModel.readerFontSize.toInt()}sp",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(100.dp)
                        )
                        
                        Button(
                            onClick = { if (viewModel.readerFontSize > 14f) viewModel.readerFontSize -= 1f },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("-", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        
                        Slider(
                            value = viewModel.readerFontSize,
                            onValueChange = { viewModel.readerFontSize = it },
                            valueRange = 14f..32f,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Button(
                            onClick = { if (viewModel.readerFontSize < 32f) viewModel.readerFontSize += 1f },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Guide text based on state, wrapped in AnimatedVisibility to keep screen 100% immersive by default
        AnimatedVisibility(visible = showHelpTip) {
            Column {
                Text(
                    text = if (isBrushMode) "💡 筆刷模式：手指在英文單字上「塗抹黃色螢光」，放開後 Gemini 將自動分析！(翻頁請按底部按鈕)" else "💡 探查模式：左滑、右滑即可像讀實體書般「翻頁」，點擊單字可隨手查詢釋義！",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Book-Style Page Turning Horizontal Pager
        // Disable swipe scroll in brush mode to avoid gesture conflicts, allowing normal swipe page-turning in read mode
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            userScrollEnabled = !isBrushMode
        ) { pageIndex ->
            val para = paragraphs[pageIndex]
            val wordOffset = pageWordOffsets[pageIndex]

            // Premium paper textured card container
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp, horizontal = 2.dp)
                    .shadow(3.dp, shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFAF2)), // Soft elegant cream book paper tone
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFEFECE6))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Page Header (Book Title & Page indicator)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📖 PAGE ${pageIndex + 1} OF ${paragraphs.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isBrushMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isBrushMode) "筆刷模式" else "翻頁/閱讀",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBrushMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF2EFE9)))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Inner drawing & reading canvas bounds
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .onGloballyPositioned { parentCoordinates = it }
                            .then(
                                if (isBrushMode) {
                                    Modifier.pointerInput(para) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                strokePoints.clear()
                                                strokePoints.add(down.position)

                                                var pointerId = down.id

                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                                    if (!change.pressed) {
                                                        break
                                                    }
                                                    val currentPos = change.position
                                                    if (strokePoints.isEmpty() || (currentPos - strokePoints.last()).getDistance() > 2f) {
                                                        strokePoints.add(currentPos)
                                                        change.consume()
                                                    }
                                                }

                                                // Pointer released - process brush stroke or tap point
                                                val parentCoords = parentCoordinates
                                                val detectedIndexes = mutableListOf<Int>()
                                                if (parentCoords != null && parentCoords.isAttached && strokePoints.isNotEmpty()) {
                                                    wordCoordinatesMap.forEach { (index, layoutCoords) ->
                                                        if (layoutCoords.isAttached) {
                                                            val localRect = parentCoords.localBoundingBoxOf(layoutCoords)
                                                            if (checkStrokeRectOverlap(localRect, strokePoints)) {
                                                                detectedIndexes.add(index)
                                                            }
                                                        }
                                                    }
                                                }

                                                val sorted = detectedIndexes.sorted()
                                                if (sorted.isNotEmpty()) {
                                                    val parentWordTokens = reconstructAllWords(paragraphs)
                                                    val firstToken = parentWordTokens.getOrNull(sorted.first())
                                                    val lastToken = parentWordTokens.getOrNull(sorted.last())

                                                    if (firstToken != null && lastToken != null) {
                                                        val span = sorted.last() - sorted.first()
                                                        val rawTargetText = if (span > 0 && span <= 4 && firstToken.paragraph == lastToken.paragraph) {
                                                            (sorted.first()..sorted.last()).mapNotNull { idx ->
                                                                parentWordTokens.getOrNull(idx)?.word
                                                            }.joinToString(" ")
                                                        } else {
                                                            firstToken.word
                                                        }

                                                        val cleanedTargetText = rawTargetText.trim().replace(Regex("^[^a-zA-Z0-9]+|[^a-zA-Z0-9]+$"), "")
                                                        val targetText = if (cleanedTargetText.isNotBlank()) cleanedTargetText else rawTargetText
                                                        viewModel.translateWord(targetText, firstToken.paragraph)
                                                    }
                                                }
                                                strokePoints.clear()
                                            }
                                        }
                                    }
                                } else Modifier
                            )
                    ) {
                        // Render words of the current page's paragraphs elegantly with double-newline line-breaks
                        val subParagraphs = para.split("\n")
                        var wordInPageIdx = 0
                        val targetWordsList = viewModel.assignmentTargetWords

                        // Hoisted unconditionally so the scroll position survives toggling isBrushMode
                        val pageScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(pageScrollState, enabled = !isBrushMode)
                        ) {
                            subParagraphs.forEach { subPara ->
                                val rawWords = subPara.split(Regex("\\s+")).filter { it.isNotBlank() }
                                if (rawWords.isNotEmpty()) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        rawWords.forEach { rawWord ->
                                            val currentIdx = wordOffset + wordInPageIdx
                                            val cleanedWord = rawWord.trim().replace(Regex("[^a-zA-Z-]"), "")
                                            val isTargetWord = remember(cleanedWord, targetWordsList) {
                                                cleanedWord.isNotBlank() && targetWordsList.any { target ->
                                                    target.trim().equals(cleanedWord, ignoreCase = true)
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 4.dp, vertical = 3.dp)
                                                    .background(
                                                        color = if (isTargetWord) Color(0xFFFEF3C7) else Color.Transparent,
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .border(
                                                        width = if (isTargetWord) 1.dp else 0.dp,
                                                        color = if (isTargetWord) Color(0xFFF59E0B) else Color.Transparent,
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = if (isTargetWord) 4.dp else 0.dp, vertical = if (isTargetWord) 2.dp else 0.dp)
                                                    .onGloballyPositioned { layoutCoordinates ->
                                                        if (layoutCoordinates.isAttached) {
                                                            wordCoordinatesMap[currentIdx] = layoutCoordinates
                                                        }
                                                    }
                                                    .then(
                                                        if (cleanedWord.isNotBlank()) {
                                                            Modifier.clickable {
                                                                viewModel.translateWord(cleanedWord, subPara)
                                                            }
                                                        } else Modifier
                                                    )
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    if (isTargetWord) {
                                                        Text("🎯", fontSize = (viewModel.readerFontSize * 0.75).sp)
                                                    }
                                                    Text(
                                                        text = rawWord,
                                                        fontSize = viewModel.readerFontSize.sp,
                                                        lineHeight = (viewModel.readerFontSize * 1.6).sp,
                                                        color = if (isTargetWord) Color(0xFF92400E) else Color(0xFF261D15),
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = if (isTargetWord) FontWeight.Bold else FontWeight.Medium,
                                                        textDecoration = if (!isBrushMode && cleanedWord.isNotBlank()) TextDecoration.Underline else TextDecoration.None
                                                    )
                                                }
                                            }
                                            wordInPageIdx++
                                        }
                                    }
                                }
                            }
                        }

                        // Live-drawn yellow highlighter brush canvas overlapping the text
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (strokePoints.size > 1) {
                                val path = Path().apply {
                                    moveTo(strokePoints[0].x, strokePoints[0].y)
                                    for (i in 1 until strokePoints.size) {
                                        lineTo(strokePoints[i].x, strokePoints[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color(0x60FFE81F), // Rich aesthetic yellow highlighter neon tint
                                    style = Stroke(
                                        width = 44f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Page Turn & Pager Controls Bar at Bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (pagerState.currentPage > 0) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
                enabled = pagerState.currentPage > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一頁", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("上一頁", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Elegant indicator dots or numeric progress indicator
            if (paragraphs.size <= 8) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(paragraphs.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 10.dp else 6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                )
                        )
                    }
                }
            } else {
                Text(
                    text = "Page ${pagerState.currentPage + 1} / ${paragraphs.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < paragraphs.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                enabled = pagerState.currentPage < paragraphs.size - 1,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Text("下一頁", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下一頁", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ArticleHubScreen(viewModel: VocabViewModel) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    val readHistory by viewModel.readHistory.collectAsState()
    
    // News browser states
    var selectedCategory by remember { mutableStateOf("technology") }
    val newsArticles by viewModel.newsArticles.collectAsState()
    val isNewsLoading by viewModel.isNewsLoading.collectAsState()
    val newsError by viewModel.newsFetchError.collectAsState()
    
    val activeSubTab = viewModel.activeSubTab

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                var fileName = "本地匯入學習文件"
                var originalName = ""
                val cursor = context.contentResolver.query(it, null, null, null, null)
                if (cursor != null) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        originalName = cursor.getString(nameIndex)
                        fileName = originalName.substringBeforeLast(".")
                    }
                    cursor.close()
                }
                title = fileName

                val isPdf = originalName.endsWith(".pdf", ignoreCase = true) || 
                            context.contentResolver.getType(it)?.contains("pdf", ignoreCase = true) == true

                if (isPdf) {
                    com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(stream)
                        val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
                        val rawText = stripper.getText(document)
                        text = cleanExtractedPdfText(rawText)
                        document.close()
                    }
                } else {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val reader = java.io.BufferedReader(java.io.InputStreamReader(stream))
                        text = reader.readText()
                    }
                }
            } catch (e: Exception) {
                title = "讀取失敗"
                text = "讀取本地檔案或解析時發生錯誤: ${e.localizedMessage}"
            }
        }
    }

    val presetArticles = listOf(
        PresetArticle(
            "伊索寓言：《烏鴉與水壺》",
            "A thirsty crow came upon a pitcher which had once been full of water. " +
            "But when the crow put its beak into the mouth of the pitcher, he found that only very little water was left in it, " +
            "and that he could not reach far enough down to get at it. " +
            "He tried, and he tried, but at last had to give up in despair. " +
            "Then a thought came to him, and he took a pebble and dropped it into the pitcher. " +
            "Then he took another pebble and dropped it into the pitcher. " +
            "Gradually, the water rose to the brim, and the clever bird was able to quench his thirst and save his life."
        ),
        PresetArticle(
            "科學探索：《火星上的生命痕跡》",
            "For decades, scientists have debated whether Mars ever hosted living organisms. " +
            "Recent images from robotic rovers have revealed dried-out ancient river beds, suggesting liquid water once flowed abundantly on the Martian surface. " +
            "In addition, organic molecules containing carbon and hydrogen have been detected inside rock core samples, " +
            "which further supports the theory that microbial life could have existed in the red planet's early humid history. " +
            "Astrophysicists are now preparing future sample-return missions to confirm these extraordinary possibilities."
        ),
        PresetArticle(
            "經濟學選談：《全球化與物價通膨》",
            "Global trade networks have connected distant raw materials, factories, and consumers over the past half-century. " +
            "This globalization brought downs costs significantly for electronic goods, clothing, and household appliances. " +
            "However, recent disruptions in international supply chains, cargo shipping delays, and geopolitical conflicts " +
            "have caused severe bottlenecks in manufacturing centers. " +
            "These factors combined with increased domestic energy expenses have triggered high inflation rates " +
            "worldwide, forcing central banking institutes to rapidly raise interest rates to curb consumer demands."
        )
    )

    LaunchedEffect(selectedCategory) {
        viewModel.fetchCategoryNews(selectedCategory)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Dynamic Student Greeting Banner (Hi, Elizabeth style)
        val currentUser = viewModel.currentUser
        val wordsList by viewModel.vocabWords.collectAsState(initial = emptyList())
        val totalWords = wordsList.size
        val greetingName = currentUser?.name ?: "同學"
        val currentDateStr = remember {
            val sdf = java.text.SimpleDateFormat("E, dd MMM", java.util.Locale.ENGLISH)
            sdf.format(java.util.Date())
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hi, $greetingName 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = currentDateStr,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            // Cute circular avatar with subtle gradient and initial letter
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(4.dp, RoundedCornerShape(22.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFF43F5E), // Coral
                                Color(0xFFF59E0B)  // Orange
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = greetingName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                // Small green indicator dot representing online state
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF10B981), RoundedCornerShape(5.dp))
                        .border(1.5.dp, Color.White, RoundedCornerShape(5.dp))
                        .align(Alignment.TopEnd)
                )
            }
        }

        // 4. Quick Review Banner Card (一鍵複習單字)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.startReviewSession() }
                .shadow(4.dp, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "一鍵複習",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "一鍵複習刷單字 🎴",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = if (totalWords > 0) "智慧複習字卡組 • 累積 5 次自動熟悉！" else "點擊開啟複習模式 (目前尚無單字)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "進入複習",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 5. 歷史閱讀足跡 (Read History Footprints - Prominent standalone top-level section)
        Text(
            text = "📖 歷史閱讀足跡 (${readHistory.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        if (readHistory.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "尚未累積閱讀足跡。點選上方「閱讀即時新聞」或「貼上英文教材」開啟文章閱讀，記錄將自動呈現在這裡！",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(readHistory) { article ->
                    Card(
                        modifier = Modifier
                            .width(230.dp)
                            .height(130.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = article.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteHistoryArticle(article) },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close, 
                                            contentDescription = "刪除記錄",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = article.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 14.sp,
                                    fontSize = 11.sp
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.openArticle(article.title, article.content) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("繼續閱讀 📖", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Primary Article Content Source Tab Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (activeSubTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .onGloballyPositioned { coords ->
                        viewModel.step1TargetRect = coords.boundsInWindow()
                    }
                    .clickable { viewModel.activeSubTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = if (activeSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "即時新聞 🌐",
                        color = if (activeSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (activeSubTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .onGloballyPositioned { coords ->
                        viewModel.step2TargetRect = coords.boundsInWindow()
                    }
                    .clickable { viewModel.activeSubTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = if (activeSubTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "貼上教材 📝",
                        color = if (activeSubTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (activeSubTab == 2) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { viewModel.activeSubTab = 2 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = if (activeSubTab == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "班級作業 🏫",
                        color = if (activeSubTab == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (activeSubTab == 2) {
            ClassAssignmentsSection(viewModel = viewModel)
        } else if (activeSubTab == 1) {
            // Custom Text Uploads & Paste Section
            Text(
                text = "📝 自訂英文教材解析",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("文章標題") },
                        placeholder = { Text("例如：NASA 韋伯探測新發現") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("英文文章內容") },
                        placeholder = { Text("請貼上您想研讀的英文單字/片語段落...") },
                        modifier = Modifier.fillMaxWidth().height(85.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { fileLauncher.launch("text/plain") },
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TXT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { fileLauncher.launch("application/pdf") },
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (text.isNotBlank()) {
                                    val finalTitle = if (title.isBlank()) "自訂教材" else title
                                    viewModel.openArticle(finalTitle, text)
                                    title = ""
                                    text = ""
                                }
                            },
                            enabled = text.isNotBlank(),
                            modifier = Modifier.weight(2f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("開啟筆刷 🚀", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        } else {
            // Live News Section directly inside the Article Hub
            Text(
                text = "🌐 聯合外媒即時英語新聞",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Category selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val categories = listOf(
                    "technology" to "💻 科技",
                    "science" to "🔬 科學",
                    "business" to "📈 商業",
                    "general" to "綜合"
                )
                categories.forEach { (key, label) ->
                    val isSelected = selectedCategory == key
                    Button(
                        onClick = { selectedCategory = key },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isNewsLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("正在同步外媒主題並由 Gemini 撰寫完整文章...", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                }
            } else if (newsError != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(newsError ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { viewModel.fetchCategoryNews(selectedCategory) }) {
                        Text("重試載入")
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    newsArticles.take(15).forEach { article ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectNewsArticle(article) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = article.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 2,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (!article.description.isNullOrBlank()) {
                                    Text(
                                        text = article.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "來源: ${article.source?.name ?: "即時時事"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "點擊立刻載入筆刷閱讀 📖",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Flat structure representation for index mapping
private data class FlatWordToken(val index: Int, val word: String, val paragraph: String)

private fun chunkTextIntoPages(text: String, wordsPerPage: Int = 180): List<String> {
    if (text.isBlank()) return listOf("")
    
    val normalizedText = text.replace("\r\n", "\n").trim()
    var rawParagraphs = normalizedText.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }
    if (rawParagraphs.size <= 1) {
        rawParagraphs = normalizedText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    }
    
    val pages = mutableListOf<String>()
    var currentPageParagraphs = mutableListOf<String>()
    var currentWordCount = 0
    
    for (paragraph in rawParagraphs) {
        val wordCount = paragraph.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        if (wordCount == 0) continue
        
        if (currentWordCount + wordCount > wordsPerPage && currentPageParagraphs.isNotEmpty()) {
            pages.add(currentPageParagraphs.joinToString("\n"))
            currentPageParagraphs = mutableListOf(paragraph)
            currentWordCount = wordCount
        } else {
            currentPageParagraphs.add(paragraph)
            currentWordCount += wordCount
        }
    }
    
    if (currentPageParagraphs.isNotEmpty()) {
        pages.add(currentPageParagraphs.joinToString("\n"))
    }
    
    return if (pages.isEmpty()) listOf("") else pages
}

private fun cleanExtractedPdfText(rawText: String): String {
    if (rawText.isBlank()) return ""
    val lines = rawText.split("\n")
    val cleanedLines = mutableListOf<String>()
    
    // Pattern to match page numbers, running headers, and typical PDF noise lines:
    val pageNumberRegex = Regex("^(?i)(page\\s+\\d+|\\d+\\s*/\\s*\\d+|\\d+\\s+of\\s+\\d+|\\d+|\\s*—\\s*\\d+\\s*—\\s*)$")
    
    for (line in lines) {
        val trimmed = line.trim()
        
        // 1. Skip completely empty lines
        if (trimmed.isEmpty()) continue
        
        // 2. Skip obvious page numbers, running footers/headers
        if (pageNumberRegex.matches(trimmed)) {
            continue
        }
        
        // 3. Skip typical lines consisting of non-alphanumeric dividers/symbols
        if (trimmed.all { !it.isLetterOrDigit() && !it.isWhitespace() }) {
            continue
        }
        
        cleanedLines.add(trimmed)
    }
    
    // Reconstruct lines elegantly into clean paragraphs
    val paragraphs = mutableListOf<String>()
    val currentParagraph = java.lang.StringBuilder()
    
    for (line in cleanedLines) {
        if (currentParagraph.isNotEmpty()) {
            currentParagraph.append(" ")
        }
        currentParagraph.append(line)
        
        // If a line ends with paragraph punctuation, or is short, let's treat it as a paragraph end
        if (line.endsWith(".") || line.endsWith("!") || line.endsWith("?") || line.length < 45) {
            paragraphs.add(currentParagraph.toString().trim())
            currentParagraph.setLength(0)
        }
    }
    if (currentParagraph.isNotEmpty()) {
        paragraphs.add(currentParagraph.toString().trim())
    }
    
    return paragraphs.joinToString("\n\n")
}

private fun reconstructAllWords(paragraphs: List<String>): List<FlatWordToken> {
    val list = mutableListOf<FlatWordToken>()
    var counter = 0
    paragraphs.forEach { para ->
        val rawWords = para.split(Regex("\\s+")).filter { it.isNotBlank() }
        rawWords.forEach { w ->
            list.add(FlatWordToken(counter, w, para))
            counter++
        }
    }
    return list
}

@Composable
fun FilledTonalButton(
    isHighlighted: Boolean,
    onClick: () -> Unit,
    text: String
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isHighlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ImportDialog(viewModel: VocabViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var importSourceTab by remember { mutableStateOf(0) } // 0 = 本地與內建, 1 = 外媒即時新聞

    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }

    // File Picker for downloaded text documents (.txt or .pdf)
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                // Try to resolve clean file name dynamically
                var fileName = "本地匯入文件"
                val cursor = context.contentResolver.query(it, null, null, null, null)
                val nameIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor != null && nameIndex != null && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex)
                }
                cursor?.close()
                title = fileName.substringBeforeLast(".")

                val isPdf = fileName.endsWith(".pdf", ignoreCase = true) || 
                            context.contentResolver.getType(it)?.contains("pdf", ignoreCase = true) == true

                if (isPdf) {
                    com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(stream)
                        val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
                        val rawText = stripper.getText(document)
                        text = cleanExtractedPdfText(rawText)
                        document.close()
                    }
                } else {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val reader = java.io.BufferedReader(java.io.InputStreamReader(stream))
                        text = reader.readText()
                    }
                }
            } catch (e: Exception) {
                title = "讀取失敗"
                text = "讀取或解析文件時發生異常: ${e.localizedMessage}"
            }
        }
    }

    val presetArticles = listOf(
        PresetArticle(
            "伊索寓言：《烏鴉與水壺》",
            "A thirsty crow came upon a pitcher which had once been full of water. But when the crow put its beak into the mouth of the pitcher, he found that only very little water was left in it, and that he could not reach far enough down to get at it. He tried, and he tried, but at last had to give up in despair. Then a thought came to him, and he took a pebble and dropped it into the pitcher. Gradually, the water rose to the brim, and the clever bird was able to save his life."
        ),
        PresetArticle(
            "小王子精選（Chapter 1）",
            "Once when I was six years old I saw a magnificent picture in a book, called True Stories from Nature, about the primeval forest. It was a picture of a boa constrictor in the act of swallowing an animal. In the book it said: 'Boa constrictors swallow their prey whole, without chewing it. After that they are not able to move, and they sleep through the six months of their digestion.'"
        ),
        PresetArticle(
            "人工智慧的科技浪潮",
            "Artificial intelligence is transforming our civilization at a staggering rate. Large Language Models and intelligent machine learning algorithms analyze mountains of human knowledge to assist in coding, reasoning, and creating. Mastering these cognitive innovations represents a supreme goal for technology researchers and passionate engineers globally."
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("匯入英文學習文件與新聞 📰", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Horizontal Navigation segment tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { importSourceTab = 0 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (importSourceTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (importSourceTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                    ) {
                        Text("本地/內建文件 📂", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { importSourceTab = 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (importSourceTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (importSourceTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                    ) {
                        Text("外媒即時新聞 🌐", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (importSourceTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // File chooser trigger split into TXT and PDF options for flawless system filtering
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { fileLauncher.launch("text/plain") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("📄 匯入 TXT", fontWeight = FontWeight.ExtraBold)
                            }

                            Button(
                                onClick = { fileLauncher.launch("application/pdf") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("📕 匯入 PDF", fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        Text("💡 選擇內建經典文章快速體驗：", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        
                        PresetList(presetArticles) { selectedArticle ->
                            title = selectedArticle.title
                            text = selectedArticle.content
                        }

                        Text("📝 或者是貼上您想閱讀的自訂段落：", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("文章標題") },
                            placeholder = { Text("例如：自訂學習文章") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            label = { Text("英文文章內容") },
                            placeholder = { Text("貼上英文段落或單字書內容...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                    }
                } else {
                    // Live News API Browser
                    var selectedCategory by remember { mutableStateOf("technology") }
                    val newsArticles by viewModel.newsArticles.collectAsState()
                    val isNewsLoading by viewModel.isNewsLoading.collectAsState()
                    val newsError by viewModel.newsFetchError.collectAsState()

                    LaunchedEffect(selectedCategory) {
                        viewModel.fetchCategoryNews(selectedCategory)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Category switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val categories = listOf(
                                "technology" to "💻 科技",
                                "science" to "🔬 科學",
                                "business" to "📈 商業",
                                "general" to "🌐 綜合"
                            )
                            categories.forEach { (key, label) ->
                                FilterChip(
                                    selected = selectedCategory == key,
                                    onClick = { selectedCategory = key },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (isNewsLoading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("正在同步外媒主題並由 Gemini 撰寫完整文章...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                            }
                        } else if (newsError != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(newsError ?: "", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(onClick = { viewModel.fetchCategoryNews(selectedCategory) }) {
                                    Text("重試載入")
                                }
                            }
                        } else if (newsArticles.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("目前暫無該分類新聞", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                newsArticles.take(15).forEach { article ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectNewsArticle(article)
                                                onDismiss()
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = article.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 2
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = article.description ?: "點擊直接匯入閱讀此新聞...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "來源: ${article.source?.name ?: "外媒"}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Text(
                                                    text = article.publishedAt?.substringBefore("T") ?: "",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (importSourceTab == 0) {
                Button(
                    onClick = {
                        if (text.isNotBlank()) {
                            viewModel.documentTitle = if (title.isBlank()) "自訂匯入文件" else title
                            viewModel.documentText = text
                            onDismiss()
                        }
                    }
                ) {
                    Text("確定匯入自訂文件")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("關閉")
            }
        }
    )
}

private data class PresetArticle(val title: String, val content: String)

@Composable
private fun PresetList(presets: List<PresetArticle>, onSelect: (PresetArticle) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        presets.forEach { article ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(article) },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "文章",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)


@Composable
fun TranslationSheet(
    state: TranslationState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    androidx.compose.runtime.DisposableEffect(context) {
        var instance: android.speech.tts.TextToSpeech? = null
        instance = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                instance?.language = java.util.Locale.US
            }
        }
        tts = instance
        onDispose {
            instance.stop()
            instance.shutdown()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF00E676), RoundedCornerShape(50))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini AI 雙語字典室",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.bounceClick()
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "關閉")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (state) {
                is TranslationState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "正在連線 AI 加速解碼釋意中...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is TranslationState.Success -> {
                    val detail = state.detail
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = detail.word,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (detail.phonetic.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "[${detail.phonetic}]",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            // Elegant pronunciation button with custom bouncing click and ripple interaction
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .bounceClick()
                                    .clickable {
                                        tts?.speak(detail.word, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔊", fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (detail.partOfSpeech.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = detail.partOfSpeech,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = detail.translation,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        if (detail.definition.trim().isEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClick()
                                    .clickable {
                                        // Future subscription / paywall popup trigger
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Premium Lock",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "付費解鎖：詳細釋意、語境搭配與情境例句",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = "加入智慧時事 Premium，即可解鎖由 Gemini AI 悉心調教的單字深度用法、常用搭配詞以及高分寫作例句，倍速提升閱讀敏銳度！",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = detail.definition,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "存檔成功",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "此單字已自動記錄至您的生字庫後台 📝",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                is TranslationState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "翻譯或金鑰認證失敗",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("關閉")
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun VocabBookScreen(viewModel: VocabViewModel, vocabList: List<VocabWord>) {
    var filterState by remember { mutableStateOf(0) } // 0 = All, 1 = Learning, 2 = Mastered
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(vocabList, filterState, searchQuery) {
        vocabList.filter { word ->
            val matchesFilter = when (filterState) {
                1 -> word.status == 0
                2 -> word.status == 1
                else -> true
            }
            val matchesSearch = word.word.lowercase().contains(searchQuery.lowercase()) ||
                                word.definition.lowercase().contains(searchQuery.lowercase())
            matchesFilter && matchesSearch
        }
    }

    val totalCount = vocabList.size
    val masteredCount = vocabList.count { it.status == 1 }

    // Group the vocabulary list by source article for easy classification/categories,
    // most recently studied article first (子母清單: article = 母, its words = 子)
    val sortedGroups = remember(filteredList) {
        filteredList.groupBy { it.sourceArticle.ifBlank { "來自自訂筆刷查詢" } }
            .toList()
            .sortedByDescending { (_, words) -> words.maxOf { it.timestamp } }
    }

    // Per-article collapse state, keyed by sourceArticle. Absent = collapsed by default so a
    // vocab book with many articles doesn't render as one giant scrolling list.
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }
    val allExpanded = sortedGroups.isNotEmpty() && sortedGroups.all { (src, _) -> expandedGroups[src] == true }
    val dateFormatter = remember { java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.TAIWAN) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "智慧單字庫彙報表",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "總共標註了 $totalCount 個字，已熟悉 $masteredCount 個字！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Button(
                onClick = { viewModel.startReviewSession() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "隨機複習 10 個字",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("隨機複習 10 個", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        if (sortedGroups.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    val target = !allExpanded
                    sortedGroups.forEach { (src, _) -> expandedGroups[src] = target }
                }) {
                    Icon(
                        imageVector = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                        contentDescription = if (allExpanded) "全部收合" else "全部展開",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (allExpanded) "全部收合" else "全部展開", fontSize = 12.sp)
                }
            }
        }

        // Search Bar container
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("搜尋已記錄字詞...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜尋") },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除")
                    }
                }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors()
        )

        // Filter chips bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterState == 0,
                onClick = { filterState = 0 },
                label = { Text("全部 (${vocabList.size})") }
            )
            FilterChip(
                selected = filterState == 1,
                onClick = { filterState = 1 },
                label = { Text("學習中 (${vocabList.count { it.status == 0 }})") }
            )
            FilterChip(
                selected = filterState == 2,
                onClick = { filterState = 2 },
                label = { Text("已掌握 (${vocabList.count { it.status == 1 }})") }
            )
        }

        if (sortedGroups.isEmpty()) {
            // Empty placeholder state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "無資料",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (searchQuery.isNotEmpty()) "找不到相符的字彙拼圖～" else "生字欄位空空的喔！",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "快回閱讀器，用筆刷塗抹那些拼不懂的英文單字吧！",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            // Classified group list cards (子母清單：文章 = 母，底下單字 = 子，可各自收合)
            sortedGroups.forEach { (sourceArticle, words) ->
                val isExpanded = expandedGroups[sourceArticle] == true
                val lastStudied = remember(words) { dateFormatter.format(java.util.Date(words.maxOf { it.timestamp })) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                        .clickable { expandedGroups[sourceArticle] = !isExpanded },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "文章來源",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sourceArticle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "最近複習 $lastStudied",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = "${words.size} 個字詞",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        IconButton(onClick = { viewModel.startReviewSession(customWords = words) }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "只複習這篇文章的單字",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "收合" else "展開",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column {
                        words.forEach { word ->
                            VocabWordCard(
                                word = word,
                                onToggleMastery = { viewModel.toggleWordMastered(word) },
                                onDelete = { viewModel.deleteWord(word) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VocabWordCard(
    word: VocabWord,
    onToggleMastery: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .testTag("vocab_card_${word.word}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (word.status == 1) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (word.status == 1) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (word.status == 1) Icons.Default.CheckCircle else Icons.Default.Star,
                        contentDescription = null,
                        tint = if (word.status == 1) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = word.word,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = if (word.status == 1) TextDecoration.LineThrough else TextDecoration.None
                    )
                    
                    Surface(
                        color = if (word.status == 1) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = if (word.status == 1) "已熟練 (複習 5/5)" else "複習 ${word.reviewCount}/5 次",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (word.status == 1) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    
                    if (!isExpanded) {
                        Spacer(modifier = Modifier.width(12.dp))
                        val summary = remember(word.definition) {
                            val line = word.definition.substringBefore("\n").substringBefore(" - ")
                            if (line.length > 18) line.take(18) + "..." else line
                        }
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isExpanded) "收合 ▲" else "展開 ▼",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Mastery mark button (absorbs clicks so it doesn't fold/unfold)
                    IconButton(
                        onClick = { onToggleMastery() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (word.status == 1) Icons.Default.CheckCircle else Icons.Default.Check,
                            contentDescription = "掌握標記",
                            tint = if (word.status == 1) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = word.definition,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )

                    if (word.contextSentence.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "📖 上下文句：\n${highlightContextWord(word.contextSentence, word.word)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("刪除單字記錄", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun highlightContextWord(sentence: String, word: String): String {
    val regex = Regex("(?i)($word)")
    return sentence.replace(regex, "🌟 $1 🌟")
}

fun Modifier.bounceClick(): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "bounceScale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun DailyInitLoadingScreen(viewModel: VocabViewModel) {
    val immersiveQuotes = remember {
        listOf(
            "「用智慧的筆刷，在國際時事中遇見更好的自己。」\n\n讓英語學習融入生活，開啟新的一天 📖",
            "「每一天，都是拓展視野與詞彙的全新旅程。」\n\n浸淫在真實時事語境，單字便有了靈魂 ✨",
            "「靜下心來，讓我們一起從世界脈動中汲取最道地的英語養分。」\n\n智慧塗刷，即刻解密 🌐",
            "「浸淫在真實語境中，單字不再只是死記硬背，而是通往世界的一頁篇章。」\n\n正為您悉心調配今日的專屬教材 ☕",
            "「準備好用英文觸碰世界的溫度了嗎？專注當下，點亮今日的學習微光。」\n\n啟航，通往國際視野的全新一天 🌟",
            "「最有效的語言學習，是在真實的場景中去感受、去連結、去理解。」\n\n正為您雕琢今日的時事學習板塊 🎨",
            "「每天進步一點點，詞彙積沙成塔，視野與格局將隨之寬廣無垠。」\n\n靜心閱讀，預備今日的高效專注 🚀"
        )
    }
    val randomQuote = remember { immersiveQuotes.random() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(enabled = false) { } // block any clicks to widgets behind it
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            // Branded icon box with a soft infinite pulse scale animation for superior UX feedback
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
            val scalePulse by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.08f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(1200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "scalePulse"
            )

            Box(
                modifier = Modifier
                    .size(108.dp)
                    .graphicsLayer {
                        scaleX = scalePulse
                        scaleY = scalePulse
                    }
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "智慧時事筆刷閱覽",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Smart Brush Reader",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(40.dp)
            )
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "✨ 靜心微光 ✨",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = randomQuote,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingSpotlightOverlay(viewModel: VocabViewModel) {
    val step = viewModel.onboardingStep
    val goalMins = viewModel.dailyReadingGoalMinutes

    if (step == 0) {
        var selectedMins by remember { mutableStateOf(goalMins) }
        var customMinsText by remember { mutableStateOf(goalMins.toString()) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 450.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.img_app_logo),
                                contentDescription = "App Logo",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Text(
                        text = "設定您的每日閱讀目標",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "歡迎使用智慧筆刷學英文！請先設定您每天預計閱讀英文的時間 (分鐘)，我們將在個人中心為您記錄每日達標進度：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 15, 20, 30).forEach { mins ->
                            val isSelected = selectedMins == mins
                            Surface(
                                selected = isSelected,
                                onClick = {
                                    selectedMins = mins
                                    customMinsText = mins.toString()
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$mins 分鐘",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customMinsText,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            customMinsText = digits
                            digits.toIntOrNull()?.let { selectedMins = it }
                        },
                        label = { Text("自訂每日時間 (5 ~ 180 分鐘)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val finalMins = customMinsText.toIntOrNull() ?: 15
                            viewModel.updateDailyReadingGoal(finalMins)
                            viewModel.nextOnboardingStep()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("儲存目標並開始體驗 🚀", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    } else {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val screenWidthPx = with(density) { maxWidth.toPx() }
            val screenHeightPx = with(density) { maxHeight.toPx() }
            val paddingPx = with(density) { 8.dp.toPx() }

            val rawRect = when (step) {
                1 -> viewModel.step1TargetRect
                2 -> viewModel.step2TargetRect
                3 -> viewModel.step3TargetRect
                4 -> viewModel.step4TargetRect
                else -> null
            }

            val paddedTargetRect = if (rawRect != null) {
                Rect(
                    left = (rawRect.left - paddingPx).coerceAtLeast(0f),
                    top = (rawRect.top - paddingPx).coerceAtLeast(0f),
                    right = (rawRect.right + paddingPx).coerceAtMost(screenWidthPx),
                    bottom = (rawRect.bottom + paddingPx).coerceAtMost(screenHeightPx)
                )
            } else {
                val defaultWidth = with(density) { 220.dp.toPx() }
                val defaultHeight = with(density) { 54.dp.toPx() }
                val centerY = when (step) {
                    1 -> screenHeightPx * 0.42f
                    2 -> screenHeightPx * 0.42f
                    3 -> screenHeightPx * 0.94f
                    else -> screenHeightPx * 0.94f
                }
                Rect(
                    left = (screenWidthPx - defaultWidth) / 2f,
                    top = centerY - (defaultHeight / 2f),
                    right = (screenWidthPx + defaultWidth) / 2f,
                    bottom = centerY + (defaultHeight / 2f)
                )
            }

            // Smooth animated rectangle transition between targets
            val animTargetRect by animateRectAsState(
                targetValue = paddedTargetRect,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                label = "spotlight_rect_anim"
            )

            // Dynamic entrance spotlight animation (starts from normal bright screen, shrinks inward to button)
            val animatable = remember { Animatable(0f) }
            LaunchedEffect(step) {
                animatable.snapTo(0f)
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                )
            }
            val animProgress = animatable.value

            val currentLeft = androidx.compose.ui.util.lerp(0f, animTargetRect.left, animProgress)
            val currentTop = androidx.compose.ui.util.lerp(0f, animTargetRect.top, animProgress)
            val currentRight = androidx.compose.ui.util.lerp(screenWidthPx, animTargetRect.right, animProgress)
            val currentBottom = androidx.compose.ui.util.lerp(screenHeightPx, animTargetRect.bottom, animProgress)

            val overlayAlpha = androidx.compose.ui.util.lerp(0f, 0.82f, animProgress)
            val cornerRadiusPx = with(density) { androidx.compose.ui.util.lerp(0f, 16.dp.toPx(), animProgress) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = false) {}
            ) {
                // Offscreen hardware layer canvas for clear spotlight aperture cutout
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.99f }
                ) {
                    // Dark background overlay fading in from edges
                    drawRect(color = Color.Black.copy(alpha = overlayAlpha))

                    // Punch out target button area
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(currentLeft, currentTop),
                        size = Size((currentRight - currentLeft).coerceAtLeast(1f), (currentBottom - currentTop).coerceAtLeast(1f)),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        blendMode = BlendMode.Clear
                    )
                }

                // Place explanation dialog box cleanly to avoid covering target button
                val isTargetInLowerHalf = (currentTop + currentBottom) / 2f > screenHeightPx * 0.5f
                val dialogAlignment = if (isTargetInLowerHalf) Alignment.TopCenter else Alignment.BottomCenter

                Column(
                    modifier = Modifier
                        .align(dialogAlignment)
                        .fillMaxWidth(0.94f)
                        .widthIn(max = 480.dp)
                        .padding(horizontal = 16.dp, vertical = 28.dp)
                        .graphicsLayer {
                            alpha = animProgress
                            translationY = (1f - animProgress) * 20f
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "功能導覽 Step $step / 4",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            val title = when (step) {
                                1 -> "🌐 1. 閱讀即時新聞"
                                2 -> "📝 2. 貼上英文教材"
                                3 -> "🎴 3. 智慧生字庫複習"
                                else -> "🧱 4. 個人統計使用牆"
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val desc = when (step) {
                                1 -> "【閱讀即時新聞】每日自動為您刷新全球時事新聞！閱讀時「點擊單字」即可使用筆刷查詢釋義並自動收錄至生字庫，同時支援三種難易度切換。"
                                2 -> "【貼上英文教材】您可以自由複製貼上任何英文論文、小說或課程筆記。系統會協助進行句型拆解與生字剖析，開啟雙語筆刷對照！"
                                3 -> "【智慧生字庫複習】閱讀標註的單字會存入生字庫。透過「一鍵複習刷單字」閃卡與多選測驗，單字累積複習 5 次即自動判定為已熟練！"
                                else -> "【個人統計中心】全方位學習數據皆收錄於個人中心！包含【每日閱讀時間目標達成率】、總生字數、熟練字數與歷史閱讀足跡，簡潔清晰無複雜圖表。"
                            }

                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp,
                                fontSize = 13.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewModel.completeOnboarding() }
                                ) {
                                    Text("跳過導覽", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Button(
                                    onClick = { viewModel.nextOnboardingStep() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (step == 4) "完成教學 🎯" else "下一步 ➡️",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
