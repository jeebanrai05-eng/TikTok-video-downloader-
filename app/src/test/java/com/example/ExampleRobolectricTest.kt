package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.DownloadRepository
import com.example.ui.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TikSave", appName)
  }

  @Test
  fun `verify build version is updated to 1_1_4`() {
    assertEquals("1.1.4", BuildConfig.VERSION_NAME)
    assertEquals(9, BuildConfig.VERSION_CODE)
  }

  @Test
  fun `extractTikTokUrl accepts valid TikTok links and rejects non-TikTok platforms`() {
    assertNotNull(DownloadRepository.extractTikTokUrl("https://www.tiktok.com/@creator/video/7321948192384729102"))
    assertNotNull(DownloadRepository.extractTikTokUrl("https://www.tiktok.com/@nepal_travel/photo/7345678910111213141?is_from_webapp=1"))
    assertNotNull(DownloadRepository.extractTikTokUrl("Check this https://vt.tiktok.com/ZSNxAbCdE/"))
    assertNotNull(DownloadRepository.extractTikTokUrl("https://www.tiktok.com/t/ZT8AbCdEf/"))

    assertNull(DownloadRepository.extractTikTokUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertNull(DownloadRepository.extractTikTokUrl("https://www.facebook.com/reel/1234567890"))
  }

  @Test
  fun `formatDuration and detectMimeType handle edge cases`() {
    assertEquals("00:00", FormatUtils.formatDuration(-5))
    assertEquals("01:05", FormatUtils.formatDuration(65))
    assertEquals("image/jpeg", FormatUtils.detectMimeType("/storage/emulated/0/Pictures/TikSave/slide.jpg"))
    assertEquals("audio/mpeg", FormatUtils.detectMimeType("/storage/emulated/0/Music/TikSave/sound.mp3"))
    assertEquals("video/mp4", FormatUtils.detectMimeType("/storage/emulated/0/Movies/TikSave/clip.mp4"))
  }
}
