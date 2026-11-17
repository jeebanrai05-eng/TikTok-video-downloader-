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
  fun `verify build version is updated to 1_1_6`() {
    assertEquals("1.1.6", BuildConfig.VERSION_NAME)
    assertEquals(11, BuildConfig.VERSION_CODE)
  }

  @Test
  fun `extractTikTokUrl normalizes all TikTok link variants and rejects non-TikTok platforms`() {
    assertEquals(
      "https://www.tiktok.com/@creator/video/7321948192384729102",
      DownloadRepository.extractTikTokUrl("https://www.tiktok.com/@creator/video/7321948192384729102")
    )
    assertEquals(
      "https://www.tiktok.com/@/video/7321948192384729102",
      DownloadRepository.extractTikTokUrl("https://www.tiktok.com/@/video/7321948192384729102")
    )
    assertEquals(
      "https://lite.tiktok.com/t/ZSptD2ek8/",
      DownloadRepository.extractTikTokUrl("Watch this video https://lite.tiktok.com/t/ZSptD2ek8/!")
    )
    assertEquals(
      "https://vt.tiktok.com/ZSptD2ek8/",
      DownloadRepository.extractTikTokUrl("vt.tiktok.com/ZSptD2ek8/")
    )
    assertNotNull(DownloadRepository.extractTikTokUrl("https://www.tiktok.com/@nepal_travel/photo/7345678910111213141?is_from_webapp=1"))

    assertNull(DownloadRepository.extractTikTokUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertNull(DownloadRepository.extractTikTokUrl("https://www.facebook.com/reel/1234567890"))
  }

  @Test
  fun `parseAjaxSearchHtml extracts HD MP4, standard MP4, MP3, cover, and unescaped title`() {
    val sampleAjaxHtml = """
      <div class="thumbnail">
        <div class="image-tik">
          <img src="https://p16-common-sign.tiktokcdn-us.com/cover.webp?a=1&amp;b=2">
        </div>
        <div class="content">
          <h3>Scrambled Eggs &#x1F95A; #streetfood</h3>
        </div>
      </div>
      <div class="dl-action">
        <p><a href="https://dl.snapcdn.app/get?token=abc_std" class="tik-button-dl">Download MP4 [1]</a></p>
        <p><a href="https://v16m.tiktokcdn-us.com/vid.mp4?a=1&amp;b=2" class="tik-button-dl">Download MP4 [2]</a></p>
        <p><a href="https://dl.snapcdn.app/get?token=abc_hd" class="tik-button-dl">Download MP4 HD</a></p>
        <p><a href="https://dl.snapcdn.app/get?token=abc_mp3" class="tik-button-dl">Download MP3</a></p>
      </div>
    """.trimIndent()

    val meta = DownloadRepository.parseAjaxSearchHtml(sampleAjaxHtml, "https://vt.tiktok.com/ZSptD2ek8/")
    assertNotNull(meta)
    assertEquals("Scrambled Eggs 🥚 #streetfood", meta!!.title)
    assertEquals("https://p16-common-sign.tiktokcdn-us.com/cover.webp?a=1&b=2", meta.coverUrl)
    assertEquals("https://dl.snapcdn.app/get?token=abc_hd", meta.ultra8kUrl)
    assertEquals("https://dl.snapcdn.app/get?token=abc_std", meta.standardHdUrl)
    assertEquals("https://v16m.tiktokcdn-us.com/vid.mp4?a=1&b=2", meta.normal720pUrl)
    assertEquals("https://dl.snapcdn.app/get?token=abc_mp3", meta.audioUrl)
  }

  @Test
  fun `parseUniversalDataFromHtml extracts streams and photo slides`() {
    val sampleHtml = """
      <html><head></head><body>
      <script id="__UNIVERSAL_DATA_FOR_REHYDRATION__" type="application/json">
      {
        "__DEFAULT_SCOPE__": {
          "webapp.video-detail": {
            "itemInfo": {
              "itemStruct": {
                "id": "7703018206457269524",
                "desc": "Scrambled Eggs Test",
                "author": {
                  "uniqueId": "travelicious88",
                  "nickname": "Travelicious",
                  "avatarLarger": "https://p16.tiktokcdn.com/avatar.jpg"
                },
                "stats": {
                  "playCount": 6400000,
                  "diggCount": 227800,
                  "commentCount": 3553,
                  "shareCount": 57800
                },
                "music": {
                  "title": "Original Sound",
                  "authorName": "Travelicious",
                  "playUrl": "https://v45.tiktokcdn.com/music.mp3"
                },
                "video": {
                  "duration": 50,
                  "originCover": "https://p16.tiktokcdn.com/cover.jpg",
                  "playAddr": "https://v16.tiktok.com/fallback.mp4",
                  "bitrateInfo": [
                    {
                      "Bitrate": 1200000,
                      "PlayAddr": {
                        "DataSize": "6216081",
                        "UrlList": ["https://v16.tiktok.com/hd720.mp4"]
                      }
                    },
                    {
                      "Bitrate": 3200000,
                      "PlayAddr": {
                        "DataSize": "20459218",
                        "UrlList": ["https://v16.tiktok.com/ultra1080.mp4"]
                      }
                    }
                  ]
                }
              }
            }
          }
        }
      }
      </script>
      </body></html>
    """.trimIndent()

    val meta = DownloadRepository.parseUniversalDataFromHtml(
      sampleHtml,
      "https://www.tiktok.com/@travelicious88/video/7703018206457269524"
    )
    assertNotNull(meta)
    assertEquals("Scrambled Eggs Test", meta!!.title)
    assertEquals("@travelicious88", meta.authorUsername)
    assertEquals("https://v16.tiktok.com/ultra1080.mp4", meta.ultra8kUrl)
    assertEquals(20459218L, meta.ultra8kSize)
    assertEquals("https://v16.tiktok.com/hd720.mp4", meta.low480pUrl)
    assertEquals("https://v45.tiktokcdn.com/music.mp3", meta.audioUrl)
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
