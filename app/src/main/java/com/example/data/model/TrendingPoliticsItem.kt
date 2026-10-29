package com.example.data.model

data class TrendingPoliticsItem(
    val id: String,
    val title: String,
    val leaderCategory: String, // "हर्क साम्पाङ", "बालेन शाह", "रवि लामिछाने", "संसद बहस", "अन्य"
    val authorHandle: String,
    val authorName: String,
    val coverUrl: String,
    val videoUrl: String,
    val durationText: String,
    val viewsCount: String,
    val likesCount: String,
    val trendingRank: Int,
    val badgeLabel: String,
    val facebookTitle: String,
    val facebookBody: String,
    val hashtags: String,
    val downloadFileName: String
) {
    val fullFacebookPostContent: String
        get() = """
            $facebookTitle
            
            $facebookBody
            
            🔗 पूरा भिडियो हेर्नुहोस् र शेयर गर्नुहोस्!
            $hashtags
        """.trimIndent()
}
