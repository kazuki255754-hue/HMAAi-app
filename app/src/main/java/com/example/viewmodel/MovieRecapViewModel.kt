package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ContentItem
import com.example.data.GeminiClient
import com.example.data.GenerateContentRequest
import com.example.data.GenerationConfig
import com.example.data.PartItem
import com.example.data.local.AppDatabase
import com.example.data.local.RecapRepository
import com.example.data.local.SavedRecapEntity
import com.example.tts.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class RecapTone(val displayName: String, val icon: String, val promptDescription: String) {
    EPIC_TRAILER(
        displayName = "Epic Trailer",
        icon = "🎬",
        promptDescription = "Massive Hollywood blockbusters, booming deep narrator voice, explosive stakes, high drama ('In a world...')"
    ),
    FILM_NOIR(
        displayName = "Dark Noir",
        icon = "🕵️",
        promptDescription = "Moody 1940s film noir detective style, cynical monologue, smoky jazz vibes, rain-soaked streets, poetic pessimism"
    ),
    FAST_YOUTUBE(
        displayName = "Fast YouTube",
        icon = "🍿",
        promptDescription = "Fast-paced, hyper-engaging viral YouTube movie recap channel voice, punchy colloquial slang, high energy"
    ),
    SUSPENSE_THRILLER(
        displayName = "Thriller",
        icon = "⚡",
        promptDescription = "Nail-biting psychological suspense, breathless urgency, ticking clock, chilling atmosphere"
    ),
    WITTY_SARCASTIC(
        displayName = "Witty Cinema",
        icon = "😏",
        promptDescription = "Satirical, sharp-witted commentary poking fun at Hollywood tropes while narrating the plot brilliantly"
    )
}

data class MovieRecap(
    val id: String = UUID.randomUUID().toString(),
    val titleOrQuery: String,
    val script: String,
    val tone: RecapTone,
    val isFromUrl: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AppTab(val title: String) {
    RECAP("Recap Script"),
    TTS("Text-to-Speech"),
    SAVED("Saved Recaps"),
    API_KEY("Gemini API")
}

class MovieRecapViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences("cinerecap_settings", Context.MODE_PRIVATE)

    val ttsManager = TtsManager(application)

    // Room Database Repository
    val recapRepository: RecapRepository =
        RecapRepository(AppDatabase.getDatabase(application).recapDao())

    val savedRecaps: StateFlow<List<SavedRecapEntity>> = recapRepository.allRecaps
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _showHistoryBottomSheet = MutableStateFlow(false)
    val showHistoryBottomSheet: StateFlow<Boolean> = _showHistoryBottomSheet.asStateFlow()

    private val _currentTab = MutableStateFlow(AppTab.RECAP)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _inputQuery = MutableStateFlow("Inception")
    val inputQuery: StateFlow<String> = _inputQuery.asStateFlow()

    private val _selectedTone = MutableStateFlow(RecapTone.EPIC_TRAILER)
    val selectedTone: StateFlow<RecapTone> = _selectedTone.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentRecap = MutableStateFlow<MovieRecap?>(null)
    val currentRecap: StateFlow<MovieRecap?> = _currentRecap.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _statusNotice = MutableStateFlow<String?>(null)
    val statusNotice: StateFlow<String?> = _statusNotice.asStateFlow()

    // API Key Management
    private val _customApiKey = MutableStateFlow(prefs.getString("pref_custom_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _apiKeyInput = MutableStateFlow(_customApiKey.value)
    val apiKeyInput: StateFlow<String> = _apiKeyInput.asStateFlow()

    private val _isTestingApiKey = MutableStateFlow(false)
    val isTestingApiKey: StateFlow<Boolean> = _isTestingApiKey.asStateFlow()

    private val _testApiResult = MutableStateFlow<String?>(null)
    val testApiResult: StateFlow<String?> = _testApiResult.asStateFlow()

    companion object {
        const val TTS_MAX_CHARACTERS = 5000
    }

    private val _ttsInputText = MutableStateFlow("")
    val ttsInputText: StateFlow<String> = _ttsInputText.asStateFlow()

    init {
        // Preload default sample recap so the app is immediately ready for TTS testing
        val initialScript = buildDefaultRecap("Inception", RecapTone.EPIC_TRAILER)
        val initialRecap = MovieRecap(
            titleOrQuery = "Inception",
            script = initialScript,
            tone = RecapTone.EPIC_TRAILER
        )
        _currentRecap.value = initialRecap
        setTtsInputText(initialScript)

        // Seed initial recap into Room database if the database is currently empty
        viewModelScope.launch(Dispatchers.IO) {
            if (recapRepository.getCount() == 0) {
                recapRepository.insert(
                    SavedRecapEntity(
                        titleOrQuery = "Inception",
                        script = initialScript,
                        toneName = RecapTone.EPIC_TRAILER.name,
                        toneIcon = RecapTone.EPIC_TRAILER.icon,
                        toneDisplayName = RecapTone.EPIC_TRAILER.displayName,
                        isFromUrl = false
                    )
                )
            }
        }
    }

    /**
     * Updates the Text-to-Speech text, strictly capped at a maximum of 5,000 characters.
     */
    fun setTtsInputText(text: String) {
        _ttsInputText.value = if (text.length > TTS_MAX_CHARACTERS) {
            text.take(TTS_MAX_CHARACTERS)
        } else {
            text
        }
    }

    fun clearTtsInputText() {
        _ttsInputText.value = ""
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    /**
     * Updates the Recap Script query/prompt. Completely unlimited (no character limit).
     */
    fun setInputQuery(query: String) {
        _inputQuery.value = query
    }

    fun setSelectedTone(tone: RecapTone) {
        _selectedTone.value = tone
    }

    fun clearInput() {
        _inputQuery.value = ""
    }

    fun clearCurrentRecap() {
        ttsManager.stop()
        _currentRecap.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun setApiKeyInput(input: String) {
        _apiKeyInput.value = input
        _testApiResult.value = null
    }

    fun saveCustomApiKey(key: String) {
        val trimmed = key.trim()
        _customApiKey.value = trimmed
        _apiKeyInput.value = trimmed
        prefs.edit().putString("pref_custom_api_key", trimmed).apply()
        _statusNotice.value = if (trimmed.isNotEmpty()) {
            "Custom Google AI Studio API Key saved successfully!"
        } else {
            "Custom API Key removed."
        }
        _testApiResult.value = null
    }

    fun clearCustomApiKey() {
        saveCustomApiKey("")
    }

    fun getEffectiveApiKey(): String {
        val custom = _customApiKey.value.trim()
        if (custom.isNotEmpty()) return custom

        val buildConfigKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
        if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
            return buildConfigKey
        }
        return ""
    }

    fun testApiKeyConnection() {
        val key = getEffectiveApiKey()
        if (key.isBlank()) {
            _testApiResult.value = "Please enter and save an API key first."
            return
        }

        _isTestingApiKey.value = true
        _testApiResult.value = null

        viewModelScope.launch {
            try {
                val request = GenerateContentRequest(
                    contents = listOf(
                        ContentItem(parts = listOf(PartItem(text = "Reply with 'OK'.")))
                    ),
                    generationConfig = GenerationConfig(
                        maxOutputTokens = 10
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    GeminiClient.service.generateContent(key, request)
                }

                if (response.candidates?.isNotEmpty() == true) {
                    _testApiResult.value = "Success: Connected to Gemini 3.5 Flash!"
                } else {
                    _testApiResult.value = "Connected, but received empty response."
                }
            } catch (e: Exception) {
                _testApiResult.value = "Connection failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isTestingApiKey.value = false
            }
        }
    }

    fun generateRecap() {
        val query = _inputQuery.value.trim()
        if (query.isEmpty()) {
            _errorMessage.value = "Please enter a movie link, title, or plot summary first."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        _statusNotice.value = null
        ttsManager.stop()

        val isUrl = isLink(query)
        val cleanDisplayName = resolveMovieTitle(query, isUrl)

        viewModelScope.launch {
            try {
                val apiKey = getEffectiveApiKey()
                val isPlaceholderOrEmpty = apiKey.isBlank()

                if (isPlaceholderOrEmpty) {
                    val demoScript = withContext(Dispatchers.Default) {
                        buildCinematicScript(cleanDisplayName, _selectedTone.value)
                    }
                    val recap = MovieRecap(
                        titleOrQuery = cleanDisplayName,
                        script = demoScript,
                        tone = _selectedTone.value,
                        isFromUrl = isUrl
                    )
                    onRecapGenerated(recap)
                    _statusNotice.value = "Recap generated & saved to local database (Demo Engine). Add your Gemini key in the Gemini API tab for live AI models."
                } else {
                    val systemInstruction = ContentItem(
                        parts = listOf(
                            PartItem(
                                text = "You are a legendary Hollywood cinematic trailer narrator and dramatic movie recap author. " +
                                        "Write a thrilling, vivid, and suspenseful movie recap script formatted for audio narration. " +
                                        "Style requirements: Tone is ${selectedTone.value.displayName} (${selectedTone.value.promptDescription}). " +
                                        "Keep it concise yet deeply cinematic (around 180 to 250 words). " +
                                        "Avoid formatting that sounds bad when read aloud (do not include markdown headers like '###' or meta brackets like '[Cut to]'). " +
                                        "Deliver pure dramatic spoken prose that grips the listener from the opening word to the final breath."
                            )
                        )
                    )

                    val userPrompt = if (isUrl) {
                        "The user provided this movie link/URL: \"$query\". Identify the film or movie referenced in this URL or link slug, analyze its premise and plot, and write an epic dramatic movie recap script for it in the style of ${selectedTone.value.displayName}."
                    } else {
                        "Write a dramatic movie recap for: \"$query\" in the style of ${selectedTone.value.displayName}."
                    }

                    val request = GenerateContentRequest(
                        contents = listOf(
                            ContentItem(parts = listOf(PartItem(text = userPrompt)))
                        ),
                        generationConfig = GenerationConfig(
                            temperature = 0.85f,
                            topP = 0.95f,
                            topK = 40,
                            maxOutputTokens = 1000
                        ),
                        systemInstruction = systemInstruction
                    )

                    val response = withContext(Dispatchers.IO) {
                        GeminiClient.service.generateContent(apiKey, request)
                    }

                    val candidateText = response.candidates
                        ?.firstOrNull()
                        ?.content
                        ?.parts
                        ?.firstOrNull()
                        ?.text

                    if (!candidateText.isNullOrBlank()) {
                        val cleanScript = candidateText.trim()
                        val recap = MovieRecap(
                            titleOrQuery = cleanDisplayName,
                            script = cleanScript,
                            tone = _selectedTone.value,
                            isFromUrl = isUrl
                        )
                        onRecapGenerated(recap)
                        _statusNotice.value = "Recap generated & saved to local database with Gemini 3.5 Flash!"
                    } else {
                        val fallback = buildCinematicScript(cleanDisplayName, _selectedTone.value)
                        val recap = MovieRecap(
                            titleOrQuery = cleanDisplayName,
                            script = fallback,
                            tone = _selectedTone.value,
                            isFromUrl = isUrl
                        )
                        onRecapGenerated(recap)
                        _statusNotice.value = "Recap generated & saved to local database successfully."
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "API notice: ${e.localizedMessage ?: "Network error"}. Showing backup script."
                val fallback = buildCinematicScript(cleanDisplayName, _selectedTone.value)
                val recap = MovieRecap(
                    titleOrQuery = cleanDisplayName,
                    script = fallback,
                    tone = _selectedTone.value,
                    isFromUrl = isUrl
                )
                onRecapGenerated(recap)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun onRecapGenerated(recap: MovieRecap) {
        _currentRecap.value = recap
        setTtsInputText(recap.script)
        viewModelScope.launch(Dispatchers.IO) {
            recapRepository.insert(
                SavedRecapEntity(
                    titleOrQuery = recap.titleOrQuery,
                    script = recap.script,
                    toneName = recap.tone.name,
                    toneIcon = recap.tone.icon,
                    toneDisplayName = recap.tone.displayName,
                    isFromUrl = recap.isFromUrl,
                    timestamp = recap.timestamp
                )
            )
        }
    }

    private fun isLink(text: String): Boolean {
        val lower = text.lowercase()
        return lower.startsWith("http://") ||
                lower.startsWith("https://") ||
                lower.contains(".com/") ||
                lower.contains(".org/") ||
                lower.contains("imdb.") ||
                lower.contains("youtu") ||
                lower.contains("rottentomatoes") ||
                lower.contains("letterboxd")
    }

    private fun resolveMovieTitle(input: String, isUrl: Boolean): String {
        if (!isUrl) return input
        // Clean URL to display readable title if possible
        try {
            val trimmed = input.trim().removeSuffix("/")
            val lastSegment = trimmed.substringAfterLast("/")
                .replace("-", " ")
                .replace("_", " ")
            if (lastSegment.isNotBlank() && !lastSegment.startsWith("tt") && lastSegment.length > 2) {
                return lastSegment.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
        } catch (_: Exception) {}
        return "Movie from Link"
    }

    fun playAudio() {
        val script = _ttsInputText.value.ifBlank { _currentRecap.value?.script }
        if (script.isNullOrBlank()) {
            _errorMessage.value = "Please type or paste text in the Text-to-Speech tab first."
            return
        }
        ttsManager.speakScript(script)
    }

    fun pauseAudio() {
        ttsManager.pause()
    }

    fun resumeAudio() {
        ttsManager.resume()
    }

    fun stopAudio() {
        ttsManager.stop()
    }

    fun resetTtsControls() {
        ttsManager.resetToDefaults()
        _statusNotice.value = "Voice speed, pitch, and modulation reset to normal."
    }

    fun setTtsDeliveryProfile(profile: com.example.tts.VoiceDeliveryProfile, preview: Boolean = false) {
        ttsManager.setDeliveryProfile(profile, speakPreview = preview)
        _statusNotice.value = "Voice profile changed to ${profile.displayName}."
    }

    fun previewDeliveryProfile(profile: com.example.tts.VoiceDeliveryProfile) {
        ttsManager.setDeliveryProfile(profile, speakPreview = true)
    }

    fun previewTtsVoice(voice: com.example.tts.TtsVoiceProfile) {
        ttsManager.previewVoice(voice)
    }

    fun testTtsVoice() {
        ttsManager.testCurrentVoice()
    }

    fun setShowHistoryBottomSheet(show: Boolean) {
        _showHistoryBottomSheet.value = show
    }

    fun deleteSavedRecap(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            recapRepository.delete(id)
            _statusNotice.value = "Recap removed from local database."
        }
    }

    fun clearAllSavedRecaps() {
        viewModelScope.launch(Dispatchers.IO) {
            recapRepository.clearAll()
            _statusNotice.value = "All saved recaps cleared from local database."
        }
    }

    fun loadSavedRecap(saved: SavedRecapEntity, andPlayTts: Boolean = false) {
        val matchedTone = RecapTone.entries.find { it.name == saved.toneName } ?: RecapTone.EPIC_TRAILER
        val loadedRecap = MovieRecap(
            titleOrQuery = saved.titleOrQuery,
            script = saved.script,
            tone = matchedTone,
            isFromUrl = saved.isFromUrl,
            timestamp = saved.timestamp
        )
        _currentRecap.value = loadedRecap
        _inputQuery.value = saved.titleOrQuery
        _selectedTone.value = matchedTone
        setTtsInputText(saved.script)
        _showHistoryBottomSheet.value = false

        if (andPlayTts) {
            selectTab(AppTab.TTS)
            playAudio()
            _statusNotice.value = "Playing '${saved.titleOrQuery}' in Text-to-Speech Studio"
        } else {
            selectTab(AppTab.RECAP)
            _statusNotice.value = "Loaded saved recap '${saved.titleOrQuery}'"
        }
    }

    fun saveCurrentRecapManually() {
        val current = _currentRecap.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            recapRepository.insert(
                SavedRecapEntity(
                    titleOrQuery = current.titleOrQuery,
                    script = current.script,
                    toneName = current.tone.name,
                    toneIcon = current.tone.icon,
                    toneDisplayName = current.tone.displayName,
                    isFromUrl = current.isFromUrl,
                    timestamp = current.timestamp
                )
            )
            _statusNotice.value = "Recap saved to Room database!"
        }
    }

    fun loadSample(movie: String) {
        _inputQuery.value = movie
        val script = buildCinematicScript(movie, _selectedTone.value)
        val recap = MovieRecap(
            titleOrQuery = movie,
            script = script,
            tone = _selectedTone.value
        )
        onRecapGenerated(recap)
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }

    private fun buildCinematicScript(title: String, tone: RecapTone): String {
        return when (tone) {
            RecapTone.EPIC_TRAILER -> """
                In a world where secrets are locked behind shadows and fragile memories, one name echoes through the silence: $title.
                
                It begins with an ordinary premise, but in minutes, reality begins to unravel. The stakes are no longer just survival; they are existence itself. 
                
                As alliances shatter and the clock relentlessly counts down toward zero, our protagonist is forced into an impossible corner. Every breath costs a confession, and every revelation twists the knife deeper.
                
                When the dust finally settles and the shocking truth detonates, you will ask yourself: Was any of it real, or were we trapped inside the illusion all along?
            """.trimIndent()

            RecapTone.FILM_NOIR -> """
                The city was weeping cold rain, and trouble walked in wearing the name $title.
                
                Nobody ever starts a mystery looking to lose their soul, but here we were, digging through ashes and half-truths under the dim flicker of neon lights.
                
                Every lead turned into a double-cross. The dame had secrets, the cops were looking the other way, and the shadow waiting at the end of the alley held all the aces.
                
                By the time the smoke clears, there are no heroes left standing in this town. Just memories, broken glass, and an echo of what might have been.
            """.trimIndent()

            RecapTone.FAST_YOUTUBE -> """
                Alright movie fans, buckle up, because today we are breaking down $title, and trust me, you are not ready for this rollercoaster!
                
                Our main character wakes up thinking it is going to be a normal Tuesday, but boom! An insane turn of events pulls them headfirst into absolute chaos. 
                
                Rules? Broken. Physics? Completely ignored. Just when you think they finally have an escape plan figured out, the movie drops the ultimate plot twist that changes literally everything.
                
                Did you catch that ending, or did your brain short-circuit like mine? Smash that replay button!
            """.trimIndent()

            RecapTone.SUSPENSE_THRILLER -> """
                Listen closely. The silence surrounding $title is not peace; it is the calm right before the storm strikes.
                
                One wrong move, one whisper in the dark, and everything spirals out of control. With every ticking second, the walls close in tighter, and paranoia becomes the only trusted companion.
                
                Who can you trust when the enemy wears the face of a friend? In the final harrowing minutes, the mask is torn away, revealing a nightmare worse than anything imagined.
                
                Sleep with the lights on tonight. Because this story does not end when the screen goes black.
            """.trimIndent()

            RecapTone.WITTY_SARCASTIC -> """
                Welcome to $title, the cinematic masterpiece where rational decisions go to die a spectacular death.
                
                Our fearless protagonist encounters a problem that any sensible human would avoid by simply staying home. But of course, they march right into danger with maximum dramatic flair.
                
                Between convoluted plot armor and speeches delivered in pure gravelly whispers, the tension ramps up to ridiculous heights. And the grand finale? An absolute symphony of chaos that somehow manages to make complete sense.
                
                Ten out of ten, would question reality again.
            """.trimIndent()
        }
    }

    private fun buildDefaultRecap(title: String, tone: RecapTone): String {
        return """
            In a world where the human mind is the ultimate battlefield, $title takes us deep into the labyrinth of subconscious desires.
            
            Dom Cobb is a master thief of the impossible: stealing corporate secrets from within dreams. But now, he is offered the forbidden reverse: Inception. Planting an idea so deep in a man's psyche that he believes it is his own.
            
            Descending through four cascading layers of dreams within dreams, gravity bends, cities fold, and the relentless ghosts of Cobb's past threaten to pull the entire team into limbo forever.
            
            As the spinning totem turns on the table in the final heartbeat of the film, we are left suspended between fantasy and reality. An unforgettable cinematic symphony.
        """.trimIndent()
    }
}
