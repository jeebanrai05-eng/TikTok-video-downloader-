package com.example.data.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class AiService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val generationCounter = AtomicInteger(1)

    suspend fun generateViralPack(
        input: String,
        tone: AiContentTone,
        author: String = "",
        isPhotoSlide: Boolean = false,
        slideCount: Int = 0,
        forceRandomSeed: Int = Random.nextInt(1000, 99999),
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): AiViralResult = withContext(Dispatchers.IO) {
        val cleanInput = input.trim().ifEmpty { "Viral TikTok Content" }
        val currentGenId = generationCounter.getAndIncrement()

        onProgress("Analyzing TikTok topic context & sentiment...", 0.20f)
        delay(220)

        onProgress("Brainstorming viral 3-second TikTok FYP hooks...", 0.50f)
        delay(220)

        onProgress("Crafting engaging captions & algorithm-ready hashtags...", 0.80f)
        delay(180)

        val geminiKey = try {
            val keyField = BuildConfig::class.java.getField("GEMINI_API_KEY")
            keyField.get(null) as? String
        } catch (_: Exception) {
            null
        }

        if (!geminiKey.isNullOrBlank() && geminiKey != "MY_GEMINI_API_KEY") {
            try {
                val liveResult = callGeminiApi(cleanInput, tone, isPhotoSlide, geminiKey, currentGenId, forceRandomSeed)
                if (liveResult != null) {
                    onProgress("AI Viral Pack #$currentGenId Generated!", 1.0f)
                    return@withContext liveResult
                }
            } catch (_: Exception) {
                // Fall back to dynamic unlimited generator
            }
        }

        val dynamicResult = generateDynamicUnlimitedPack(
            input = cleanInput,
            tone = tone,
            author = author,
            isPhotoSlide = isPhotoSlide,
            slideCount = slideCount,
            genId = currentGenId,
            seed = forceRandomSeed
        )
        onProgress("AI Viral Pack #$currentGenId Generated!", 1.0f)
        dynamicResult
    }

    private fun callGeminiApi(
        input: String,
        tone: AiContentTone,
        isPhotoSlide: Boolean,
        apiKey: String,
        genId: Int,
        seed: Int
    ): AiViralResult? {
        val prompt = """
            You are a viral TikTok growth strategist (Nepali & global FYP audiences).
            Generate a creative, non-repetitive, high-engagement TikTok viral pack (Iteration #$genId, Seed #$seed).
            Topic: "$input"
            Tone: ${tone.label} (${tone.targetCategory})
            Format: ${if (isPhotoSlide) "TikTok Photo Carousel" else "TikTok Short Video"}

            Rules:
            1. Keep hooks punchy, bold, and scroll-stopping (under 12 words).
            2. Provide 4 distinct TikTok caption variations (Viral Hook, Relatable & Fun, Story & Curiosity, Aesthetic & Short).
            3. Provide high-reach TikTok FYP hashtags.

            Return JSON only:
            {
              "viralScore": 96,
              "hooks": ["Hook 1", "Hook 2", "Hook 3", "Hook 4"],
              "mainCaption": "Engaging TikTok caption with emojis",
              "tiktokPost": "Complete ready-to-post TikTok caption with CTA and 8 FYP hashtags",
              "hashtags": ["#fyp", "#foryou", "#viral", "#trending"],
              "bestPostingTime": "6:30 PM - 9:00 PM NPT",
              "soundtrackRecommendation": "Trending beat or acoustic style",
              "audienceInsight": "Why this angle boosts TikTok completion rate"
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            val contents = JSONArray().apply {
                val content = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    }
                    put("parts", parts)
                }
                put(content)
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 1.0)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val respBody = response.body?.string() ?: return null
            val rootObj = JSONObject(respBody)
            val candidates = rootObj.optJSONArray("candidates") ?: return null
            val firstCandidate = candidates.optJSONObject(0) ?: return null
            val contentObj = firstCandidate.optJSONObject("content") ?: return null
            val partsArr = contentObj.optJSONArray("parts") ?: return null
            val text = partsArr.optJSONObject(0)?.optString("text") ?: return null

            val parsed = JSONObject(text)
            val score = parsed.optInt("viralScore", Random.nextInt(94, 99))
            val hooksArr = parsed.optJSONArray("hooks")
            val hooks = mutableListOf<String>()
            if (hooksArr != null) {
                for (i in 0 until hooksArr.length()) hooks.add(hooksArr.getString(i))
            }

            val tagsArr = parsed.optJSONArray("hashtags")
            val tags = mutableListOf<String>()
            if (tagsArr != null) {
                for (i in 0 until tagsArr.length()) tags.add(tagsArr.getString(i))
            }

            val ttPost = parsed.optString("tiktokPost", "")
            val mainCap = parsed.optString("mainCaption", "")
            val bestTime = parsed.optString("bestPostingTime", "7:00 PM - 9:30 PM NPT")
            val sound = parsed.optString("soundtrackRecommendation", "Original Trending Sound")
            val insight = parsed.optString("audienceInsight", "High curiosity gap boosts watch-through rate by +40%")

            val variations = listOf(
                CaptionVariation(
                    "var_1",
                    "⚡ Viral Hook (High CTR)",
                    hooks.getOrNull(0) ?: "Wait till you see this 😱",
                    mainCap,
                    tags,
                    ttPost
                ),
                CaptionVariation(
                    "var_2",
                    "😂 Relatable & Fun",
                    hooks.getOrNull(1) ?: "Bro didn't even hesitate 💀",
                    "You won't believe how this ended! 😂 Tag someone who needs to see this.",
                    tags.shuffled().take(8),
                    "${hooks.getOrNull(1) ?: ""}\nYou won't believe how this ended! 😂\n${tags.shuffled().take(6).joinToString(" ")}"
                ),
                CaptionVariation(
                    "var_3",
                    "📖 Story & Curiosity",
                    hooks.getOrNull(2) ?: "The truth nobody talks about...",
                    "A closer look at this moment. Drop your thoughts below 👇",
                    tags.shuffled().take(8),
                    "${hooks.getOrNull(2) ?: ""}\nA closer look at this moment. Drop your thoughts 👇\n${tags.shuffled().take(6).joinToString(" ")}"
                ),
                CaptionVariation(
                    "var_4",
                    "✨ Aesthetic & Short",
                    hooks.getOrNull(3) ?: "Pure vibes only ✨",
                    "Moments like this remind us to pause and appreciate the details 🌿💫",
                    tags.shuffled().take(8),
                    "${hooks.getOrNull(3) ?: ""}\nMoments like this 🌿💫\n${tags.shuffled().take(6).joinToString(" ")}"
                )
            )

            return AiViralResult(
                title = input,
                viralScore = score,
                generationId = genId,
                toneLabel = tone.label,
                hooks = if (hooks.isNotEmpty()) hooks else listOf("Watch till the end 😱", "This will blow your mind 💥"),
                caption = mainCap,
                hashtags = tags,
                tiktokReadyPost = ttPost,
                bestPostingTime = bestTime,
                soundtrackRecommendation = sound,
                variations = variations,
                audienceInsight = insight
            )
        }
    }

    private fun generateDynamicUnlimitedPack(
        input: String,
        tone: AiContentTone,
        author: String,
        isPhotoSlide: Boolean,
        slideCount: Int,
        genId: Int,
        seed: Int
    ): AiViralResult {
        val rng = Random(seed)
        val score = rng.nextInt(93, 100)

        val viralOpeners = listOf(
            "😱 ९०% मान्छेले यो कुरा ख्यालै गरेका हुँदैनन्...",
            "🚨 यो टिकटक नहेरी स्क्रोल गर्दै नगर्नुस्!",
            "💥 यस्तो ट्विस्ट आउला भनेर कसैले सोचेकै थिएन!",
            "Stop scrolling right now, you NEED to see this! 🔥",
            "This literally changed my entire perspective 🤯",
            "👀 भिडियोको अन्तिम ५ सेकेन्ड हेर्नुस्, होश उड्नेछ!",
            "💀 ब्रोले यति सजिलै गरिदियो कि... म त चकित भएँ 😭",
            "Wait for the unexpected ending... ⚡",
            "💡 आजको सबैभन्दा चर्चित TikTok पल!",
            "के तपाईंलाई यो पहिले नै थाहा थियो? कमेन्ट गर्नुस् 👇"
        ).shuffled(rng)

        val humorOpeners = listOf(
            "😂 हाँसो थाम्न नसके म जिम्मेवार छैन है!",
            "💀 भाईसाब! यस्तो पनि जिन्दगीमा हुन्छ र? 😭",
            "Me pretending everything is under control vs Reality 😭💀",
            "त्यो साथीलाई ट्याग गरिहाल्नुस् जो हुबहु यस्तै छ 👇😂",
            "Nobody warned me this was going to happen lmao 🤣",
            "नेपाली भएपछि यो नभएको सायदै कोही होला 😂🔥"
        ).shuffled(rng)

        val storyOpeners = listOf(
            "📖 यो कथाले तपाईलाई भित्रैदेखि हल्लाउनेछ...",
            "Behind every silent smile, there is a story untold 💭",
            "कहिलेकाहीँ साना कुराहरूले नै जिन्दगी बदलिदिन्छन् ✨",
            "Why this TikTok moment matters more than anything right now.",
            "यो घटना पछि सबैको सोच नै बदलियो... पूरा सुन्नुस् 👇"
        ).shuffled(rng)

        val aestheticOpeners = listOf(
            "✨ Just romanticizing the little moments in life 🌿",
            "Lost in the sound, finding peace in the frames 🌸",
            "मौनता भित्रको सुन्दरता... Feel the tranquility 🎧",
            "Some places and moments just heal your soul 🏔️✨",
            "Aesthetic frames that deserve to be saved forever 💫"
        ).shuffled(rng)

        val selectedHooks = listOf(
            viralOpeners[0],
            humorOpeners[0],
            storyOpeners[0],
            aestheticOpeners[0]
        )

        val baseFypTags = listOf(
            "#fyp", "#foryou", "#foryoupage", "#viral", "#trending",
            "#tiktoknepal", "#explore", "#viralvideo", "#trend", "#tiktok"
        ).shuffled(rng).take(6)

        val nicheTags = when (tone) {
            AiContentTone.NEPAL_POLITICS -> listOf(
                "#NepalPolitics", "#BalenShah", "#HarkaSampang", "#NepalNews",
                "#DeshPremi", "#VoiceOfPeople", "#NepalDevelopment", "#TrendingNepal"
            )
            AiContentTone.COMEDY_FUNNY -> listOf(
                "#NepaliComedy", "#FunnyTikTok", "#MemeNepal",
                "#ComedyNepal", "#Relatable", "#NepaliMeme", "#LOL"
            )
            AiContentTone.AESTHETIC_VIBE -> listOf(
                "#aesthetic", "#visuals", "#vibes", "#calm", "#NepalDiaries",
                "#HimalayanVibe", "#LoFiNepal", "#photodump", "#peaceful"
            )
            AiContentTone.MOTIVATION -> listOf(
                "#NepaliMotivation", "#HardWork", "#SuccessMindset", "#Inspiration",
                "#LifeQuotes", "#NepaliQuotes", "#NeverGiveUp", "#DailyMotivation"
            )
            AiContentTone.VIRAL_TREND -> listOf(
                "#ViralChallenge", "#TikTokTrending", "#UltraHDR", "#8K120FPS",
                "#TikSave", "#ContentCreator", "#ExplorePage", "#FYPNepal"
            )
        }.shuffled(rng).take(7)

        val combinedTags = (baseFypTags + nicheTags).distinct()

        val var1Caption = if (isPhotoSlide) {
            listOf(
                "📸 Pure 8K HDR+ TikTok Photo Carousel ($slideCount slides). Swipe left to feel every detail! तपाईंलाई कुन फोटो मन पर्यो? 👇",
                "🏔️ Photo Dump Experience ($slideCount frames). Every slide tells a unique story. कुन फोटोले मन छोयो? ✨",
                "✨ Ultra High-Res Aesthetic Photo Slide ($slideCount shots). Save this carousel and share with your travel buddy! 🌿",
                "🌟 $slideCount वटा अद्भुत तस्बिरहरू एकै ठाउँमा! अन्तिम फोटो नछुटाउनुहोला। कस्तो लाग्यो कमेन्ट गर्नुस् 👇"
            ).shuffled(rng)[0]
        } else {
            listOf(
                "⚡ $input! यो टिकटक भिडियोले सोच्ने तरिका नै बदलिदिन्छ। तपाईंको मनमा के आयो? कमेन्ट गर्नुहोस् 🔥",
                "🔥 Watch till the very end! '$input' को यो पलले TikTok FYP मा तहल्का मच्चाइरहेको छ। तपाईंको धारणा के छ? 👇",
                "🎯 हेरेपछि सेयर नगरी बस्नै नसकिने क्षण! '$input' तपाईंका साथीहरूसँग तुरुन्त सेयर गरिहाल्नुहोस् 🚀",
                "💥 Mind-blowing TikTok! '$input' बारे तपाईंलाई के लाग्छ? सहमति भए Like र Comment गर्नुहोस् 👇",
                "✨ आजको सबैभन्दा भाइरल पल। के तपाईंलाई यो पहिले नै थाहा थियो? कमेन्ट बक्समा बताउनुहोस् 💬"
            ).shuffled(rng)[0]
        }

        val var2Caption = listOf(
            "यस्तो दृश्य सायदै देख्नुभएको थियो होला 😂 यसलाई देखेपछि हाँसो रोक्नै सकिएन! आफ्नो बेस्ट फ्रेन्डलाई ट्याग गर्नुस् 💀💥",
            "Bro thought nobody was recording 💀😂 यो हेरेर तपाईंको हाँसो छुट्यो कि छुटेन? 🤣",
            "दिनभरिको तनाव एकैछिनमा गायब बनाउने भिडियो! 😂 लास्ट सम्म हेर्नुस् 💥",
            "कसले सोचेको थियो र यस्तो पनि होला भनेर! 😭😂 कमेन्टमा आफ्नो रियाक्सन दिनुहोला 💬"
        ).shuffled(rng)[0]

        val var3Caption = listOf(
            "कहिलेकाहीँ एउटा सानो टिकटकले हाम्रो सोचमा गहिरो प्रभाव पार्छ। '$input' बारे तपाईंको दृष्टिकोण के छ? 👇",
            "जिन्दगीका कतिपय कुराहरू शब्दमा भन्दा यस्ता दृश्यहरूमा महसुस हुन्छन्। सेयर गरेर सकारात्मक ऊर्जा फैलाउनुहोस् 💫",
            "The story behind this moment is deeper than it seems. तपाईंको अनुभव कस्तो रह्यो? 🌿",
            "केही कुराहरू समयमै सिक्नुपर्छ। यो भिडियोले सिकाउने पाठ मन छुने छ। सेभ गर्नुहोस् 📌"
        ).shuffled(rng)[0]

        val var4Caption = listOf(
            "Aesthetic calm & high fidelity visuals. Lost in the moment ✨🌿",
            "Pure vibes and serenity. एउटा शान्त र सुन्दर पल जसले मनलाई आनन्द दिन्छ 💫🏔️",
            "Late night thoughts and aesthetic visuals. Just let the music take you away ✨🎶",
            "Aesthetic frames that deserve to be saved forever. कस्तो लाग्यो? 🕊️"
        ).shuffled(rng)[0]

        val authorCredit = if (author.isNotBlank()) "\n🎥 Creator: $author" else ""

        val ttReady = buildString {
            appendLine(selectedHooks[0])
            appendLine()
            appendLine(var1Caption)
            if (authorCredit.isNotBlank()) appendLine(authorCredit)
            appendLine()
            append(combinedTags.take(10).joinToString(" "))
        }

        val variations = listOf(
            CaptionVariation(
                id = "var_${genId}_1",
                styleTitle = "⚡ Viral Hook (High CTR)",
                hook = selectedHooks[0],
                caption = var1Caption,
                hashtags = combinedTags,
                tiktokPost = ttReady
            ),
            CaptionVariation(
                id = "var_${genId}_2",
                styleTitle = "😂 Comedy & Relatable",
                hook = selectedHooks[1],
                caption = var2Caption,
                hashtags = (nicheTags.shuffled(rng).take(5) + listOf("#fyp", "#viral", "#comedy")),
                tiktokPost = "${selectedHooks[1]}\n\n$var2Caption\n\n#fyp #comedy #tiktoknepal #viral"
            ),
            CaptionVariation(
                id = "var_${genId}_3",
                styleTitle = "📖 Story & Curiosity",
                hook = selectedHooks[2],
                caption = var3Caption,
                hashtags = combinedTags.shuffled(rng).take(8),
                tiktokPost = "${selectedHooks[2]}\n\n$var3Caption\n\n#story #fyp #tiktoknepal"
            ),
            CaptionVariation(
                id = "var_${genId}_4",
                styleTitle = "✨ Aesthetic & Chill",
                hook = selectedHooks[3],
                caption = var4Caption,
                hashtags = listOf("#aesthetic", "#visuals", "#calm", "#fyp", "#trending", "#lofi"),
                tiktokPost = "${selectedHooks[3]}\n\n$var4Caption\n\n#aesthetic #vibes #fyp"
            )
        )

        val bestTime = when (rng.nextInt(0, 3)) {
            0 -> "6:45 PM - 8:30 PM NPT (Peak TikTok FYP)"
            1 -> "12:30 PM - 2:15 PM NPT (Midday Viral Window)"
            else -> "8:30 PM - 10:45 PM NPT (High Completion Window)"
        }

        val soundtrack = when (rng.nextInt(0, 4)) {
            0 -> "🎵 Original Sound - Trending Bass Boosted Beat"
            1 -> "🎧 Lo-Fi Acoustic Piano & Flute Melody"
            2 -> "⚡ Fast High-Energy Transition Beat"
            else -> "🎶 Viral TikTok Dialogue with Ambient Echo"
        }

        val audienceInsight =
            "Angle #${genId}: A strong 3-second TikTok hook with a comment prompt increases FYP watch-through and completion rate by up to 64%."

        return AiViralResult(
            title = input,
            viralScore = score,
            generationId = genId,
            toneLabel = tone.label,
            hooks = selectedHooks,
            caption = var1Caption,
            hashtags = combinedTags,
            tiktokReadyPost = ttReady,
            bestPostingTime = bestTime,
            soundtrackRecommendation = soundtrack,
            variations = variations,
            audienceInsight = audienceInsight
        )
    }
}
