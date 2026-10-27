package com.example

import com.example.data.repository.DownloadRepository
import com.example.ui.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testFormatBytes() {
        assertEquals("0 B", FormatUtils.formatBytes(0))
        assertEquals("1.0 KB", FormatUtils.formatBytes(1024))
        assertEquals("1.5 MB", FormatUtils.formatBytes(1572864))
    }

    @Test
    fun testFormatCount() {
        assertEquals("950", FormatUtils.formatCount(950))
        assertEquals("1.5K", FormatUtils.formatCount(1500))
        assertEquals("2.4M", FormatUtils.formatCount(2400000))
    }

    @Test
    fun testFormatDuration() {
        assertEquals("00:45", FormatUtils.formatDuration(45))
        assertEquals("02:15", FormatUtils.formatDuration(135))
    }

    @Test
    fun testExtractTikTokUrl() {
        val fullUrl = "https://www.tiktok.com/@creator/video/7321948192384729102"
        assertEquals(fullUrl, DownloadRepository.extractTikTokUrl(fullUrl))

        val textWithLink = "Hey check this out! https://vm.tiktok.com/ZMxxxxxx/ it's crazy"
        val extracted = DownloadRepository.extractTikTokUrl(textWithLink)
        assertNotNull(extracted)
    }
}
