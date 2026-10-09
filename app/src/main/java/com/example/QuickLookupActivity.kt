package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.data.VocabWordStore
import com.example.network.VocabDetail
import com.example.network.VocabTranslationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "Vocabrush 翻譯" entry in the system text-selection menu (ACTION_PROCESS_TEXT) and the share
 * sheet (ACTION_SEND text/plain). Shows a small dynamic-island-style card over whatever app the
 * user was reading in, looks the selection up through the same public_dictionary -> Gemini path
 * as in-app brushing, and saves it into the vocab book under SOURCE_TITLE so it joins the normal
 * review flow (and syncs to the PWA through users/{uid}/vocabWords).
 */
class QuickLookupActivity : ComponentActivity() {

    private sealed class LookupState {
        object Loading : LookupState()
        data class Success(val detail: VocabDetail, val savedToCloud: Boolean) : LookupState()
        data class Message(val text: String) : LookupState()
    }

    private var state by mutableStateOf<LookupState>(LookupState.Loading)
    private var query by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        enableEdgeToEdge()
        setContent {
            com.example.ui.theme.MyApplicationTheme {
                LookupOverlay()
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val raw = intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            ?: intent?.getStringExtra(Intent.EXTRA_TEXT)
            ?: ""
        val cleaned = cleanSelection(raw)
        query = cleaned.ifBlank { raw.trim().take(40) }

        if (!isLookupable(cleaned)) {
            state = LookupState.Message("請選取一個英文單字或短片語（最多 $MAX_WORDS 個字）")
            return
        }

        state = LookupState.Loading
        lifecycleScope.launch {
            val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
            val firestore = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
            val detail = try {
                withContext(Dispatchers.IO) {
                    VocabTranslationHelper.translateWordWithPublicDict(cleaned, cleaned, firestore)
                }
            } catch (e: Exception) {
                null
            }
            if (detail == null) {
                state = LookupState.Message("找不到「$cleaned」的翻譯。")
                return@launch
            }
            if (VocabWordStore.isFailedTranslation(detail)) {
                state = LookupState.Message(detail.definition.ifBlank { "翻譯連線失敗，請稍後再試。" })
                return@launch
            }
            withContext(Dispatchers.IO) {
                VocabWordStore.upsertLookedUpWord(
                    this@QuickLookupActivity, detail, sentence = "", sourceTitle = SOURCE_TITLE,
                    auth = auth, firestore = firestore
                )
            }
            val signedIn = auth?.currentUser?.let { !it.isAnonymous } ?: false
            state = LookupState.Success(detail, savedToCloud = signedIn)
        }
    }

    private fun openApp() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
    }

    @Composable
    private fun LookupOverlay() {
        // Tapping anywhere outside the card dismisses it, like a system popup.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { finish() },
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .background(IslandBlack, RoundedCornerShape(28.dp))
                    // Swallow taps on the card itself so they don't dismiss it.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .animateContentSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                when (val s = state) {
                    is LookupState.Loading -> LoadingContent()
                    is LookupState.Success -> SuccessContent(s.detail, s.savedToCloud)
                    is LookupState.Message -> MessageContent(s.text)
                }
            }
        }
    }

    @Composable
    private fun LoadingContent() {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Accent
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = query,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    private fun SuccessContent(detail: VocabDetail, savedToCloud: Boolean) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = detail.word,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            if (detail.phonetic.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(text = detail.phonetic, color = Muted, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = listOf(detail.partOfSpeech, detail.translation).filter { it.isNotBlank() }.joinToString("  "),
            color = Accent,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (detail.definition.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = detail.definition,
                color = Muted,
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (savedToCloud) "✓ 已存入單字本「$SOURCE_TITLE」" else "✓ 已存入本機單字本（登入後會同步到雲端）",
            color = Saved,
            fontSize = 13.sp
        )
        ActionRow()
    }

    @Composable
    private fun MessageContent(text: String) {
        Text(text = query, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Text(text = text, color = Muted, fontSize = 14.sp)
        ActionRow()
    }

    @Composable
    private fun ActionRow() {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { openApp() }) { Text("開啟 Vocabrush", color = Muted) }
            TextButton(onClick = { finish() }) { Text("完成", color = Accent, fontWeight = FontWeight.SemiBold) }
        }
    }

    companion object {
        /** Vocab-book group these words land in (the book groups words by sourceArticle). */
        const val SOURCE_TITLE = "站外選字"
        private const val MAX_WORDS = 6
        private const val MAX_CHARS = 60

        private val IslandBlack = Color(0xF2111111)
        private val Accent = Color(0xFF7DD3FC)
        private val Muted = Color(0xFFB4B4B4)
        private val Saved = Color(0xFF86EFAC)

        /** Same edge-punctuation cleanup as the in-app brush gesture (MainActivity). */
        fun cleanSelection(raw: String): String =
            raw.replace(Regex("\\s+"), " ").trim()
                .replace(Regex("^[^a-zA-Z0-9]+|[^a-zA-Z0-9]+$"), "")

        fun isLookupable(text: String): Boolean =
            text.isNotBlank() &&
                text.length <= MAX_CHARS &&
                text.any { it in 'a'..'z' || it in 'A'..'Z' } &&
                text.split(' ').size <= MAX_WORDS
    }
}
