package com.example.nallanudi

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nallanudi.ui.theme.NallaNudiTheme
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

val DeepNavy = Color(0xFF0F172A)
val Charcoal = Color(0xFF1E293B)
val Indigo = Color(0xFF6366F1)
val Teal = Color(0xFF14B8A6)
val WhiteGray = Color(0xFFF8FAFC)
val MutedGray = Color(0xFF94A3B8)
val SoftRed = Color(0xFFEF4444)

enum class Screen {
    Home,
    SavedWords,
    AboutDeveloper
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NallaNudiTheme {
                AppScreen()
            }
        }
    }
}

@Composable
fun AppScreen() {
    val context = LocalContext.current
    val savedWords = remember {
        loadWordsFromPrefs(context).toMutableStateList()
    }

    var currentScreen by remember { mutableStateOf(Screen.Home) }

    when (currentScreen) {
        Screen.Home -> HomeScreen(
            savedWords = savedWords,
            onNavigateToSaved = { currentScreen = Screen.SavedWords },
            onNavigateToAbout = { currentScreen = Screen.AboutDeveloper }
        )

        Screen.SavedWords -> SavedWordsScreen(
            savedWords = savedWords,
            onBack = { currentScreen = Screen.Home }
        )

        Screen.AboutDeveloper -> AboutDeveloperScreen(
            onBack = { currentScreen = Screen.Home }
        )
    }
}

@Composable
fun StyledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Indigo,
    contentColor: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "button_scale"
    )

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(
                elevation = if (isPressed) 4.dp else 12.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = containerColor.copy(alpha = 0.6f)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun HomeScreen(
    savedWords: SnapshotStateList<Word>,
    onNavigateToSaved: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val context = LocalContext.current
    val jsonWords = remember { loadWordsFromJson(context) }
    val allWords = remember { wordList + jsonWords }

    val isPreview = LocalInspectionMode.current
    val tts = if (!isPreview) {
        remember { TextToSpeech(context) { } }
    } else null

    LaunchedEffect(Unit) {
        tts?.language = Locale.US
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val searchText = remember { mutableStateOf("") }
    val result = remember { mutableStateOf("") }
    val selectedSubject = remember { mutableStateOf("All") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(
            text = "📘 Nalla Nudi",
            fontSize = 36.sp,
            color = WhiteGray,
            fontWeight = FontWeight.ExtraBold,
            style = TextStyle(
                shadow = Shadow(
                    color = Teal.copy(alpha = 0.7f),
                    blurRadius = 25f
                )
            )
        )

        Text(
            text = "Formal English-Kannada Dictionary",
            color = MutedGray
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("All", "Science", "Maths", "Commerce").forEach { subject ->
                val isSelected = selectedSubject.value == subject
                val bg by animateColorAsState(
                    targetValue = if (isSelected) Teal else Charcoal,
                    label = "subject_color"
                )

                Surface(
                    onClick = { selectedSubject.value = subject },
                    modifier = Modifier.weight(1f),
                    color = bg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = subject,
                        modifier = Modifier.padding(vertical = 12.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) DeepNavy else WhiteGray
                    )
                }
            }
        }

        OutlinedTextField(
            value = searchText.value,
            onValueChange = { searchText.value = it },
            label = { Text("Enter English Word") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = WhiteGray,
                unfocusedTextColor = WhiteGray,
                focusedBorderColor = Indigo,
                unfocusedBorderColor = Charcoal
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StyledButton(
                text = "🔍 Search",
                onClick = {
                    val filtered = if (selectedSubject.value == "All") {
                        allWords
                    } else {
                        allWords.filter { it.subject == selectedSubject.value }
                    }

                    val found = filtered.find {
                        it.english.equals(searchText.value.trim(), ignoreCase = true)
                    }

                    result.value = found?.let {
                        "Subject: ${it.subject}\nKannada: ${it.kannada}\nMeaning: ${it.explanation}"
                    } ?: "Word not found"
                },
                modifier = Modifier.weight(1f)
            )

            StyledButton(
                text = "🔊 Speak",
                onClick = {
                    tts?.speak(
                        searchText.value,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        null
                    )
                },
                containerColor = Teal,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        StyledButton(
            text = "❤️ Save Word",
            onClick = {
                val filtered = if (selectedSubject.value == "All") {
                    allWords
                } else {
                    allWords.filter { it.subject == selectedSubject.value }
                }

                val found = filtered.find {
                    it.english.equals(searchText.value.trim(), ignoreCase = true)
                }

                if (found != null && !savedWords.any { it.english == found.english }) {
                    savedWords.add(found)
                    saveWordsToPrefs(context, savedWords)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            containerColor = Charcoal
        )

        if (result.value.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = Teal
                    ),
                colors = CardDefaults.cardColors(Charcoal),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = result.value,
                    modifier = Modifier.padding(24.dp),
                    fontSize = 18.sp,
                    color = WhiteGray,
                    lineHeight = 28.sp
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clickable { onNavigateToSaved() },
            colors = CardDefaults.cardColors(
                containerColor = Indigo.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Indigo.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Saved Words",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhiteGray
                    )
                    Text(
                        text = "View your personal dictionary",
                        fontSize = 14.sp,
                        color = MutedGray
                    )
                }
                Text(
                    text = "➔",
                    fontSize = 24.sp,
                    color = Teal,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clickable { onNavigateToAbout() },
            colors = CardDefaults.cardColors(
                containerColor = Teal.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Teal.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "About Developer",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhiteGray
                    )
                    Text(
                        text = "Meet the creator of Nalla Nudi",
                        fontSize = 14.sp,
                        color = MutedGray
                    )
                }
                Text("👨‍💻", fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SavedWordsScreen(
    savedWords: SnapshotStateList<Word>,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Text("⬅", color = WhiteGray, fontSize = 24.sp)
                }
                Text(
                    text = "Saved Words",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhiteGray
                )
            }

            if (savedWords.isNotEmpty()) {
                TextButton(
                    onClick = {
                        savedWords.clear()
                        saveWordsToPrefs(context, savedWords)
                    }
                ) {
                    Text(
                        text = "Clear All",
                        color = SoftRed,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (savedWords.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No words saved yet",
                    color = MutedGray,
                    fontSize = 16.sp
                )
            }
        } else {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                savedWords.forEach { word ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(Charcoal),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = word.english,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhiteGray
                                )
                                Text(
                                    text = word.kannada,
                                    color = Teal,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = word.explanation,
                                    fontSize = 14.sp,
                                    color = MutedGray
                                )
                            }

                            IconButton(
                                onClick = {
                                    savedWords.remove(word)
                                    saveWordsToPrefs(context, savedWords)
                                }
                            ) {
                                Text("❌", color = SoftRed)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun AboutDeveloperScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Text("⬅", color = WhiteGray, fontSize = 24.sp)
            }
            Text(
                text = "About Developer",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = WhiteGray
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = CardDefaults.cardColors(Charcoal),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    text = "👤 Developer",
                    color = Teal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Likhitha M S",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhiteGray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "Student | Android Developer",
                    fontSize = 16.sp,
                    color = MutedGray
                )
                Text(
                    text = "A dedicated student and aspiring Android developer focused on creating intuitive mobile experiences using Kotlin and Jetpack Compose.",
                    fontSize = 15.sp,
                    color = WhiteGray.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = CardDefaults.cardColors(Charcoal),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    text = "🚀 Project",
                    color = Indigo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Nalla Nudi App",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhiteGray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "Nalla Nudi is an English-Kannada dictionary built for students. It supports subjects like Science, Maths, and Commerce with academic translations.",
                    fontSize = 15.sp,
                    color = WhiteGray.copy(alpha = 0.8f)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = CardDefaults.cardColors(Charcoal),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "✉️ Contact & Links",
                    color = Teal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📧", fontSize = 20.sp, modifier = Modifier.width(32.dp))
                    Text(
                        text = "1mp22ai024@gmail.com",
                        color = WhiteGray,
                        fontSize = 15.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔗", fontSize = 20.sp, modifier = Modifier.width(32.dp))
                    Text(
                        text = "linkedin.com/in/likhitha-ms-69404525a",
                        color = Indigo,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Version 1.0.0",
            color = MutedGray,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

data class Word(
    val english: String,
    val kannada: String,
    val explanation: String,
    val subject: String
)

val wordList = listOf(
    Word("Gravity", "ಗುರುತ್ವಾಕರ್ಷಣೆ", "Objects attract each other", "Science"),
    Word("Force", "ಬಲ", "Push or pull", "Science"),
    Word("Atom", "ಅಣು", "Smallest unit of matter", "Science"),
    Word("Angle", "ಕೋನ", "Space between two lines", "Maths"),
    Word("Equation", "ಸಮೀಕರಣ", "Mathematical statement", "Maths"),
    Word("Profit", "ಲಾಭ", "Gain from business", "Commerce"),
    Word("Loss", "ನಷ್ಟ", "Money lost", "Commerce")
)

fun saveWordsToPrefs(context: Context, words: List<Word>) {
    val prefs = context.getSharedPreferences("nallanudi_prefs", Context.MODE_PRIVATE)
    val jsonArray = JSONArray()

    words.forEach {
        val obj = JSONObject().apply {
            put("english", it.english)
            put("kannada", it.kannada)
            put("explanation", it.explanation)
            put("subject", it.subject)
        }
        jsonArray.put(obj)
    }

    prefs.edit()
        .putString("saved_words", jsonArray.toString())
        .apply()
}

fun loadWordsFromPrefs(context: Context): MutableList<Word> {
    val prefs = context.getSharedPreferences("nallanudi_prefs", Context.MODE_PRIVATE)
    val jsonString = prefs.getString("saved_words", null) ?: return mutableListOf()

    val jsonArray = JSONArray(jsonString)
    val list = mutableListOf<Word>()

    for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        list.add(
            Word(
                english = obj.getString("english"),
                kannada = obj.getString("kannada"),
                explanation = obj.getString("explanation"),
                subject = obj.getString("subject")
            )
        )
    }

    return list
}

fun loadWordsFromJson(context: Context): List<Word> {
    val list = mutableListOf<Word>()

    try {
        val jsonString = context.assets.open("words.json")
            .bufferedReader()
            .use { it.readText() }

        val jsonArray = JSONArray(jsonString)

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            list.add(
                Word(
                    english = obj.getString("english"),
                    kannada = obj.getString("kannada"),
                    explanation = obj.getString("explanation"),
                    subject = obj.getString("subject")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return list
}