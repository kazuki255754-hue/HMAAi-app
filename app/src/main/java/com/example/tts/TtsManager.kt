package com.example.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class TtsLanguage(val displayName: String, val nativeName: String, val locale: Locale) {
    ENGLISH("English", "English", Locale.US),
    MYANMAR("Myanmar", "မြန်မာစာ", Locale.forLanguageTag("my-MM"))
}

enum class VoiceGender {
    MALE, FEMALE
}

enum class TtsVoiceProfile(
    val id: String,
    val label: String,
    val displayName: String,
    val myanmarName: String,
    val description: String,
    val gender: VoiceGender,
    val basePitch: Float,
    val baseRate: Float,
    val sampleEnglish: String,
    val sampleMyanmar: String
) {
    MALE_1(
        id = "male_1",
        label = "Male 1",
        displayName = "Deep Baritone",
        myanmarName = "အမျိုးသား ၁ (နက်နဲသောအသံ)",
        description = "Deep, authoritative cinematic narration",
        gender = VoiceGender.MALE,
        basePitch = 0.78f,
        baseRate = 0.95f,
        sampleEnglish = "In the shadows of the forgotten realm, the mystery begins.",
        sampleMyanmar = "မှောင်မိုက်သော ကမ္ဘာဟောင်းကြီးထဲတွင် လျှို့ဝှက်ချက် စတင်ခဲ့သည်။"
    ),
    MALE_2(
        id = "male_2",
        label = "Male 2",
        displayName = "Dynamic Storyteller",
        myanmarName = "အမျိုးသား ၂ (သွက်လက်သောအသံ)",
        description = "Energetic, fast-paced action narrator",
        gender = VoiceGender.MALE,
        basePitch = 0.94f,
        baseRate = 1.05f,
        sampleEnglish = "Every second counts in this high-octane race for survival!",
        sampleMyanmar = "စက္ကန့်တိုင်းသည် အသက်ရှင်သန်မှုအတွက် အဆုံးအဖြတ် ဖြစ်သည်။"
    ),
    FEMALE_1(
        id = "female_1",
        label = "Female 1",
        displayName = "Warm Narrator",
        myanmarName = "အမျိုးသမီး ၁ (နူးညံ့သောအသံ)",
        description = "Warm, emotive and immersive story tone",
        gender = VoiceGender.FEMALE,
        basePitch = 1.15f,
        baseRate = 0.98f,
        sampleEnglish = "Every journey begins with a heartbeat that echoes through time.",
        sampleMyanmar = "ခရီးစဉ်တိုင်းသည် နှလုံးသား၏ ခေါ်သံဖြင့် စတင်ခဲ့ပါသည်။"
    ),
    FEMALE_2(
        id = "female_2",
        label = "Female 2",
        displayName = "Crisp Dramatic",
        myanmarName = "အမျိုးသမီး ၂ (ကြည်လင်သောအသံ)",
        description = "Crisp, suspenseful thriller voice",
        gender = VoiceGender.FEMALE,
        basePitch = 1.28f,
        baseRate = 1.05f,
        sampleEnglish = "The final clue was discovered, changing everything forever.",
        sampleMyanmar = "နောက်ဆုံး သဲလွန်စကို တွေ့ရှိခဲ့ပြီး အရာအားလုံး ပြောင်းလဲသွားခဲ့သည်။"
    )
}

enum class VoiceDeliveryProfile(
    val id: String,
    val displayName: String,
    val myanmarName: String,
    val description: String,
    val icon: String,
    val pitchMultiplier: Float,
    val speedMultiplier: Float,
    val toneModulation: Float,
    val sampleEnglish: String,
    val sampleMyanmar: String
) {
    NARRATOR(
        id = "narrator",
        displayName = "Narrator",
        myanmarName = "ဇာတ်ကြောင်းပြော",
        description = "Balanced, cinematic, and steady classic narrator tone",
        icon = "🎙️",
        pitchMultiplier = 1.0f,
        speedMultiplier = 1.0f,
        toneModulation = 0.5f,
        sampleEnglish = "The truth was hidden deep beneath the surface of the quiet city.",
        sampleMyanmar = "အမှန်တရားသည် တိတ်ဆိတ်သော မြို့ပြအောက်ခြေတွင် နက်ရှိုင်းစွာ ဖုံးကွယ်ထားခဲ့သည်။"
    ),
    EXCITED(
        id = "excited",
        displayName = "Excited",
        myanmarName = "စိတ်လှုပ်ရှားဖွယ်",
        description = "High-energy, fast-paced thrill with bright, vivid excitement",
        icon = "⚡",
        pitchMultiplier = 1.25f,
        speedMultiplier = 1.22f,
        toneModulation = 0.85f,
        sampleEnglish = "Unbelievable! Against all impossible odds, they made the ultimate escape!",
        sampleMyanmar = "မယုံနိုင်လောက်အောင်ပါပဲ! မဖြစ်နိုင်တဲ့ အခြေအနေကနေ လွတ်မြောက်သွားခဲ့ပါပြီ!"
    ),
    WHISPERING(
        id = "whispering",
        displayName = "Whispering",
        myanmarName = "တီးတိုးလေသံ",
        description = "Soft, intimate, low-frequency suspense with quiet breathy tone",
        icon = "🤫",
        pitchMultiplier = 0.78f,
        speedMultiplier = 0.86f,
        toneModulation = 0.15f,
        sampleEnglish = "Listen closely... the darkest secrets only speak in hushed whispers.",
        sampleMyanmar = "သေချာနားထောင်ပါ... အမှောင်မိုက်ဆုံး လျှို့ဝှက်ချက်တွေဟာ တီးတိုးလေသံနဲ့သာ စကားပြောပါတယ်။"
    ),
    DRAMATIC(
        id = "dramatic",
        displayName = "Dramatic",
        myanmarName = "ပြင်းထန်သော သရုပ်ဆောင်",
        description = "Heavy emotional gravity, intense cinema trailer pauses",
        icon = "🎭",
        pitchMultiplier = 0.88f,
        speedMultiplier = 0.94f,
        toneModulation = 0.95f,
        sampleEnglish = "In this final hour, every sacrifice will decide the fate of humankind.",
        sampleMyanmar = "ဤနောက်ဆုံးအချိန်တွင် ပေးဆပ်မှုတိုင်းသည် လူသားတို့၏ ကံကြမ္မာကို ဆုံးဖြတ်ပေလိမ့်မည်။"
    )
}

data class TtsState(
    val isInitialized: Boolean = false,
    val isSpeaking: Boolean = false,
    val isPaused: Boolean = false,
    val selectedLanguage: TtsLanguage = TtsLanguage.ENGLISH,
    val selectedVoice: TtsVoiceProfile = TtsVoiceProfile.MALE_1,
    val selectedDeliveryProfile: VoiceDeliveryProfile = VoiceDeliveryProfile.NARRATOR,
    val pitch: Float = 1.0f,
    val speechRate: Float = 1.0f,
    val toneModulation: Float = 0.5f, // 0.0 (Warm/Subtle) to 1.0 (Dramatic/Intense)
    val currentSentenceIndex: Int = 0,
    val totalSentences: Int = 0,
    val engineName: String = "Android TTS",
    val isLanguageSupported: Boolean = true,
    val errorMessage: String? = null
)

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val tag = "TtsManager"
    private var tts: TextToSpeech? = null

    private val _state = MutableStateFlow(TtsState())
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private var sentenceList: List<String> = emptyList()
    private var currentIndex: Int = 0

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts?.defaultEngine ?: "Default Audio Engine"
            applyLanguageAndVoice(_state.value.selectedLanguage, _state.value.selectedVoice)
            setupProgressListener()

            _state.value = _state.value.copy(
                isInitialized = true,
                engineName = engine,
                errorMessage = null
            )
            Log.d(tag, "TTS initialized successfully on engine: $engine")
        } else {
            Log.e(tag, "Failed to initialize TTS engine with status: $status")
            _state.value = _state.value.copy(
                isInitialized = false,
                errorMessage = "TTS engine initialization failed. Ensure TTS engine is installed."
            )
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.value = _state.value.copy(
                    isSpeaking = true,
                    isPaused = false,
                    currentSentenceIndex = currentIndex
                )
            }

            override fun onDone(utteranceId: String?) {
                currentIndex++
                if (currentIndex < sentenceList.size && !_state.value.isPaused) {
                    speakSentence(currentIndex)
                } else {
                    _state.value = _state.value.copy(
                        isSpeaking = false,
                        isPaused = false,
                        currentSentenceIndex = 0
                    )
                    currentIndex = 0
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(tag, "TTS Utterance error for: $utteranceId")
                _state.value = _state.value.copy(
                    isSpeaking = false,
                    isPaused = false,
                    errorMessage = "Playback error occurred"
                )
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(tag, "TTS Utterance error: $errorCode for: $utteranceId")
                _state.value = _state.value.copy(
                    isSpeaking = false,
                    isPaused = false,
                    errorMessage = "TTS error code: $errorCode"
                )
            }
        })
    }

    fun setLanguage(language: TtsLanguage) {
        _state.value = _state.value.copy(selectedLanguage = language)
        applyLanguageAndVoice(language, _state.value.selectedVoice)
    }

    fun setVoiceProfile(voiceProfile: TtsVoiceProfile) {
        _state.value = _state.value.copy(selectedVoice = voiceProfile)
        applyLanguageAndVoice(_state.value.selectedLanguage, voiceProfile)
    }

    fun setDeliveryProfile(profile: VoiceDeliveryProfile, speakPreview: Boolean = false) {
        _state.value = _state.value.copy(
            selectedDeliveryProfile = profile,
            pitch = profile.pitchMultiplier,
            speechRate = profile.speedMultiplier,
            toneModulation = profile.toneModulation
        )
        applyVoiceModulation()
        if (speakPreview) {
            val sample = if (_state.value.selectedLanguage == TtsLanguage.MYANMAR) {
                profile.sampleMyanmar
            } else {
                profile.sampleEnglish
            }
            speakScript(sample)
        }
    }

    fun setPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        _state.value = _state.value.copy(pitch = clamped)
        applyVoiceModulation()
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.0f)
        _state.value = _state.value.copy(speechRate = clamped)
        applyVoiceModulation()
    }

    fun setToneModulation(tone: Float) {
        val clamped = tone.coerceIn(0.0f, 1.0f)
        _state.value = _state.value.copy(toneModulation = clamped)
        applyVoiceModulation()
    }

    fun resetToDefaults() {
        val defaultProfile = VoiceDeliveryProfile.NARRATOR
        _state.value = _state.value.copy(
            selectedDeliveryProfile = defaultProfile,
            pitch = defaultProfile.pitchMultiplier,
            speechRate = defaultProfile.speedMultiplier,
            toneModulation = defaultProfile.toneModulation
        )
        applyVoiceModulation()
    }

    private fun applyLanguageAndVoice(language: TtsLanguage, voiceProfile: TtsVoiceProfile) {
        val langResult = tts?.setLanguage(language.locale)
        val supported = langResult != TextToSpeech.LANG_MISSING_DATA && langResult != TextToSpeech.LANG_NOT_SUPPORTED

        if (!supported) {
            Log.w(tag, "Language ${language.displayName} not fully supported by TTS engine.")
        }

        // Try to match available system Voice by gender if engine provides voice packs
        try {
            val availableVoices = tts?.voices
            if (!availableVoices.isNullOrEmpty()) {
                val matchedVoice = findMatchingSystemVoice(availableVoices, language.locale, voiceProfile.gender)
                if (matchedVoice != null) {
                    tts?.voice = matchedVoice
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "Voice matching exception: ${e.message}")
        }

        _state.value = _state.value.copy(isLanguageSupported = supported)
        applyVoiceModulation()
    }

    private fun findMatchingSystemVoice(voices: Set<Voice>, locale: Locale, gender: VoiceGender): Voice? {
        val matchingLocale = voices.filter { it.locale.language == locale.language }
        if (matchingLocale.isEmpty()) return null

        val genderKey = if (gender == VoiceGender.MALE) "male" else "female"
        return matchingLocale.firstOrNull { it.name.lowercase().contains(genderKey) }
            ?: matchingLocale.firstOrNull()
    }

    private fun applyVoiceModulation() {
        val voice = _state.value.selectedVoice
        val userPitch = _state.value.pitch
        val userRate = _state.value.speechRate
        val tone = _state.value.toneModulation

        // Calculate final effective pitch combining voice profile, user pitch slider, and dramatic tone modulation
        val toneShift = (tone - 0.5f) * 0.15f
        val effectivePitch = (voice.basePitch * userPitch + toneShift).coerceIn(0.5f, 2.0f)

        // Calculate final effective rate combining voice profile and user speed slider
        val effectiveRate = (voice.baseRate * userRate).coerceIn(0.5f, 2.0f)

        tts?.setPitch(effectivePitch)
        tts?.setSpeechRate(effectiveRate)
    }

    fun previewVoice(voice: TtsVoiceProfile) {
        setVoiceProfile(voice)
        val sampleText = if (_state.value.selectedLanguage == TtsLanguage.MYANMAR) {
            voice.sampleMyanmar
        } else {
            voice.sampleEnglish
        }
        speakScript(sampleText)
    }

    fun testCurrentVoice() {
        val voice = _state.value.selectedVoice
        val sampleText = if (_state.value.selectedLanguage == TtsLanguage.MYANMAR) {
            voice.sampleMyanmar
        } else {
            voice.sampleEnglish
        }
        speakScript(sampleText)
    }

    fun speakScript(script: String) {
        if (!_state.value.isInitialized) {
            _state.value = _state.value.copy(errorMessage = "TTS engine is still loading...")
            return
        }

        stop()

        val cleanedText = cleanScriptForSpeech(script)
        val sentences = cleanedText.split(Regex("(?<=[.!?\\n။])\\s*|(?<=[၊])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (sentences.isEmpty()) return

        sentenceList = sentences
        currentIndex = 0
        _state.value = _state.value.copy(
            totalSentences = sentences.size,
            currentSentenceIndex = 0,
            isPaused = false
        )

        speakSentence(0)
    }

    private fun speakSentence(index: Int) {
        if (index !in sentenceList.indices) return
        val sentence = sentenceList[index]
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "recap_sentence_$index")
        }
        tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, params, "recap_sentence_$index")
    }

    fun pause() {
        if (_state.value.isSpeaking) {
            _state.value = _state.value.copy(isSpeaking = false, isPaused = true)
            tts?.stop()
        }
    }

    fun resume() {
        if (_state.value.isPaused && sentenceList.isNotEmpty() && currentIndex < sentenceList.size) {
            _state.value = _state.value.copy(isSpeaking = true, isPaused = false)
            speakSentence(currentIndex)
        }
    }

    fun stop() {
        _state.value = _state.value.copy(
            isSpeaking = false,
            isPaused = false,
            currentSentenceIndex = 0
        )
        currentIndex = 0
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun cleanScriptForSpeech(raw: String): String {
        return raw
            .replace(Regex("#+\\s*"), "")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("\\*([^*]+)\\*"), "$1")
            .replace(Regex("\\[[^\\]]+\\]"), "")
            .replace(Regex("[🎬🍿🎭📢✨💥⚡🕵️😏]"), "")
            .trim()
    }
}
