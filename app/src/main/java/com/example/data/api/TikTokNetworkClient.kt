package com.example.data.api

import okhttp3.ConnectionPool
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Shared high-speed network stack for TikSave:
 * - IPv4-preferred DNS to eliminate mobile carrier IPv6 SYN timeouts.
 * - Shared CookieJar capturing TikTok session cookies (ttwid, tt_chain_token, msToken)
 *   for fast, 403-free CDN previews and downloads.
 */
object TikTokNetworkClient {

    const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    const val MOBILE_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    val ipv4FirstDns: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val addresses = Dns.SYSTEM.lookup(hostname)
            return addresses.sortedWith(compareBy<InetAddress> { if (it is Inet4Address) 0 else 1 })
        }
    }

    private val cookieStore = ConcurrentHashMap<String, ConcurrentHashMap<String, Cookie>>()

    val sharedCookieJar: CookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val host = url.host.lowercase()
            val rootDomain = extractRootDomain(host)
            val map = cookieStore.getOrPut(rootDomain) { ConcurrentHashMap() }
            cookies.forEach { cookie ->
                map[cookie.name] = cookie
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val host = url.host.lowercase()
            val now = System.currentTimeMillis()
            val result = mutableListOf<Cookie>()
            cookieStore.forEach { (domain, cookies) ->
                if (host == domain || host.endsWith(".$domain")) {
                    cookies.values.forEach { cookie ->
                        if (cookie.expiresAt >= now) {
                            result.add(cookie)
                        }
                    }
                }
            }
            return result
        }
    }

    fun cookieHeaderFor(urlString: String): String? {
        val parsed = runCatching { okhttp3.HttpUrl.Builder().apply {
            val uri = java.net.URI(urlString)
            scheme(uri.scheme ?: "https")
            host(uri.host ?: return null)
        }.build() }.getOrNull() ?: return null

        val cookies = sharedCookieJar.loadForRequest(parsed)
        if (cookies.isEmpty()) return null
        return cookies.joinToString("; ") { "${it.name}=${it.value}" }
    }

    private fun extractRootDomain(host: String): String {
        val parts = host.split('.')
        return if (parts.size >= 2) {
            "${parts[parts.size - 2]}.${parts[parts.size - 1]}"
        } else {
            host
        }
    }

    private val sharedConnectionPool = ConnectionPool(12, 5, TimeUnit.MINUTES)

    val fastApiClient: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(sharedConnectionPool)
        .dns(ipv4FirstDns)
        .cookieJar(sharedCookieJar)
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val downloadClient: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(sharedConnectionPool)
        .dns(ipv4FirstDns)
        .cookieJar(sharedCookieJar)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
}
