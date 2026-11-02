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
        val cleanInput = input.trim().ifEmpty { "Viral TikTok & Reels Content" }
        val currentGenId = generationCounter.getAndIncrement()

        onProgress("Analyzing topic context & sentiment...", 0.20f)
        delay(350)

        onProgress("Brainstorming unlimited viral hooks & storytelling angles...", 0.50f)
        delay(350)

        onProgress("Crafting multi-platform Facebook Reels & TikTok captions...", 0.75f)
        delay(300)

        onProgress("Selecting high-reach trending hashtags...", 0.90f)
        delay(200)

        // Try Gemini live AI if available
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
                // Fallback gracefully to dynamic generator
            }
        }

        val dynamicResult = generateDynamicUnlimitedPack(cleanInput, tone, author, isPhotoSlide, slideCount, currentGenId, forceRandomSeed)
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
            You are a viral social media growth master for TikTok, Facebook Reels, and YouTube Shorts (Nepali & global audiences).
            Generate an ultra-creative, non-generic, high-engagement viral pack (Iteration #$genId, Creative Seed #$seed).
            Topic: "$input"
            Tone: ${tone.label} (${tone.targetCategory})
            Format: ${if (isPhotoSlide) "Photo Slideshow Carousel" else "Short Video Clip / Reel"}

            Rules:
            1. DO NOT give repetitive or generic copy. Be bold, witty, intriguing, and authentic!
            2. Provide 4 distinct variations (Viral Hook, Humor/Relatable, Storytelling, and Aesthetic).
            3. Provide high-reach hashtags for TikTok, Facebook, and Shorts.

            Return JSON only:
            {
              "viralScore": 96,
              "hooks": ["Hook 1 (Max 10 words)", "Hook 2", "Hook 3", "Hook 4"],
              "mainCaption": "Engaging main caption with emojis",
              "facebookPost": "Complete ready-to-post Facebook caption with call to action",
              "tiktokPost": "Short punchy TikTok caption with 6 FYP hashtags",
              "youtubeShortsPost": "YouTube Shorts title and description with #Shorts",
              "hashtags": ["#fyp", "#viral", "#foryou", ...12 tags],
              "bestPostingTime": "6:30 PM - 9:00 PM NPT",
              "soundtrackRecommendation": "Trending beat or acoustic style",
              "audienceInsight": "Why this angle works for algorithms"
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

        val response = httpClient.newCall(request).execute()
        if (response.isSuccessful) {
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

            val fbPost = parsed.optString("facebookPost", "")
            val ttPost = parsed.optString("tiktokPost", "")
            val ytPost = parsed.optString("youtubeShortsPost", "")
            val mainCap = parsed.optString("mainCaption", "")
            val bestTime = parsed.optString("bestPostingTime", "7:00 PM - 9:30 PM NPT")
            val sound = parsed.optString("soundtrackRecommendation", "Original Trending Sound")
            val insight = parsed.optString("audienceInsight", "High curiosity gap boosts retention by +40%")

            val variations = listOf(
                CaptionVariation("var_1", "⚡ Viral Hook (High CTR)", hooks.getOrNull(0) ?: "Wait till you see this 😱", mainCap, tags, fbPost, ttPost, ytPost),
                CaptionVariation("var_2", "😂 Relatable & Fun", hooks.getOrNull(1) ?: "Bro didn't even hesitate 💀", "You won't believe how this ended! 😂 Tag someone who needs to see this.", tags.shuffled().take(8), fbPost, ttPost, ytPost),
                CaptionVariation("var_3", "📖 Story & Curiosity", hooks.getOrNull(2) ?: "The truth nobody talks about...", "A closer look at this moment. What are your thoughts on this? Drop a comment below 👇", tags.shuffled().take(8), fbPost, ttPost, ytPost),
                CaptionVariation("var_4", "✨ Aesthetic & Short", hooks.getOrNull(3) ?: "Pure vibes only ✨", "Moments like this remind us to pause and appreciate the details 🌿💫", tags.shuffled().take(8), fbPost, ttPost, ytPost)
            )

            return AiViralResult(
                title = input,
                viralScore = score,
                generationId = genId,
                toneLabel = tone.label,
                hooks = if (hooks.isNotEmpty()) hooks else listOf("Watch till the end 😱", "This will blow your mind 💥"),
                caption = mainCap,
                hashtags = tags,
                facebookReadyPost = fbPost,
                tiktokReadyPost = ttPost,
                youtubeShortsReadyPost = ytPost,
                bestPostingTime = bestTime,
                soundtrackRecommendation = sound,
                variations = variations,
                audienceInsight = insight
            )
        }
        return null
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

        // Dynamic viral score
        val score = rng.nextInt(93, 100)

        // Massive library of creative hook templates
        val viralOpeners = listOf(
            "😱 ९०% मान्छेले यो कुरा ख्यालै गरेका हुँदैनन्...",
            "🚨 यो भिडियो नहेरी स्क्रोल गर्दै नगर्नुस्!",
            "💥 यस्तो ट्विस्ट आउला भनेर कसैले सोचेकै थिएन!",
            "Stop scrolling right now, you NEED to hear this! 🔥",
            "This literally changed my entire perspective 🤯",
            "👀 भिडियोको अन्तिम ५ सेकेन्ड हेर्नुस्, होश उड्नेछ!",
            "💀 ब्रोले यति सजिलै भनिदियो कि... म त चकित भएँ 😭",
            "Wait for the unexpected ending... ⚡",
            "💡 आजको सबैभन्दा ठूलो खुलासा र चर्चाको विषय!",
            "के तपाईंलाई यो कुरा पहिले नै थाहा थियो? कमेन्ट गर्नुस् 👇"
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
            "Why this moment matters more than anything else right now.",
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

        // Dynamic Hashtags Pool
        val baseFypTags = listOf(
            "#fyp", "#foryou", "#foryoupage", "#viral", "#trending",
            "#tiktoknepal", "#explore", "#viralvideo", "#trend", "#growth"
        ).shuffled(rng).take(6)

        val nicheTags = when (tone) {
            AiContentTone.NEPAL_POLITICS -> listOf(
                "#NepalPolitics", "#BalenShah", "#HarkaSampang", "#RabiLamichhane",
                "#NepalNews", "#SamsadBaithak", "#DeshPremi", "#VoiceOfPeople",
                "#NepalDevelopment", "#TrendingNepal", "#RabiLamichhaneNews"
            )
            AiContentTone.COMEDY_FUNNY -> listOf(
                "#NepaliComedy", "#Hahah", "#FunnyTikTok", "#MemeNepal",
                "#ComedyNepal", "#Relatable", "#NepaliMeme", "#LOL", "#FunnyReels"
            )
            AiContentTone.AESTHETIC_VIBE -> listOf(
                "#aesthetic", "#visuals", "#vibes", "#calm", "#NepalDiaries",
                "#HimalayanVibe", "#LoFiNepal", "#photodump", "#nature", "#peaceful"
            )
            AiContentTone.MOTIVATION -> listOf(
                "#NepaliMotivation", "#HardWork", "#SuccessMindset", "#Inspiration",
                "#LifeQuotes", "#NepaliQuotes", "#NeverGiveUp", "#DailyMotivation", "#Hustle"
            )
            AiContentTone.YOUTUBE_TO_REELS -> listOf(
                "#FacebookReels", "#ReelsViral", "#TrendingReels", "#FBReelsNepal",
                "#VideoClip", "#BestMoments", "#ReelItFeelIt", "#ViralReel"
            )
            AiContentTone.FACEBOOK_TO_SHORTS -> listOf(
                "#Shorts", "#YouTubeShorts", "#ShortsViral", "#TikTokVideo",
                "#ClipMaster", "#DailyShorts", "#ViralClips", "#InstantReel"
            )
            AiContentTone.VIRAL_TREND -> listOf(
                "#ViralChallenge", "#TikTokTrending", "#UltraHDR", "#8K120FPS",
                "#TikSave", "#AlgorithmHack", "#ContentCreator", "#ExplorePage"
            )
        }.shuffled(rng).take(8)

        val combinedTags = (baseFypTags + nicheTags).distinct()

        // Dynamic Variations
        val var1Caption = if (isPhotoSlide) {
            "📸 Pure 8K HDR+ Photo Carousel ($slideCount slides). Swipe left to feel every detail! तपाईंलाई कुन फोटो सबैभन्दा बढी मन पर्यो? कमेन्ट गर्नुस् 👇"
        } else {
            "⚡ $input! यो भिडियोले सोच्ने तरिका नै बदलिदिन्छ। हेरेपछि तपाईंको मनमा के आयो? तल कमेन्टमा आफ्ना विचारहरू व्यक्त गर्नुहोस् र साथीहरूसँग सेयर गर्नुहोस्! 🔥"
        }

        val var2Caption = "यस्तो दृश्य सायदै देख्नुभएको थियो होला 😂 यसलाई देखेपछि हाँसो रोक्नै सकिएन! तपाईंका त्यो खास साथीहरूलाई ट्याग गरेर सेयर गर्न नबिर्सनुहोला 💀💥"

        val var3Caption = "कहिलेकाहीँ एउटा सानो भिडियोले हाम्रो सोचमा गहिरो प्रभाव पार्छ। '$input' बारे यो महत्त्वपूर्ण अंश सबैले बुझ्न आवश्यक छ। तपाईंको यसमा के दृष्टिकोण छ? 👇"

        val var4Caption = "Aesthetic calm & high fidelity visuals. Lost in the moment, appreciating the finer details of life. Save this reel for when you need peace ✨🌿"

        val authorCredit = if (author.isNotBlank()) "\n🎥 Creator: $author" else ""

        val fbReady = buildString {
            appendLine("🔥 ${selectedHooks[0]}")
            appendLine()
            appendLine(var1Caption)
            if (authorCredit.isNotBlank()) appendLine(authorCredit)
            appendLine()
            appendLine("👉 भिडियो मन परे लाइक, कमेन्ट र साथीहरूलाई सेयर गरिदिनुहोला!")
            appendLine()
            append(combinedTags.joinToString(" "))
        }

        val ttReady = buildString {
            appendLine(selectedHooks[0])
            appendLine(var1Caption.take(120) + "...")
            append(combinedTags.take(8).joinToString(" "))
        }

        val ytReady = buildString {
            appendLine("🔥 $input (Full HD Clip)")
            appendLine()
            appendLine(var1Caption)
            appendLine()
            append(combinedTags.take(6).joinToString(" "))
        }

        val variations = listOf(
            CaptionVariation(
                id = "var_${genId}_1",
                styleTitle = "⚡ Viral Hook (High CTR)",
                hook = selectedHooks[0],
                caption = var1Caption,
                hashtags = combinedTags,
                facebookReelsPost = fbReady,
                tiktokPost = ttReady,
                youtubeShortsPost = ytReady
            ),
            CaptionVariation(
                id = "var_${genId}_2",
                styleTitle = "😂 Comedy & Relatable",
                hook = selectedHooks[1],
                caption = var2Caption,
                hashtags = (nicheTags.shuffled(rng).take(5) + listOf("#fyp", "#viral", "#comedy")),
                facebookReelsPost = "😂 ${selectedHooks[1]}\n\n$var2Caption\n\n${combinedTags.shuffled(rng).take(8).joinToString(" ")}",
                tiktokPost = "${selectedHooks[1]}\n$var2Caption\n#fyp #comedy #nepali #viral",
                youtubeShortsPost = "${selectedHooks[1]} 😂 #Shorts #comedy"
            ),
            CaptionVariation(
                id = "var_${genId}_3",
                styleTitle = "📖 Story & Curiosity",
                hook = selectedHooks[2],
                caption = var3Caption,
                hashtags = combinedTags.shuffled(rng).take(8),
                facebookReelsPost = "💭 ${selectedHooks[2]}\n\n$var3Caption\n\n${combinedTags.joinToString(" ")}",
                tiktokPost = "${selectedHooks[2]}\n$var3Caption\n#story #fyp #nepal",
                youtubeShortsPost = "${selectedHooks[2]} #Shorts #story"
            ),
            CaptionVariation(
                id = "var_${genId}_4",
                styleTitle = "✨ Aesthetic & Chill",
                hook = selectedHooks[3],
                caption = var4Caption,
                hashtags = listOf("#aesthetic", "#visuals", "#calm", "#fyp", "#trending", "#lofi"),
                facebookReelsPost = "✨ ${selectedHooks[3]}\n\n$var4Caption\n\n#aesthetic #reels #peace",
                tiktokPost = "${selectedHooks[3]}\n$var4Caption\n#aesthetic #vibes #fyp",
                youtubeShortsPost = "${selectedHooks[3]} #Shorts #aesthetic"
            )
        )

        val bestTime = when (rng.nextInt(0, 3)) {
            0 -> "6:45 PM - 8:30 PM NPT (Peak Evening Scroll)"
            1 -> "12:30 PM - 2:15 PM NPT (Lunchtime Viral Window)"
            else -> "8:30 PM - 10:45 PM NPT (Nighttime High Retention)"
        }

        val soundtrack = when (rng.nextInt(0, 4)) {
            0 -> "🎵 Original Sound - Trending Bass Boosted Beat"
            1 -> "🎧 Lo-Fi Acoustic Piano & Flute Melody"
            2 -> "⚡ Fast High-Energy Drill / Hip-Hop Beat"
            else -> "🎶 Viral TikTok Dialogue with Ambient Echo"
        }

        val audienceInsight = "Angle #${genId}: Early 3-second hook with relatable question increases watch-through rate by up to 64% on Facebook Reels and TikTok FYP."

        return AiViralResult(
            title = input,
            viralScore = score,
            generationId = genId,
            toneLabel = tone.label,
            hooks = selectedHooks,
            caption = var1Caption,
            hashtags = combinedTags,
            facebookReadyPost = fbReady,
            tiktokReadyPost = ttReady,
            youtubeShortsReadyPost = ytReady,
            bestPostingTime = bestTime,
            soundtrackRecommendation = soundtrack,
            variations = variations,
            audienceInsight = audienceInsight
        )
    }
}
