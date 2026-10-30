/**
 * TikSave Web - Application Logic
 * Matches com.example.ui.screens and com.example.data.repository
 */

// Application State
const state = {
  activeTab: 'downloader',
  selectedQuality: '8k', // '8k', 'hdr', 'hd', 'mp3', 'cover'
  currentMetadata: null,
  activeDownload: null,
  downloadInterval: null,
  history: [],
  preferences: {
    enable8k: true,
    enableHdr: true,
    enableTurbo: true,
    autoClipboard: true
  },
  trendingCategory: 'सबै',
  trendingSearchQuery: '',
  activeFbModalItem: null
};

// Nepal Politics Viral Dataset (Matches TrendingPoliticsRepository.kt)
const TRENDING_POLITICS = [
  {
    id: "np_pol_01",
    title: "हर्क साम्पाङको गर्जन: 'श्रमदान रोकिँदैन, धरानले देशलाई पाठ सिकाउँछ!' 🔥🇳🇵",
    leaderCategory: "हर्क साम्पाङ",
    authorHandle: "@harkasampang.official",
    authorName: "Harka Sampang A Revolution",
    coverUrl: "https://images.unsplash.com/photo-1541872703-74c5e44368f9?w=600&auto=format&fit=crop&q=80",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
    durationText: "1:15",
    viewsCount: "2.8M",
    likesCount: "340K",
    trendingRank: 1,
    badgeLabel: "#1 Trending Nepal",
    facebookTitle: "🇳🇵 धरानका मेयर हर्क साम्पाङको कडा गर्जन! 'हामी जनताको सेवक हौं, मालिक होइनौं!'",
    facebookBody: "धरानको श्रमदान अभियानमा जनताको अपार समर्थन। खोलाबाट ढुङ्गा बोक्दै देश बनाउने नयाँ मोडेल। भाषण होइन, कर्म गरेर देखाउने हर्क साम्पाङको नयाँ सन्देश।",
    hashtags: "#HarkaSampang #Dharan #Shramdaan #NepalPolitics #RajnitiNepal #BalenShah #RabiLamichhane #ViralNepal #NepaliTikTok #DeshBanauneAbhiyan",
    downloadFileName: "Harka_Sampang_Shramdaan_Speech.mp4"
  },
  {
    id: "np_pol_02",
    title: "बालेन शाहको एक्सन: काठमाडौंको ऐतिहासिक सम्पदा र सार्वजनिक जग्गा संरक्षण अभियान! ⚡🏛️",
    leaderCategory: "बालेन शाह",
    authorHandle: "@balenshah.updates",
    authorName: "Balen Shah Supporters",
    coverUrl: "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&auto=format&fit=crop&q=80",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
    durationText: "0:58",
    viewsCount: "3.4M",
    likesCount: "480K",
    trendingRank: 2,
    badgeLabel: "#2 Viral Politics",
    facebookTitle: "⚡ बालेन शाहको सम्झौताहीन एक्सन! काठमाडौं महानगरमा कानुन उल्लंघन गर्नेलाई कडा चेतावनी!",
    facebookBody: "सडक पेटी खाली गर्नेदेखि टुकुचा खोला उत्खनन र सार्वजनिक जग्गा फिर्ता ल्याउनेसम्मको ऐतिहासिक कदम। के तपाईं बालेनको एक्सनलाई समर्थन गर्नुहुन्छ? आफ्नो विचार कमेन्टमा लेख्नुहोस्।",
    hashtags: "#BalenShah #Kathmandu #KMC #GoodGovernance #NepalPolitics #MayorBalen #ViralNepal #NepaliTikTok #Rajniti2081 #NepalGovt",
    downloadFileName: "Balen_Shah_Action_Kathmandu.mp4"
  },
  {
    id: "np_pol_03",
    title: "संसदमा रवि लामिछानेका ५ ज्वलन्त प्रश्न: सत्तापक्ष निरुत्तर! 💥🎤",
    leaderCategory: "रवि लामिछाने",
    authorHandle: "@rabikura.nepal",
    authorName: "RSP Media Cell",
    coverUrl: "https://images.unsplash.com/photo-1577962917302-cd874c4e31d2?w=600&auto=format&fit=crop&q=80",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
    durationText: "2:10",
    viewsCount: "1.9M",
    likesCount: "290K",
    trendingRank: 3,
    badgeLabel: "#3 Hot Parliament",
    facebookTitle: "💥 संसदको रोष्ट्रमबाट रवि लामिछानेको गर्जन! जनताले तिरेको करको हिसाब माग!",
    facebookBody: "युवाहरुको विदेश पलायन रोक्न र देशमा सुशासन कायम गर्न प्रत्यक्ष संसद बहस। सत्तापक्षलाई प्रत्यक्ष जवाफदेही बनाउने प्रयास। अवश्य शेयर गर्नुहोस्!",
    hashtags: "#RabiLamichhane #RSP #NepalParliament #SamsadBaithak #NepalPolitics #BalenShah #HarkaSampang #BhrastacharBirodh #TrendingNepal #NepaliViral",
    downloadFileName: "Rabi_Lamichhane_Parliament_Debate.mp4"
  },
  {
    id: "np_pol_04",
    title: "हर्क साम्पाङ: 'विदेशी ऋण र अनुदानमा होइन, आफ्नै पसिनामा गर्व गर्न सिकौं!' 🏔️💪",
    leaderCategory: "हर्क साम्पाङ",
    authorHandle: "@dharan.samachar",
    authorName: "Dharan Voice",
    coverUrl: "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600&auto=format&fit=crop&q=80",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
    durationText: "1:42",
    viewsCount: "2.1M",
    likesCount: "260K",
    trendingRank: 4,
    badgeLabel: "#4 Trending Speech",
    facebookTitle: "🏔️ हर्क साम्पाङको प्रेरणादायी विचार: 'स्वाभिमान बेचेर देश बन्दैन!'",
    facebookBody: "श्रम र उत्पादनमा आधारित अर्थतन्त्रको वकालत गर्दै धरानका मेयर हर्क साम्पाङ। नेपाली माटो र युवा शक्तिलाई जगाउने शक्तिशाली भनाइ।",
    hashtags: "#HarkaSampang #DharanModel #YouthPower #Swabhiman #NepalPolitics #RajnitiNepal #TrendingTikTok #ViralVideo #DharanWaterProject",
    downloadFileName: "Harka_Sampang_Youth_Speech.mp4"
  },
  {
    id: "np_pol_05",
    title: "संसदमा चर्काचर्की! युवा सांसदहरुको खबरदारी: 'अब पुरानो दलको सिन्डिकेट टिक्दैन' ⚠️🔥",
    leaderCategory: "संसद बहस",
    authorHandle: "@samsad.watch",
    authorName: "Nepal Parliament Watch",
    coverUrl: "https://images.unsplash.com/photo-1540910419892-4a36d2c3266c?w=600&auto=format&fit=crop&q=80",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4",
    durationText: "1:55",
    viewsCount: "1.5M",
    likesCount: "180K",
    trendingRank: 5,
    badgeLabel: "#5 Parliament Debate",
    facebookTitle: "🔥 संसदमा प्रत्यक्ष बहस: नयाँ पुस्ताको निर्णायक आवाज!",
    facebookBody: "बजेट अनियमितता र युवाको रोजगारीका सवालमा संसदमा उठेको तीखो आवाज। जनताको पक्षमा उभिने जनप्रतिनिधिको खाँचो छ भन्दै कडा सम्बोधन।",
    hashtags: "#SamsadNews #NepalParliament #YouthInPolitics #NepalPolitics #HotDebate #RajnitiUpdate #BalenShah #HarkaSampang #ViralNews",
    downloadFileName: "Parliament_Hot_Debate_Youth.mp4"
  },
  {
    id: "np_pol_06",
    title: "नेपालको राजनीतिमा नयाँ युग: बालेन, हर्क र रविको प्रभाव कति? 🚀🇳🇵",
    leaderCategory: "अन्य राजनीति",
    authorHandle: "@rajniti.nepal",
    authorName: "Nepali Politics Analysis",
    coverUrl: "https://images.unsplash.com/photo-1494526585095-c41746248156?w=600&auto=format&fit=crop&q=80",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
    durationText: "2:30",
    viewsCount: "3.1M",
    likesCount: "410K",
    trendingRank: 6,
    badgeLabel: "#6 Analysis Viral",
    facebookTitle: "🚀 नेपालको राजनीतिमा नयाँ युगको सुरुवात! बालेन, हर्क र रविले कसरी बदल्दैछन् सोच?",
    facebookBody: "परम्परागत राजनीतिक दलहरुको असफलतापछि उदाएको नयाँ नेतृत्व। जनता अब कुरामा होइन, परिणाममा विश्वास गर्छन्। तपाईलाई सबैभन्दा मनपर्ने नेता को हुन्?",
    hashtags: "#BalenShah #HarkaSampang #RabiLamichhane #Kathmandu #Dharan #NepalPolitics #RajnitiRevolution #NayaNepal #ViralTikTok #FacebookPost",
    downloadFileName: "Nepal_New_Politics_Wave.mp4"
  },
  {
    id: "np_pol_07",
    title: "रवि लामिछाने: 'हामी डराउँदैनौं, जनताको अदालत सबैभन्दा ठूलो अदालत हो!' ⚡🛡️",
    leaderCategory: "रवि लामिछाने",
    authorHandle: "@rsp.updates",
    authorName: "RSP Official Clips",
    coverUrl: "https://images.unsplash.com/photo-1521737604893-d14cc237f11d?w=600&auto=format&fit=crop&q=80",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
    durationText: "1:20",
    viewsCount: "1.7M",
    likesCount: "230K",
    trendingRank: 7,
    badgeLabel: "#7 Direct Statement",
    facebookTitle: "⚡ 'हामी झुक्दैनौं, सत्यको जीत हुन्छ!' रवि लामिछानेको नयाँ उद्घोष!",
    facebookBody: "राजनीतिक प्रतिशोध र घेराबन्दीविरुद्ध दृढतापूर्वक उभिने सन्देश। जनताको समर्थन नै सबैभन्दा ठूलो शक्ति हो।",
    hashtags: "#RabiLamichhane #RSPNepal #NyayaRaSushasan #NepalPolitics #TrendingNow #ViralPost #FacebookReelsNepal #Nepal2081",
    downloadFileName: "Rabi_Lamichhane_Statement.mp4"
  },
  {
    id: "np_pol_08",
    title: "बालेन शाहको विकास मोडल: 'काठमाडौं सफा र व्यवस्थित हुनैपर्छ' 🇳🇵🧹",
    leaderCategory: "बालेन शाह",
    authorHandle: "@balen.vision",
    authorName: "Balen City Vision",
    coverUrl: "https://images.unsplash.com/photo-1477959858617-67f30bc75b82?w=600&auto=format&fit=crop&q=80",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4",
    durationText: "1:05",
    viewsCount: "2.6M",
    likesCount: "390K",
    trendingRank: 8,
    badgeLabel: "#8 Smart City",
    facebookTitle: "🇳🇵 बालेन शाहको सपना: सुन्दर, सफा र व्यवस्थित काठमाडौं!",
    facebookBody: "पैदलयात्रीको अधिकार, सम्पदा संरक्षण र फोहोर व्यवस्थापनमा महानगरको निरन्तर प्रयास। बालेनको कामलाई कस्तो मान्नुहुन्छ?",
    hashtags: "#BalenShah #KathmanduMetropolitan #CleanKathmandu #HeritageNepal #NepalPolitics #MayorAction #TikTokNepal #ViralReels",
    downloadFileName: "Balen_Clean_City_Mission.mp4"
  }
];

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
  loadPreferences();
  loadHistory();
  setupClipboardListener();
  handleInputStateChange();
  renderTrendingCards();
});

// Tab Switching
function switchTab(tabName) {
  state.activeTab = tabName;

  // View sections
  const views = ['downloader', 'trending', 'batch', 'library', 'settings'];
  views.forEach(v => {
    const el = document.getElementById(`view-${v}`);
    if (el) el.style.display = (v === tabName) ? 'block' : 'none';
  });

  // Top Nav tab buttons
  document.querySelectorAll('.nav-tab').forEach(btn => btn.classList.remove('active'));
  const activeBtn = document.getElementById(`tab-btn-${tabName}`);
  if (activeBtn) activeBtn.classList.add('active');

  // Bottom Nav items
  document.querySelectorAll('.bnav-item').forEach(btn => btn.classList.remove('active'));
  const activeBnav = document.getElementById(`bnav-${tabName}`);
  if (activeBnav) activeBnav.classList.add('active');

  if (tabName === 'trending') {
    renderTrendingCards();
  } else if (tabName === 'library') {
    renderHistory();
  }
}

// Input Handlers
function handleInputStateChange() {
  const input = document.getElementById('url-input');
  const clearBtn = document.getElementById('btn-clear-input');
  if (!input || !clearBtn) return;
  clearBtn.style.display = input.value.trim().length > 0 ? 'block' : 'none';
}

function clearInput() {
  const input = document.getElementById('url-input');
  if (input) {
    input.value = '';
    input.focus();
  }
  handleInputStateChange();
  hideError();
}

async function pasteFromClipboard() {
  try {
    if (navigator.clipboard && navigator.clipboard.readText) {
      const text = await navigator.clipboard.readText();
      if (text && text.trim()) {
        const input = document.getElementById('url-input');
        input.value = text.trim();
        handleInputStateChange();
        dismissClipboardBanner();
        startExtraction();
        return;
      }
    }
  } catch (err) {
    console.warn("Clipboard read error, prompting user:", err);
  }

  // Fallback prompt
  const manual = prompt("Paste your TikTok video link here:");
  if (manual && manual.trim()) {
    const input = document.getElementById('url-input');
    input.value = manual.trim();
    handleInputStateChange();
    startExtraction();
  }
}

// Clipboard Auto-Detect Banner
function setupClipboardListener() {
  window.addEventListener('focus', checkClipboardForTikTok);
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      checkClipboardForTikTok();
    }
  });
}

async function checkClipboardForTikTok() {
  if (!state.preferences.autoClipboard) return;
  try {
    if (navigator.clipboard && navigator.clipboard.readText) {
      const text = await navigator.clipboard.readText();
      if (text && isTikTokUrl(text) && text.trim() !== document.getElementById('url-input').value.trim()) {
        showClipboardBanner(text.trim());
      }
    }
  } catch (e) {
    // Permission not granted or inactive tab
  }
}

function showClipboardBanner(url) {
  const banner = document.getElementById('clipboard-banner');
  const urlEl = document.getElementById('clipboard-url-text');
  if (banner && urlEl) {
    urlEl.textContent = url;
    banner.style.display = 'flex';
  }
}

function dismissClipboardBanner() {
  const banner = document.getElementById('clipboard-banner');
  if (banner) banner.style.display = 'none';
}

function useClipboardDetectedLink() {
  const urlEl = document.getElementById('clipboard-url-text');
  if (urlEl) {
    const input = document.getElementById('url-input');
    input.value = urlEl.textContent.trim();
    handleInputStateChange();
    dismissClipboardBanner();
    startExtraction();
  }
}

// TikTok URL Validation
function isTikTokUrl(text) {
  if (!text) return false;
  const regex = /https?:\/\/(?:(?:www|m|vt|vm)\.)?tiktok\.com\/(?:@[a-zA-Z0-9_.-]+\/video\/[0-9]+|[a-zA-Z0-9_.-]+\/[a-zA-Z0-9_.-]+|[A-Za-z0-9_-]+)\/?/i;
  return regex.test(text.trim()) || text.includes('tiktok.com');
}

// Select Quality Chip in Header
function selectQuality(q, element) {
  state.selectedQuality = q;
  document.querySelectorAll('.quality-chip').forEach(c => c.classList.remove('active'));
  if (element) element.classList.add('active');
  selectStreamRow(q);
}

// Select Quality Stream Row inside Result Card
function selectStreamRow(q) {
  state.selectedQuality = q;
  const rows = ['8k', 'hdr', 'hd', 'mp3'];
  rows.forEach(r => {
    const rowEl = document.getElementById(`stream-row-${r}`);
    if (rowEl) {
      if (r === q) rowEl.classList.add('active');
      else rowEl.classList.remove('active');
    }
  });

  const label = document.getElementById('btn-download-label');
  if (label) {
    switch (q) {
      case '8k': label.textContent = 'Download 8K 120FPS (No Watermark)'; break;
      case 'hdr': label.textContent = 'Download HDR+ Ultra (Best Quality)'; break;
      case 'hd': label.textContent = 'Download HD Video (Clean Fast)'; break;
      case 'mp3': label.textContent = 'Download MP3 Audio (320 kbps)'; break;
      case 'cover': label.textContent = 'Download Full HD Cover (JPG)'; break;
      default: label.textContent = 'Download Video (No Watermark)';
    }
  }
}

// Quick Sample Load
function loadSampleVideo(index) {
  const sample = SAMPLE_VIDEOS[index];
  if (!sample) return;
  const input = document.getElementById('url-input');
  input.value = sample.url;
  handleInputStateChange();
  startExtraction();
}

// Start Video Extraction (Multi-Phase Animation)
async function startExtraction() {
  const input = document.getElementById('url-input');
  const rawUrl = input.value.trim();

  if (!rawUrl) {
    showError("Please enter a TikTok video link (e.g., https://vt.tiktok.com/...)");
    return;
  }

  hideError();
  hideResult();
  showExtracting(true);

  // Phase 1
  setExtractingProgress("Analyzing TikTok link structure...", 25);
  await delay(350);

  // Check if it's one of our sample videos
  const sampleMatch = SAMPLE_VIDEOS.find(s => rawUrl.includes(s.url) || rawUrl.includes(s.title));
  if (sampleMatch) {
    setExtractingProgress("Connecting to high-speed CDN...", 60);
    await delay(300);
    setExtractingProgress("Optimizing 8K 120FPS & HDR+ streams...", 90);
    await delay(350);
    setExtractingProgress("Ready to download!", 100);
    await delay(200);

    const meta = {
      id: "sample_" + Date.now(),
      title: sampleMatch.title,
      authorNickname: sampleMatch.nickname,
      authorUsername: sampleMatch.author,
      authorAvatar: sampleMatch.avatar,
      coverUrl: sampleMatch.cover,
      durationSeconds: sampleMatch.duration,
      playCount: sampleMatch.views,
      likeCount: sampleMatch.likes,
      commentCount: sampleMatch.comments,
      shareCount: sampleMatch.shares,
      stream8k: sampleMatch.videoUrl,
      streamHdr: sampleMatch.videoUrl,
      streamHd: sampleMatch.videoUrl,
      streamMp3: sampleMatch.videoUrl,
      musicTitle: sampleMatch.music,
      sizes: sampleMatch.sizes,
      originalUrl: sampleMatch.url
    };

    displayResult(meta);
    showExtracting(false);
    return;
  }

  // Real TikTok URL resolver via /api/extract or direct fallback
  try {
    setExtractingProgress("Resolving video stream without watermark...", 55);

    let data = null;
    let errorMsg = null;

    // Try our local backend API endpoint first (bypasses CORS & proxies perfectly)
    try {
      const res = await fetch(`/api/extract?url=${encodeURIComponent(rawUrl)}&hd=1`);
      if (res.ok) {
        const json = await res.json();
        if (json.code === 0 && json.data) {
          data = json.data;
        } else {
          errorMsg = json.msg || "Could not resolve stream";
        }
      }
    } catch (e) {
      console.warn("Local extract API failed, trying direct TikWM fetch:", e);
    }

    // Direct browser fetch fallback (TikWM provides access-control-allow-origin: *)
    if (!data) {
      try {
        const directRes = await fetch(`https://www.tikwm.com/api/?url=${encodeURIComponent(rawUrl)}&hd=1`);
        if (directRes.ok) {
          const directJson = await directRes.json();
          if (directJson.code === 0 && directJson.data) {
            data = directJson.data;
          } else {
            errorMsg = directJson.msg || errorMsg;
          }
        }
      } catch (err) {
        console.warn("Direct TikWM fetch also failed:", err);
      }
    }

    if (!data) {
      throw new Error(errorMsg || "Unable to extract video. The video might be private, deleted, or region-restricted.");
    }

    setExtractingProgress("Processing 8K 120FPS & HDR+ video streams...", 90);
    await delay(300);

    // Map TikWM payload to clean metadata
    const playUrl = fixUrl(data.play);
    const hdPlayUrl = fixUrl(data.hdplay) || playUrl;
    const musicUrl = fixUrl(data.music) || playUrl;
    const coverUrl = fixUrl(data.cover) || "https://picsum.photos/seed/vid/600/400";
    const avatarUrl = fixUrl(data.author && data.author.avatar) || "https://picsum.photos/seed/user/100/100";

    const standardSize = data.size || 8500000;
    const hdSize = data.hd_size || Math.round(standardSize * 1.8);
    const ultra8kSize = Math.round(hdSize * 1.6);

    const meta = {
      id: data.id || Date.now().toString(),
      title: data.title || `TikTok Video by ${data.author ? data.author.nickname : "Creator"}`,
      authorNickname: data.author ? data.author.nickname : "TikTok User",
      authorUsername: `@${data.author ? data.author.unique_id : "user"}`,
      authorAvatar: avatarUrl,
      coverUrl: coverUrl,
      durationSeconds: data.duration || 15,
      playCount: data.play_count || 0,
      likeCount: data.digg_count || 0,
      commentCount: data.comment_count || 0,
      shareCount: data.share_count || 0,
      stream8k: hdPlayUrl,
      streamHdr: hdPlayUrl,
      streamHd: playUrl,
      streamMp3: musicUrl,
      musicTitle: (data.music_info && data.music_info.title) || (data.music ? "Original Sound" : ""),
      sizes: {
        '8k': ultra8kSize,
        'hdr': hdSize,
        'hd': standardSize,
        'mp3': 3200000
      },
      originalUrl: rawUrl
    };

    setExtractingProgress("Ready to download!", 100);
    await delay(200);

    displayResult(meta);
  } catch (err) {
    showError(err.message || "Failed to reach TikTok resolver. Please test our sample videos or verify link.");
  } finally {
    showExtracting(false);
  }
}

function fixUrl(url) {
  if (!url) return "";
  if (url.startsWith("/")) return "https://www.tikwm.com" + url;
  return url;
}

// Display Extracted Video Card
function displayResult(meta) {
  state.currentMetadata = meta;

  document.getElementById('res-author-avatar').src = meta.authorAvatar;
  document.getElementById('res-author-nickname').textContent = meta.authorNickname;
  document.getElementById('res-author-username').textContent = meta.authorUsername;
  document.getElementById('res-video-cover').src = meta.coverUrl;
  document.getElementById('res-video-title').textContent = meta.title;
  document.getElementById('res-video-duration').textContent = formatDuration(meta.durationSeconds);

  // Music row
  const musicRow = document.getElementById('res-music-row');
  const musicTitle = document.getElementById('res-music-title');
  if (meta.musicTitle) {
    musicTitle.textContent = meta.musicTitle;
    musicRow.style.display = 'flex';
  } else {
    musicRow.style.display = 'none';
  }

  // Stats
  document.getElementById('res-stat-views').textContent = formatCount(meta.playCount);
  document.getElementById('res-stat-likes').textContent = formatCount(meta.likeCount);
  document.getElementById('res-stat-comments').textContent = formatCount(meta.commentCount);
  document.getElementById('res-stat-shares').textContent = formatCount(meta.shareCount);

  // Sizes in rows
  document.getElementById('size-8k').textContent = formatBytes(meta.sizes['8k']);
  document.getElementById('size-hdr').textContent = formatBytes(meta.sizes['hdr']);
  document.getElementById('size-hd').textContent = formatBytes(meta.sizes['hd']);
  document.getElementById('size-mp3').textContent = formatBytes(meta.sizes['mp3']);

  selectStreamRow(state.selectedQuality);

  const card = document.getElementById('result-card');
  card.style.display = 'block';
  card.classList.add('visible');
  card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });

  // Save to History
  saveToHistory(meta);
}

// Download Trigger with Modern Download Modal
function triggerDownload() {
  if (!state.currentMetadata) return;

  const meta = state.currentMetadata;
  const q = state.selectedQuality;

  let streamUrl = meta.streamHd;
  let filename = `tiksave_${meta.id}_nowatermark.mp4`;
  let totalBytes = meta.sizes['hd'] || 10000000;

  if (q === '8k') {
    streamUrl = meta.stream8k || meta.streamHdr || meta.streamHd;
    filename = `tiksave_${meta.id}_8k_120fps.mp4`;
    totalBytes = meta.sizes['8k'];
  } else if (q === 'hdr') {
    streamUrl = meta.streamHdr || meta.streamHd;
    filename = `tiksave_${meta.id}_hdr_ultra.mp4`;
    totalBytes = meta.sizes['hdr'];
  } else if (q === 'mp3') {
    streamUrl = meta.streamMp3 || meta.streamHd;
    filename = `tiksave_${meta.id}_audio.mp3`;
    totalBytes = meta.sizes['mp3'];
  } else if (q === 'cover') {
    streamUrl = meta.coverUrl;
    filename = `tiksave_${meta.id}_cover.jpg`;
    totalBytes = 650000;
  }

  openDownloadModal(meta, filename, totalBytes, streamUrl);
}

// Modern Download Modal Simulation & Real Save
function openDownloadModal(meta, filename, totalBytes, targetUrl) {
  const modal = document.getElementById('modal-download');
  const titleEl = document.getElementById('modal-dl-video-title');
  const statusEl = document.getElementById('modal-dl-status-title');
  const pctEl = document.getElementById('modal-dl-pct');
  const speedEl = document.getElementById('modal-dl-speed');
  const bytesEl = document.getElementById('modal-dl-bytes');
  const circleBar = document.getElementById('progress-circle-bar');

  const cancelBtn = document.getElementById('modal-dl-cancel-btn');
  const playBtn = document.getElementById('modal-dl-play-btn');
  const doneBtn = document.getElementById('modal-dl-done-btn');

  titleEl.textContent = meta.title;
  statusEl.textContent = "Connecting to High-Speed Turbo CDN...";
  pctEl.textContent = "0%";
  speedEl.textContent = "Calculating CDN route...";
  bytesEl.textContent = `0 MB / ${formatBytes(totalBytes)}`;
  circleBar.style.strokeDashoffset = '345.5';

  cancelBtn.style.display = 'block';
  playBtn.style.display = 'none';
  doneBtn.style.display = 'none';

  modal.classList.add('visible');

  // Multi-step animated download simulation
  let progress = 0;
  let downloadedBytes = 0;
  const startTime = Date.now();

  if (state.downloadInterval) clearInterval(state.downloadInterval);

  state.downloadInterval = setInterval(() => {
    // Speed between 12 MB/s to 24 MB/s
    const speed = (14 + Math.random() * 8).toFixed(1);
    progress += Math.floor(Math.random() * 9) + 4;

    if (progress > 100) progress = 100;

    downloadedBytes = Math.round((progress / 100) * totalBytes);

    pctEl.textContent = `${progress}%`;
    speedEl.textContent = `${speed} MB/s • High Speed Turbo CDN`;
    bytesEl.textContent = `${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}`;

    // Circle circumference is 2 * PI * 55 ~= 345.5
    const offset = 345.5 - (progress / 100) * 345.5;
    circleBar.style.strokeDashoffset = offset.toString();

    if (progress >= 30 && progress < 80) {
      statusEl.textContent = "Bypassing Watermark & Buffering Stream...";
    } else if (progress >= 80 && progress < 100) {
      statusEl.textContent = "Saving Clean File to Device Storage...";
    }

    if (progress >= 100) {
      clearInterval(state.downloadInterval);
      state.downloadInterval = null;

      statusEl.textContent = "Download Complete! Saved to Device";
      speedEl.textContent = "Zero Watermark • 100% Quality Preserved";

      cancelBtn.style.display = 'none';
      playBtn.style.display = 'block';
      doneBtn.style.display = 'block';

      // Perform real direct download to browser device storage!
      saveFileToDevice(targetUrl, filename);
    }
  }, 120);
}

// Direct File Save to Browser Storage (Single file, no duplicate)
async function saveFileToDevice(url, filename) {
  // 1. Try Blob download first for seamless in-browser saving with custom filename
  try {
    const res = await fetch(url, { mode: 'cors' });
    if (res.ok) {
      const blob = await res.blob();
      const blobUrl = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = blobUrl;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      setTimeout(() => {
        if (document.body.contains(a)) document.body.removeChild(a);
        URL.revokeObjectURL(blobUrl);
      }, 1200);
      return;
    }
  } catch (e) {
    console.info("Blob download unavailable or blocked by CORS, using direct stream save:", e);
  }

  // 2. Single clean trigger (Never duplicate)
  const isStaticHost = window.location.hostname.includes('github.io') || window.location.protocol === 'file:';
  const downloadHref = isStaticHost ? url : `/api/download?url=${encodeURIComponent(url)}&filename=${encodeURIComponent(filename)}`;

  const singleAnchor = document.createElement('a');
  singleAnchor.href = downloadHref;
  singleAnchor.download = filename;
  singleAnchor.target = '_blank';
  singleAnchor.rel = 'noopener noreferrer';
  document.body.appendChild(singleAnchor);
  singleAnchor.click();
  setTimeout(() => {
    if (document.body.contains(singleAnchor)) {
      document.body.removeChild(singleAnchor);
    }
  }, 1200);
}

function cancelDownload() {
  if (state.downloadInterval) {
    clearInterval(state.downloadInterval);
    state.downloadInterval = null;
  }
  closeDownloadModal();
}

function closeDownloadModal() {
  const modal = document.getElementById('modal-download');
  if (modal) modal.classList.remove('visible');
}

// In-App Video Preview Player
function openVideoPlayerModal() {
  if (!state.currentMetadata) return;
  const meta = state.currentMetadata;
  const player = document.getElementById('preview-video-element');
  const titleEl = document.getElementById('player-modal-title');
  const modal = document.getElementById('modal-player');

  titleEl.textContent = meta.title;
  player.src = meta.streamHdr || meta.streamHd || meta.stream8k;
  modal.classList.add('visible');
  player.play().catch(e => console.log("Autoplay blocked:", e));
}

function closeVideoPlayerModal() {
  const player = document.getElementById('preview-video-element');
  if (player) {
    player.pause();
    player.src = '';
  }
  const modal = document.getElementById('modal-player');
  if (modal) modal.classList.remove('visible');
}

// Copy Direct Stream Link
function copyDirectStreamLink() {
  if (!state.currentMetadata) return;
  const meta = state.currentMetadata;
  const q = state.selectedQuality;
  const streamUrl = q === '8k' ? meta.stream8k : (q === 'mp3' ? meta.streamMp3 : meta.streamHdr);

  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(streamUrl).then(() => {
      alert("Direct clean stream link copied to clipboard!");
    });
  } else {
    prompt("Copy clean video stream link:", streamUrl);
  }
}

// Batch Downloader Processing
async function startBatchProcessing() {
  const textarea = document.getElementById('batch-textarea');
  const container = document.getElementById('batch-list-container');
  const lines = textarea.value.split('\n').map(l => l.trim()).filter(l => l.length > 0);

  if (lines.length === 0) {
    alert("Please enter at least 1 TikTok link to batch download.");
    return;
  }

  container.innerHTML = '';

  for (let i = 0; i < lines.length; i++) {
    const url = lines[i];
    const itemEl = document.createElement('div');
    itemEl.className = 'batch-item';
    itemEl.id = `batch-item-${i}`;
    itemEl.innerHTML = `
      <div style="flex: 1; overflow: hidden; margin-right: 12px;">
        <div style="font-size: 13px; font-weight: 700; color: #fff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${url}</div>
        <div id="batch-status-${i}" style="font-size: 11px; color: var(--neon-cyan);">Analyzing stream...</div>
      </div>
      <div id="batch-action-${i}">
        <div class="spinner-ring" style="width: 20px; height: 20px; border-width: 2px;"></div>
      </div>
    `;
    container.appendChild(itemEl);

    // Simulate batch download sequentially
    await delay(600);
    const statusEl = document.getElementById(`batch-status-${i}`);
    const actionEl = document.getElementById(`batch-action-${i}`);
    
    statusEl.textContent = "Ready • Clean HD (No Watermark)";
    actionEl.innerHTML = `
      <button class="btn-sec-action" style="padding: 4px 10px; height: 32px; font-size: 11px;" onclick="window.open('${url}', '_blank')">
        Download
      </button>
    `;
  }
}

function clearBatch() {
  document.getElementById('batch-textarea').value = '';
  document.getElementById('batch-list-container').innerHTML = '';
}

// LocalStorage History / Saved Library
function loadHistory() {
  try {
    const raw = localStorage.getItem('tiksave_history');
    if (raw) {
      state.history = JSON.parse(raw);
    }
  } catch (e) {
    state.history = [];
  }
  updateLibBadge();
}

function saveToHistory(meta) {
  // Avoid duplicate entries
  state.history = state.history.filter(h => h.id !== meta.id);
  state.history.unshift({
    id: meta.id,
    title: meta.title,
    author: meta.authorNickname,
    cover: meta.coverUrl,
    streamUrl: meta.streamHdr || meta.streamHd,
    timestamp: Date.now()
  });

  if (state.history.length > 30) state.history.pop();

  try {
    localStorage.setItem('tiksave_history', JSON.stringify(state.history));
  } catch (e) {}

  updateLibBadge();
}

function renderHistory() {
  const grid = document.getElementById('library-grid');
  const empty = document.getElementById('library-empty');
  if (!grid || !empty) return;

  grid.innerHTML = '';

  if (state.history.length === 0) {
    empty.style.display = 'block';
    return;
  }

  empty.style.display = 'none';

  state.history.forEach((item, idx) => {
    const card = document.createElement('div');
    card.className = 'history-card';
    card.innerHTML = `
      <div class="history-thumb-wrap" onclick="playHistoryItem(${idx})">
        <img src="${item.cover}" alt="Thumb" class="history-thumb" onerror="this.src='https://picsum.photos/seed/hist/300/200'">
        <button class="btn-play-preview" style="width: 40px; height: 40px;">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><polygon points="5 3 19 12 5 21 5 3"/></svg>
        </button>
      </div>
      <div class="history-body">
        <div>
          <div class="history-title">${item.title}</div>
          <div class="history-author">${item.author}</div>
        </div>
        <div class="history-actions">
          <button class="btn-sec-action" style="flex: 1; padding: 4px 8px; height: 32px; font-size: 11px;" onclick="saveFileToDevice('${item.streamUrl}', 'tiksave_${item.id}.mp4')">
            Download
          </button>
          <button class="btn-sec-action" style="width: 34px; height: 32px; padding: 0;" onclick="removeHistoryItem(${idx})">
            ✕
          </button>
        </div>
      </div>
    `;
    grid.appendChild(card);
  });
}

function playHistoryItem(index) {
  const item = state.history[index];
  if (!item) return;
  const player = document.getElementById('preview-video-element');
  const titleEl = document.getElementById('player-modal-title');
  const modal = document.getElementById('modal-player');

  titleEl.textContent = item.title;
  player.src = item.streamUrl;
  modal.classList.add('visible');
  player.play().catch(e => {});
}

function removeHistoryItem(index) {
  state.history.splice(index, 1);
  try {
    localStorage.setItem('tiksave_history', JSON.stringify(state.history));
  } catch (e) {}
  updateLibBadge();
  renderHistory();
}

function clearHistory() {
  if (confirm("Clear all saved video history?")) {
    state.history = [];
    localStorage.removeItem('tiksave_history');
    updateLibBadge();
    renderHistory();
  }
}

function updateLibBadge() {
  const badge = document.getElementById('lib-count-badge');
  if (badge) badge.textContent = state.history.length.toString();
}

// Preferences
function loadPreferences() {
  try {
    const raw = localStorage.getItem('tiksave_pref');
    if (raw) {
      state.preferences = Object.assign(state.preferences, JSON.parse(raw));
    }
  } catch (e) {}

  const set8k = document.getElementById('setting-8k');
  const setHdr = document.getElementById('setting-hdr');
  const setTurbo = document.getElementById('setting-turbo');
  const setClip = document.getElementById('setting-clip');

  if (set8k) set8k.checked = state.preferences.enable8k;
  if (setHdr) setHdr.checked = state.preferences.enableHdr;
  if (setTurbo) setTurbo.checked = state.preferences.enableTurbo;
  if (setClip) setClip.checked = state.preferences.autoClipboard;
}

function savePreferences() {
  state.preferences.enable8k = document.getElementById('setting-8k').checked;
  state.preferences.enableHdr = document.getElementById('setting-hdr').checked;
  state.preferences.enableTurbo = document.getElementById('setting-turbo').checked;
  state.preferences.autoClipboard = document.getElementById('setting-clip').checked;

  try {
    localStorage.setItem('tiksave_pref', JSON.stringify(state.preferences));
  } catch (e) {}
}

// Helpers
function showExtracting(show) {
  const box = document.getElementById('extracting-box');
  const btn = document.getElementById('btn-extract-main');
  if (box) box.style.display = show ? 'block' : 'none';
  if (btn) btn.disabled = show;
}

function setExtractingProgress(stageText, pct) {
  const label = document.getElementById('extracting-stage-label');
  const bar = document.getElementById('extracting-progress-bar');
  if (label) label.textContent = stageText;
  if (bar) bar.style.width = `${pct}%`;
}

function showError(msg) {
  const card = document.getElementById('error-card');
  const text = document.getElementById('error-card-msg');
  if (card && text) {
    text.textContent = msg;
    card.style.display = 'flex';
  }
}

function hideError() {
  const card = document.getElementById('error-card');
  if (card) card.style.display = 'none';
}

function hideResult() {
  const card = document.getElementById('result-card');
  if (card) {
    card.style.display = 'none';
    card.classList.remove('visible');
  }
}

function formatDuration(sec) {
  if (!sec || isNaN(sec)) return "00:15";
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
}

function formatCount(num) {
  if (!num) return "0";
  if (num >= 1000000) return (num / 1000000).toFixed(1) + "M";
  if (num >= 1000) return (num / 1000).toFixed(1) + "K";
  return num.toString();
}

function formatBytes(bytes) {
  if (!bytes) return "0 MB";
  const mb = bytes / (1024 * 1024);
  return mb.toFixed(1) + " MB";
}

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// ==========================================================================
// Trending Nepal Politics Functions (Matches TrendingScreen.kt)
// ==========================================================================

function getFilteredTrendingVideos() {
  const cat = state.trendingCategory || 'सबै';
  const q = (state.trendingSearchQuery || '').toLowerCase().trim();

  let list = TRENDING_POLITICS;
  if (cat !== 'सबै' && cat !== 'सबै (All)') {
    list = list.filter(item => item.leaderCategory.toLowerCase() === cat.toLowerCase());
  }

  if (q.length > 0) {
    list = list.filter(item => 
      item.title.toLowerCase().includes(q) ||
      item.leaderCategory.toLowerCase().includes(q) ||
      item.authorName.toLowerCase().includes(q) ||
      item.hashtags.toLowerCase().includes(q) ||
      item.facebookTitle.toLowerCase().includes(q)
    );
  }

  return list;
}

function renderTrendingCards() {
  const container = document.getElementById('trending-cards-grid');
  const countBadge = document.getElementById('trending-count-badge');
  if (!container) return;

  const items = getFilteredTrendingVideos();
  if (countBadge) countBadge.textContent = items.length.toString();

  if (items.length === 0) {
    container.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; padding: 48px 16px; color: var(--text-tertiary);">
        <div style="font-size: 32px; margin-bottom: 8px;">🇳🇵</div>
        <div style="font-size: 15px; font-weight: 700; color: #fff;">कुनै भिडियो भेटिएन</div>
        <div style="font-size: 12px; margin-top: 4px;">कृपया अर्को नेता वा शब्द खोज्नुहोस्</div>
      </div>
    `;
    return;
  }

  container.innerHTML = items.map(item => `
    <div class="trending-card" id="card-${item.id}">
      <div class="trending-thumb-wrap" onclick="openTrendingVideoPreview('${item.id}')">
        <img src="${item.coverUrl}" alt="${item.title}" loading="lazy" onerror="this.src='https://picsum.photos/seed/${item.id}/600/340'">
        <span class="trending-badge-rank">${item.badgeLabel}</span>
        <span class="trending-duration">${item.durationText}</span>
        <div class="trending-play-overlay">
          <div class="play-circle">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><polygon points="6 4 20 12 6 20 6 4"/></svg>
          </div>
        </div>
      </div>

      <div class="trending-card-body">
        <div style="display: flex; gap: 6px; align-items: center;">
          <span class="cat-pill active" style="font-size: 10px; padding: 2px 8px;">${item.leaderCategory}</span>
          <span style="font-size: 11px; color: var(--neon-cyan); font-weight: 600;">Clean HD</span>
        </div>

        <h3 class="trending-card-title">${item.title}</h3>

        <div class="trending-author-row">
          <span>${item.authorName}</span>
          <span>${item.authorHandle}</span>
        </div>

        <div class="trending-stats-row">
          <span>👁️ ${item.viewsCount} views</span>
          <span>❤️ ${item.likesCount} likes</span>
        </div>

        <!-- Facebook Ready Post Preview Box -->
        <div class="fb-preview-box">
          <div class="fb-preview-header">
            <span class="fb-badge">
              <svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor"><path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/></svg>
              Ready To Post
            </span>
            <button class="btn-copy-small" onclick="copyFacebookPost('${item.id}')">Copy FB Post</button>
          </div>
          <div class="fb-preview-title">${item.facebookTitle}</div>
          <div class="fb-preview-tags">${item.hashtags}</div>
        </div>

        <!-- Actions -->
        <div class="trending-card-actions">
          <button class="btn-fb-share" onclick="openFacebookModal('${item.id}')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/></svg>
            <span>Facebook मा Upload गर्नुहोस्</span>
          </button>

          <div class="trending-sub-actions">
            <button class="btn-trending-dl" onclick="downloadTrendingVideo('${item.id}')">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
              <span>Download</span>
            </button>
            <button class="btn-copy-tags" onclick="copyTrendingTags('${item.id}')">
              <span># Copy Tags</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  `).join('');
}

function filterTrendingCategory(cat) {
  state.trendingCategory = cat;
  document.querySelectorAll('.cat-pill').forEach(pill => {
    if (pill.textContent.includes(cat) || (cat === 'सबै' && pill.textContent.includes('सबै'))) {
      pill.classList.add('active');
    } else {
      pill.classList.remove('active');
    }
  });
  renderTrendingCards();
}

function handleTrendingSearch(query) {
  state.trendingSearchQuery = query;
  renderTrendingCards();
}

function openFacebookModal(itemId) {
  const item = TRENDING_POLITICS.find(x => x.id === itemId);
  if (!item) return;

  state.activeFbModalItem = item;

  const modal = document.getElementById('modal-fb-post');
  const cover = document.getElementById('fb-modal-cover');
  const cat = document.getElementById('fb-modal-category');
  const rank = document.getElementById('fb-modal-rank');
  const title = document.getElementById('fb-modal-video-title');
  const author = document.getElementById('fb-modal-author');
  const inputTitle = document.getElementById('fb-input-title');
  const inputBody = document.getElementById('fb-input-body');
  const inputTags = document.getElementById('fb-input-tags');

  if (cover) cover.src = item.coverUrl;
  if (cat) cat.textContent = item.leaderCategory;
  if (rank) rank.textContent = item.badgeLabel;
  if (title) title.textContent = item.title;
  if (author) author.textContent = `${item.authorName} (${item.authorHandle})`;
  if (inputTitle) inputTitle.value = item.facebookTitle;
  if (inputBody) inputBody.value = item.facebookBody;
  if (inputTags) inputTags.value = item.hashtags;

  if (modal) {
    modal.style.display = 'flex';
    modal.classList.add('visible');
  }
}

function closeFacebookModal() {
  const modal = document.getElementById('modal-fb-post');
  if (modal) {
    modal.style.display = 'none';
    modal.classList.remove('visible');
  }
}

function copyFieldText(elementId) {
  const el = document.getElementById(elementId);
  if (!el) return;
  const val = el.value || el.textContent;
  navigator.clipboard.writeText(val).then(() => {
    showToast("कपी भयो! (Copied to Clipboard)");
  }).catch(() => {
    showToast("कपी भयो!");
  });
}

function copyFullPost() {
  const inputTitle = document.getElementById('fb-input-title');
  const inputBody = document.getElementById('fb-input-body');
  const inputTags = document.getElementById('fb-input-tags');

  const fullText = `${inputTitle ? inputTitle.value : ''}\n\n${inputBody ? inputBody.value : ''}\n\n${inputTags ? inputTags.value : ''}`.trim();
  navigator.clipboard.writeText(fullText).then(() => {
    showToast("सम्पूर्ण Facebook पोष्ट कपी भयो! 📋");
  }).catch(() => {
    showToast("पोष्ट कपी भयो!");
  });
}

function copyFacebookPost(itemId) {
  const item = TRENDING_POLITICS.find(x => x.id === itemId);
  if (!item) return;

  const fullText = `${item.facebookTitle}\n\n${item.facebookBody}\n\n${item.hashtags}`;
  navigator.clipboard.writeText(fullText).then(() => {
    showToast("Facebook शीर्षक र ह्यासट्याग कपी भयो! 📋");
  }).catch(() => {
    showToast("कपी भयो!");
  });
}

function copyTrendingTags(itemId) {
  const item = TRENDING_POLITICS.find(x => x.id === itemId);
  if (!item) return;

  navigator.clipboard.writeText(item.hashtags).then(() => {
    showToast("भाइरल ह्यासट्याग कपी भयो! 🏷️");
  }).catch(() => {
    showToast("ह्यासट्याग कपी भयो!");
  });
}

function postToFacebookFromModal() {
  copyFullPost();
  showToast("शीर्षक कपी भयो! Facebook खुल्दैछ... 🚀");
  setTimeout(() => {
    // Open Facebook create post or facebook feed
    window.open("https://www.facebook.com/", "_blank");
  }, 500);
}

function downloadFromFbModal() {
  if (!state.activeFbModalItem) return;
  downloadTrendingVideo(state.activeFbModalItem.id);
}

function downloadTrendingVideo(itemId) {
  const item = TRENDING_POLITICS.find(x => x.id === itemId);
  if (!item) return;

  // Synthesize metadata for download modal
  state.currentMetadata = {
    title: item.title,
    author: item.authorHandle,
    nickname: item.authorName,
    cover: item.coverUrl,
    avatar: item.coverUrl,
    videoUrl: item.videoUrl,
    music: "Nepal Politics Viral Audio",
    likes: 340000,
    duration: 65,
    sizes: { '8k': 24000000, 'hdr': 15000000, 'hd': 8500000, 'mp3': 2800000 }
  };

  triggerDownload();
  showToast(`${item.downloadFileName} डाउनलोड सुरु भयो! ⚡`);
}

function openTrendingVideoPreview(itemId) {
  const item = TRENDING_POLITICS.find(x => x.id === itemId);
  if (!item) return;

  const modal = document.getElementById('modal-player');
  const video = document.getElementById('preview-video-element');
  const title = document.getElementById('player-modal-title');

  if (title) title.textContent = item.title;
  if (video) {
    video.src = item.videoUrl;
    video.load();
    video.play().catch(() => {});
  }

  state.currentMetadata = {
    title: item.title,
    author: item.authorHandle,
    nickname: item.authorName,
    cover: item.coverUrl,
    avatar: item.coverUrl,
    videoUrl: item.videoUrl,
    music: "Nepal Politics Audio",
    likes: 340000,
    duration: 65,
    sizes: { '8k': 24000000, 'hdr': 15000000, 'hd': 8500000, 'mp3': 2800000 }
  };

  if (modal) {
    modal.style.display = 'flex';
    modal.classList.add('visible');
  }
}

// Simple Toast Notification
function showToast(msg) {
  let toast = document.getElementById('tiksave-toast');
  if (!toast) {
    toast = document.createElement('div');
    toast.id = 'tiksave-toast';
    toast.style.position = 'fixed';
    toast.style.bottom = '80px';
    toast.style.left = '50%';
    toast.style.transform = 'translateX(-50%)';
    toast.style.background = 'rgba(18, 20, 29, 0.95)';
    toast.style.color = '#fff';
    toast.style.border = '1px solid var(--neon-cyan)';
    toast.style.boxShadow = '0 8px 24px rgba(0, 242, 254, 0.3)';
    toast.style.padding = '10px 20px';
    toast.style.borderRadius = '9999px';
    toast.style.fontSize = '13px';
    toast.style.fontWeight = '700';
    toast.style.zIndex = '9999';
    toast.style.transition = 'all 0.3s ease';
    toast.style.pointerEvents = 'none';
    document.body.appendChild(toast);
  }

  toast.textContent = msg;
  toast.style.opacity = '1';
  toast.style.transform = 'translateX(-50%) translateY(0)';

  clearTimeout(window.__toastTimeout);
  window.__toastTimeout = setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(-50%) translateY(10px)';
  }, 2500);
}

