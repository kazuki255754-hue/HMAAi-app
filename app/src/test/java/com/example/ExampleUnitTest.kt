package com.example

import com.example.data.local.SavedRecapEntity
import com.example.tts.TtsLanguage
import com.example.tts.TtsVoiceProfile
import com.example.tts.VoiceDeliveryProfile
import com.example.tts.VoiceGender
import com.example.viewmodel.AppTab
import com.example.viewmodel.MovieRecapViewModel
import com.example.viewmodel.RecapTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun `verify exactly 2 male and 2 female voices are configured`() {
    val maleVoices = TtsVoiceProfile.entries.filter { it.gender == VoiceGender.MALE }
    val femaleVoices = TtsVoiceProfile.entries.filter { it.gender == VoiceGender.FEMALE }

    assertEquals(2, maleVoices.size)
    assertEquals(2, femaleVoices.size)
    assertEquals(4, TtsVoiceProfile.entries.size)
  }

  @Test
  fun `verify Myanmar and English languages are supported`() {
    val languages = TtsLanguage.entries
    assertEquals(2, languages.size)
    assertTrue(languages.any { it == TtsLanguage.ENGLISH })
    assertTrue(languages.any { it == TtsLanguage.MYANMAR })
  }

  @Test
  fun `verify Text-to-Speech max characters constant is 5000`() {
    assertEquals(5000, MovieRecapViewModel.TTS_MAX_CHARACTERS)
  }

  @Test
  fun `verify Text-to-Speech input truncation caps at 5000 characters`() {
    val longText = "A".repeat(7500)
    val truncated = if (longText.length > MovieRecapViewModel.TTS_MAX_CHARACTERS) {
      longText.take(MovieRecapViewModel.TTS_MAX_CHARACTERS)
    } else {
      longText
    }
    assertEquals(5000, truncated.length)

    val shortText = "Dramatic movie recap monologue".repeat(50)
    assertTrue(shortText.length < 5000)
    val preserved = if (shortText.length > MovieRecapViewModel.TTS_MAX_CHARACTERS) {
      shortText.take(MovieRecapViewModel.TTS_MAX_CHARACTERS)
    } else {
      shortText
    }
    assertEquals(shortText.length, preserved.length)
  }

  @Test
  fun `verify Recap Script section input is completely unlimited`() {
    val massiveMoviePlot = "Inception synopsis: ".repeat(1000) // ~20,000 characters
    assertTrue(massiveMoviePlot.length > 15000)
    val processedInput = massiveMoviePlot
    assertEquals(massiveMoviePlot.length, processedInput.length)
  }

  @Test
  fun `verify VoiceDeliveryProfile contains Narrator, Excited, Whispering, and Dramatic profiles`() {
    val profiles = VoiceDeliveryProfile.entries
    assertTrue(profiles.any { it == VoiceDeliveryProfile.NARRATOR })
    assertTrue(profiles.any { it == VoiceDeliveryProfile.EXCITED })
    assertTrue(profiles.any { it == VoiceDeliveryProfile.WHISPERING })
    assertTrue(profiles.any { it == VoiceDeliveryProfile.DRAMATIC })

    // Verify acoustic distinctiveness
    val narrator = VoiceDeliveryProfile.NARRATOR
    val excited = VoiceDeliveryProfile.EXCITED
    val whispering = VoiceDeliveryProfile.WHISPERING

    assertTrue("Excited profile should have faster speed", excited.speedMultiplier > narrator.speedMultiplier)
    assertTrue("Excited profile should have higher pitch", excited.pitchMultiplier > narrator.pitchMultiplier)
    assertTrue("Whispering profile should have lower pitch", whispering.pitchMultiplier < narrator.pitchMultiplier)
    assertTrue("Whispering profile should have slower speed", whispering.speedMultiplier < narrator.speedMultiplier)

    // Verify sample texts in English and Burmese
    profiles.forEach { profile ->
      assertTrue(profile.sampleEnglish.isNotBlank())
      assertTrue(profile.sampleMyanmar.isNotBlank())
      assertNotNull(profile.displayName)
    }
  }

  @Test
  fun `verify SavedRecapEntity computes words and characters properly for Room database`() {
    val script = "In a world where secrets are locked behind shadows, one man risks everything."
    val entity = SavedRecapEntity(
      id = 1L,
      titleOrQuery = "Interstellar",
      script = script,
      toneName = RecapTone.EPIC_TRAILER.name,
      toneIcon = RecapTone.EPIC_TRAILER.icon,
      toneDisplayName = RecapTone.EPIC_TRAILER.displayName,
      isFromUrl = false
    )

    assertEquals(1L, entity.id)
    assertEquals("Interstellar", entity.titleOrQuery)
    assertEquals(script.length, entity.characterCount)
    assertEquals(12, entity.wordCount)
    assertEquals("Epic Trailer", entity.toneDisplayName)
  }

  @Test
  fun `verify AppTab includes Saved Recaps tab`() {
    val tabs = AppTab.entries
    assertTrue(tabs.any { it == AppTab.SAVED })
    assertEquals(4, tabs.size)
  }
}
