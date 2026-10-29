package com.example.data.repository

import com.example.data.model.TrendingPoliticsItem

object TrendingPoliticsRepository {

    val CATEGORIES = listOf(
        "सबै (All)",
        "हर्क साम्पाङ",
        "बालेन शाह",
        "रवि लामिछाने",
        "संसद बहस",
        "अन्य राजनीति"
    )

    val TRENDING_POLITICS_VIDEOS = listOf(
        TrendingPoliticsItem(
            id = "np_pol_01",
            title = "हर्क साम्पाङको गर्जन: 'श्रमदान रोकिँदैन, धरानले देशलाई पाठ सिकाउँछ!' 🔥🇳🇵",
            leaderCategory = "हर्क साम्पाङ",
            authorHandle = "@harkasampang.official",
            authorName = "Harka Sampang A Revolution",
            coverUrl = "https://images.unsplash.com/photo-1541872703-74c5e44368f9?w=600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            durationText = "1:15",
            viewsCount = "2.8M",
            likesCount = "340K",
            trendingRank = 1,
            badgeLabel = "#1 Trending Nepal",
            facebookTitle = "🇳🇵 धरानका मेयर हर्क साम्पाङको कडा गर्जन! 'हामी जनताको सेवक हौं, मालिक होइनौं!'",
            facebookBody = "धरानको श्रमदान अभियानमा जनताको अपार समर्थन। खोलाबाट ढुङ्गा बोक्दै देश बनाउने नयाँ मोडेल। भाषण होइन, कर्म गरेर देखाउने हर्क साम्पाङको नयाँ सन्देश।",
            hashtags = "#HarkaSampang #Dharan #Shramdaan #NepalPolitics #RajnitiNepal #BalenShah #RabiLamichhane #ViralNepal #NepaliTikTok #DeshBanauneAbhiyan",
            downloadFileName = "Harka_Sampang_Shramdaan_Speech.mp4"
        ),
        TrendingPoliticsItem(
            id = "np_pol_02",
            title = "बालेन शाहको एक्सन: काठमाडौंको ऐतिहासिक सम्पदा र सार्वजनिक जग्गा संरक्षण अभियान! ⚡🏛️",
            leaderCategory = "बालेन शाह",
            authorHandle = "@balenshah.updates",
            authorName = "Balen Shah Supporters",
            coverUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            durationText = "0:58",
            viewsCount = "3.4M",
            likesCount = "480K",
            trendingRank = 2,
            badgeLabel = "#2 Viral Politics",
            facebookTitle = "⚡ बालेन शाहको सम्झौताहीन एक्सन! काठमाडौं महानगरमा कानुन उल्लंघन गर्नेलाई कडा चेतावनी!",
            facebookBody = "सडक पेटी खाली गर्नेदेखि टुकुचा खोला उत्खनन र सार्वजनिक जग्गा फिर्ता ल्याउनेसम्मको ऐतिहासिक कदम। के तपाईं बालेनको एक्सनलाई समर्थन गर्नुहुन्छ? आफ्नो विचार कमेन्टमा लेख्नुहोस्।",
            hashtags = "#BalenShah #Kathmandu #KMC #GoodGovernance #NepalPolitics #MayorBalen #ViralNepal #NepaliTikTok #Rajniti2081 #NepalGovt",
            downloadFileName = "Balen_Shah_Action_Kathmandu.mp4"
        ),
        TrendingPoliticsItem(
            id = "np_pol_03",
            title = "संसदमा रवि लामिछानेका ५ ज्वलन्त प्रश्न: सत्तापक्ष निरुत्तर! 💥🎤",
            leaderCategory = "रवि लामिछाने",
            authorHandle = "@rabikura.nepal",
            authorName = "RSP Media Cell",
            coverUrl = "https://images.unsplash.com/photo-1577962917302-cd874c4e31d2?w=600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
            durationText = "2:10",
            viewsCount = "1.9M",
            likesCount = "290K",
            trendingRank = 3,
            badgeLabel = "#3 Hot Parliament",
            facebookTitle = "💥 संसदको रोष्ट्रमबाट रवि लामिछानेको गर्जन! जनताले तिरेको करको हिसाब माग!",
            facebookBody = "युवाहरुको विदेश पलायन रोक्न र देशमा सुशासन कायम गर्न प्रत्यक्ष संसद बहस। सत्तापक्षलाई प्रत्यक्ष जवाफदेही बनाउने प्रयास। अवश्य शेयर गर्नुहोस्!",
            hashtags = "#RabiLamichhane #RSP #NepalParliament #SamsadBaithak #NepalPolitics #BalenShah #HarkaSampang #BhrastacharBirodh #TrendingNepal #NepaliViral",
            downloadFileName = "Rabi_Lamichhane_Parliament_Debate.mp4"
        ),
        TrendingPoliticsItem(
            id = "np_pol_04",
            title = "हर्क साम्पाङ: 'विदेशी ऋण र अनुदानमा होइन, आफ्नै पसिनामा गर्व गर्न सिकौं!' 🏔️💪",
            leaderCategory = "हर्क साम्पाङ",
            authorHandle = "@dharan.samachar",
            authorName = "Dharan Voice",
            coverUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
            durationText = "1:42",
            viewsCount = "2.1M",
            likesCount = "260K",
            trendingRank = 4,
            badgeLabel = "#4 Trending Speech",
            facebookTitle = "🏔️ हर्क साम्पाङको प्रेरणादायी विचार: 'स्वाभिमान बेचेर देश बन्दैन!'",
            facebookBody = "श्रम र उत्पादनमा आधारित अर्थतन्त्रको वकालत गर्दै धरानका मेयर हर्क साम्पाङ। नेपाली माटो र युवा शक्तिलाई जगाउने शक्तिशाली भनाइ।",
            hashtags = "#HarkaSampang #DharanModel #YouthPower #Swabhiman #NepalPolitics #RajnitiNepal #TrendingTikTok #ViralVideo #DharanWaterProject",
            downloadFileName = "Harka_Sampang_Youth_Speech.mp4"
        ),
        TrendingPoliticsItem(
            id = "np_pol_05",
            title = "संसदमा चर्काचर्की! युवा सांसदहरुको खबरदारी: 'अब पुरानो दलको सिन्डिकेट टिक्दैन' ⚠️🔥",
            leaderCategory = "संसद बहस",
            authorHandle = "@samsad.watch",
            authorName = "Nepal Parliament Watch",
            coverUrl = "https://images.unsplash.com/photo-1540910419892-4a36d2c3266c?w=600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4",
            durationText = "1:55",
            viewsCount = "1.5M",
            likesCount = "180K",
            trendingRank = 5,
            badgeLabel = "#5 Parliament Debate",
            facebookTitle = "🔥 संसदमा प्रत्यक्ष बहस: नयाँ पुस्ताको निर्णायक आवाज!",
            facebookBody = "बजेट अनियमितता र युवाको रोजगारीका सवालमा संसदमा उठेको तीखो आवाज। जनताको पक्षमा उभिने जनप्रतिनिधिको खाँचो छ भन्दै कडा सम्बोधन।",
            hashtags = "#SamsadNews #NepalParliament #YouthInPolitics #NepalPolitics #HotDebate #RajnitiUpdate #BalenShah #HarkaSampang #ViralNews",
            downloadFileName = "Parliament_Hot_Debate_Youth.mp4"
        ),
        TrendingPoliticsItem(
            id = "np_pol_06",
            title = "नेपालको राजनीतिमा नयाँ युग: बालेन, हर्क र रविको प्रभाव कति? 🚀🇳🇵",
            leaderCategory = "अन्य राजनीति",
            authorHandle = "@rajniti.nepal",
            authorName = "Nepali Politics Analysis",
            coverUrl = "https://images.unsplash.com/photo-1494526585095-c41746248156?w=600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            durationText = "2:30",
            viewsCount = "3.1M",
            likesCount = "410K",
            trendingRank = 6,
            badgeLabel = "#6 Analysis Viral",
            facebookTitle = "🚀 नेपालको राजनीतिमा नयाँ युगको सुरुवात! बालेन, हर्क र रविले कसरी बदल्दैछन् सोच?",
            facebookBody = "परम्परागत राजनीतिक दलहरुको असफलतापछि उदाएको नयाँ नेतृत्व। जनता अब कुरामा होइन, परिणाममा विश्वास गर्छन्। तपाईलाई सबैभन्दा मनपर्ने नेता को हुन्?",
            hashtags = "#BalenShah #HarkaSampang #RabiLamichhane #Kathmandu #Dharan #NepalPolitics #RajnitiRevolution #NayaNepal #ViralTikTok #FacebookPost",
            downloadFileName = "Nepal_New_Politics_Wave.mp4"
        ),
        TrendingPoliticsItem(
            id = "np_pol_07",
            title = "रवि लामिछाने: 'हामी डराउँदैनौं, जनताको अदालत सबैभन्दा ठूलो अदालत हो!' ⚡🛡️",
            leaderCategory = "रवि लामिछाने",
            authorHandle = "@rsp.updates",
            authorName = "RSP Official Clips",
            coverUrl = "https://images.unsplash.com/photo-1521737604893-d14cc237f11d?w=600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
            durationText = "1:20",
            viewsCount = "1.7M",
            likesCount = "230K",
            trendingRank = 7,
            badgeLabel = "#7 Direct Statement",
            facebookTitle = "⚡ 'हामी झुक्दैनौं, सत्यको जीत हुन्छ!' रवि लामिछानेको नयाँ उद्घोष!",
            facebookBody = "राजनीतिक प्रतिशोध र घेराबन्दीविरुद्ध दृढतापूर्वक उभिने सन्देश। जनताको समर्थन नै सबैभन्दा ठूलो शक्ति हो।",
            hashtags = "#RabiLamichhane #RSPNepal #NyayaRaSushasan #NepalPolitics #TrendingNow #ViralPost #FacebookReelsNepal #Nepal2081",
            downloadFileName = "Rabi_Lamichhane_Statement.mp4"
        ),
        TrendingPoliticsItem(
            id = "np_pol_08",
            title = "बालेन शाहको विकास मोडल: 'काठमाडौं सफा र व्यवस्थित हुनैपर्छ' 🇳🇵🧹",
            leaderCategory = "बालेन शाह",
            authorHandle = "@balen.vision",
            authorName = "Balen City Vision",
            coverUrl = "https://images.unsplash.com/photo-1477959858617-67f30bc75b82?w=600&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4",
            durationText = "1:05",
            viewsCount = "2.6M",
            likesCount = "390K",
            trendingRank = 8,
            badgeLabel = "#8 Smart City",
            facebookTitle = "🇳🇵 बालेन शाहको सपना: सुन्दर, सफा र व्यवस्थित काठमाडौं!",
            facebookBody = "पैदलयात्रीको अधिकार, सम्पदा संरक्षण र फोहोर व्यवस्थापनमा महानगरको निरन्तर प्रयास। बालेनको कामलाई कस्तो मान्नुहुन्छ?",
            hashtags = "#BalenShah #KathmanduMetropolitan #CleanKathmandu #HeritageNepal #NepalPolitics #MayorAction #TikTokNepal #ViralReels",
            downloadFileName = "Balen_Clean_City_Mission.mp4"
        )
    )

    fun getItemsForCategory(category: String, query: String = ""): List<TrendingPoliticsItem> {
        val baseList = if (category == "सबै (All)" || category.isBlank()) {
            TRENDING_POLITICS_VIDEOS
        } else {
            TRENDING_POLITICS_VIDEOS.filter { it.leaderCategory.equals(category, ignoreCase = true) }
        }

        if (query.isBlank()) return baseList

        val lowerQuery = query.lowercase().trim()
        return baseList.filter {
            it.title.lowercase().contains(lowerQuery) ||
                    it.leaderCategory.lowercase().contains(lowerQuery) ||
                    it.authorName.lowercase().contains(lowerQuery) ||
                    it.hashtags.lowercase().contains(lowerQuery) ||
                    it.facebookTitle.lowercase().contains(lowerQuery)
        }
    }
}
