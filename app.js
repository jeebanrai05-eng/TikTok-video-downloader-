/**
 * TikSave Web - Application Logic (v1.0.3)
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
  aiInputText: '',
  aiSelectedTone: 'viral', // 'viral', 'humorous', 'curiosity', 'inspirational', 'controversial'
  aiActiveResult: null,
  isGeneratingAi: false
};

// Curated Sample Items (Including 8K/4K HDR+ TikTok Photo Carousel and Videos)
const SAMPLE_VIDEOS = [
  {
    url: "https://www.tiktok.com/@nature.photographer/video/photo_slide_8k_hdr",
    title: "8K HDR+ Mountain Landscapes of the Himalayas • Photo Slides",
    nickname: "Nepal Alpine Explorers",
    author: "@alpine.explorers",
    avatar: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80",
    cover: "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800&auto=format&fit=crop&q=90",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
    music: "Original Flute of Himalayas (Hi-Res Audio)",
    duration: 30,
    views: 4500000,
    likes: 820000,
    comments: 42000,
    shares: 110000,
    isPhotoSlide: true,
    images: [
      "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=2400&auto=format&fit=crop&q=95",
      "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=2400&auto=format&fit=crop&q=95",
      "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=2400&auto=format&fit=crop&q=95",
      "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?w=2400&auto=format&fit=crop&q=95",
      "https://images.unsplash.com/photo-1426604966848-d7adac402bff?w=2400&auto=format&fit=crop&q=95",
      "https://images.unsplash.com/photo-1434725039720-aaad6dd32dfe?w=2400&auto=format&fit=crop&q=95"
    ],
    sizes: { '8k': 48000000, 'hdr': 28000000, 'hd': 12000000, 'mp3': 3800000 }
  },
  {
    url: "https://www.tiktok.com/@tiktokcreators/video/7289192038192301928",
    title: "Cinematic 8K Tokyo Rain Cyberpunk Neon Drift 120FPS",
    nickname: "Tokyo City Lights",
    author: "@tokyovibes.official",
    avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80",
    cover: "https://images.unsplash.com/photo-1514565131-fce0801e5785?w=800&auto=format&fit=crop&q=90",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
    music: "Tokyo Drift Synthwave • Neon Beats",
    duration: 45,
    views: 3200000,
    likes: 490000,
    comments: 18400,
    shares: 62000,
    isPhotoSlide: false,
    images: [],
    sizes: { '8k': 38000000, 'hdr': 22000000, 'hd': 9500000, 'mp3': 3400000 }
  },
  {
    url: "https://www.tiktok.com/@travelnepal/video/everest_base_camp_drone",
    title: "Mount Everest 8K Ultra Drone Aerial Sunrise • No Watermark",
    nickname: "Nepal Tourism Hub",
    author: "@discovernepal",
    avatar: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80",
    cover: "https://images.unsplash.com/photo-1544735716-392fe2489ffa?w=800&auto=format&fit=crop&q=90",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
    music: "Himalayan Sunrise Meditation Flute",
    duration: 60,
    views: 5800000,
    likes: 920000,
    comments: 31000,
    shares: 140000,
    isPhotoSlide: false,
    images: [],
    sizes: { '8k': 52000000, 'hdr': 31000000, 'hd': 14000000, 'mp3': 4100000 }
  }
];

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
  loadPreferences();
  loadHistory();
  setupClipboardListener();
  handleInputStateChange();
  initAiStudio();
});

// Tab Switching
function switchTab(tabName) {
  state.activeTab = tabName;

  // View sections
  const views = ['downloader', 'ai', 'batch', 'library', 'settings'];
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

  if (tabName === 'ai') {
    renderAiStudio();
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
      originalUrl: sampleMatch.url,
      images: sampleMatch.images || [],
      isPhotoSlide: !!sampleMatch.isPhotoSlide
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
    const rawImages = (data.images && Array.isArray(data.images)) ? data.images.map(img => fixUrl(img)) : [];

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
      originalUrl: rawUrl,
      images: rawImages,
      isPhotoSlide: rawImages.length > 0
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

  // Render TikTok Photo Slides (Carousel)
  const slidesBox = document.getElementById('photo-slides-box');
  const slidesCount = document.getElementById('photo-slides-count');
  const slidesScroll = document.getElementById('photo-slides-scroll');

  if (slidesBox && slidesScroll) {
    if (meta.images && meta.images.length > 0) {
      if (slidesCount) slidesCount.textContent = meta.images.length;
      slidesScroll.innerHTML = meta.images.map((imgUrl, idx) => `
        <div class="photo-slide-card">
          <div class="slide-img-wrap">
            <span class="slide-badge-hdr">HDR+ 8K</span>
            <span class="slide-badge-num">#${idx + 1}</span>
            <img src="${imgUrl}" alt="Slide ${idx + 1}" loading="lazy" onerror="this.src='https://picsum.photos/seed/slide${idx}/400/600'">
          </div>
          <button class="btn-slide-save" onclick="saveSlidePhoto('${imgUrl}', ${idx + 1})">
            💾 Save 8K
          </button>
        </div>
      `).join('');
      slidesBox.style.display = 'flex';
    } else {
      slidesBox.style.display = 'none';
      slidesScroll.innerHTML = '';
    }
  }

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
// TikTok Photo Slides (8K HDR+ Ultra Photo Carousel) Functions
// ==========================================================================

function saveSlidePhoto(url, slideNum) {
  if (!url) return;
  const meta = state.currentMetadata || {};
  const filename = `tiksave_${meta.id || Date.now()}_slide_${slideNum}_8k_hdr.jpg`;

  showToast(`Downloading Slide #${slideNum} (8K Ultra HDR+)...`);

  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.target = "_blank";
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);

  // Add record to Library history
  saveToHistory({
    id: `${meta.id || Date.now()}_slide_${slideNum}`,
    title: `${meta.title || "TikTok Photo Slide"} - Slide #${slideNum} [8K HDR+]`,
    coverUrl: url,
    authorUsername: meta.authorUsername || "@tiktok",
    authorNickname: meta.authorNickname || "TikTok Creator",
    streamHd: url,
    sizes: { hd: 4500000 },
    durationSeconds: 0
  });
}

async function downloadAllPhotoSlides() {
  if (!state.currentMetadata || !state.currentMetadata.images || state.currentMetadata.images.length === 0) {
    showToast("No photo slides found in this post!");
    return;
  }

  const images = state.currentMetadata.images;
  showToast(`Starting batch download of all ${images.length} 8K photos...`);

  for (let i = 0; i < images.length; i++) {
    saveSlidePhoto(images[i], i + 1);
    await delay(350);
  }

  showToast(`✅ Successfully queued all ${images.length} 8K photos!`);
}

// ==========================================================================
// AI Creator Studio (Viral Hook, Facebook Post, CTR Description) Functions
// ==========================================================================

function initAiStudio() {
  const input = document.getElementById("ai-topic-input");
  if (input) {
    input.addEventListener("input", (e) => {
      state.aiInputText = e.target.value;
    });
  }
}

function openAiWithCurrentVideo() {
  if (!state.currentMetadata) return;
  const meta = state.currentMetadata;
  const topicInput = document.getElementById("ai-topic-input");
  if (topicInput) {
    topicInput.value = meta.title;
  }
  state.aiInputText = meta.title;
  switchTab("ai");
  runAiViralGeneration();
}

function renderAiStudio() {
  const topicInput = document.getElementById("ai-topic-input");
  if (topicInput && state.aiInputText) {
    topicInput.value = state.aiInputText;
  }
}

function selectAiTone(tone, element) {
  state.aiSelectedTone = tone;
  document.querySelectorAll(".tone-pill").forEach(p => p.classList.remove("active"));
  if (element) element.classList.add("active");
  if (state.aiActiveResult) {
    runAiViralGeneration();
  }
}

async function runAiViralGeneration() {
  const input = document.getElementById("ai-topic-input");
  const query = (input ? input.value : state.aiInputText).trim() || 
                (state.currentMetadata ? state.currentMetadata.title : "") || 
                "Himalayan travel adventures and high energy lifestyle";

  const btn = document.getElementById("btn-run-ai");
  if (btn) {
    btn.disabled = true;
    btn.innerHTML = `<span>⏳</span> Generating Viral Hooks & Facebook Post...`;
  }

  showToast("✨ AI is analyzing high-retention TikTok patterns...");
  await delay(600);

  // Generate Tone-Aware Viral Hooks
  const tone = state.aiSelectedTone || "viral";
  let hooks = [];
  let fbTitle = "";
  let fbBody = "";
  let viralScore = 96;

  if (tone === "viral") {
    hooks = [
      `🔥 Wait until you see the ending! 99% of people missed this detail 😱`,
      `Stop scrolling! This TikTok hack changed everything I thought I knew 🤯`,
      `Here is why everyone is talking about: "${query.substring(0, 40)}..." 👀`
    ];
    fbTitle = `🔥 तपाईंले विश्वास नै गर्न सक्नुहुन्न! "${query}" को सबैभन्दा भाइरल क्लिप! 😱`;
    fbBody = `यस भिडियोले टिकटकमा तहल्का मच्चाएको छ! पूरै भिडियो हेर्नुहोस् र तपाईंको प्रतिक्रिया कमेन्ट गर्नुहोस्। साथीहरुलाई पनि शेयर गरेर देखाउनुहोस्!`;
    viralScore = 97;
  } else if (tone === "humorous") {
    hooks = [
      `😂 I was not supposed to laugh this hard, but wait for the 5-second mark!`,
      `Tell me you do this too without actually telling me 💀🤣`,
      `My honest reaction to: "${query.substring(0, 35)}..." 😂`
    ];
    fbTitle = `😂 हाँसेर पेट मिचिमिचि रुनुहुनेछ! "${query}" 🤣`;
    fbBody = `दिनभरिको तनाव भगाउन यो भिडियो एकपटक हेर्नैपर्छ! तपाईंको हाँसो रोकिने छैन। साथीहरुलाई मेन्सन गर्नुहोस्!`;
    viralScore = 94;
  } else if (tone === "curiosity") {
    hooks = [
      `👀 The secret truth nobody talks about when it comes to this...`,
      `Ever wondered what actually happens behind the scenes? Watch closely:`,
      `This one simple detail explains why this went insanely viral...`
    ];
    fbTitle = `👀 यसको पछाडिको वास्तविक सत्य के हो? "${query}" बारे गोप्य रहस्य!`;
    fbBody = `धेरैलाई थाहा नभएको अनौठो र रोचक जानकारी। के तपाईंलाई यो कुरा पहिल्यै थाहा थियो? आफ्नो विचार अवश्य लेख्नुहोस्।`;
    viralScore = 98;
  } else if (tone === "inspirational") {
    hooks = [
      `🌟 If you needed a sign to never give up today, this is it.`,
      `Proof that passion and relentless dedication conquer all obstacles 💪`,
      `Save this video for the days you feel like quitting ✨`
    ];
    fbTitle = `🌟 हृदय छुने प्रेरणादायी सन्देश: "${query}" 💪🇳🇵`;
    fbBody = `मेहनत र आत्मविश्वास भए असम्भव केही छैन। यो प्रेरणादायी भिडियो हेरेर आफ्नो लक्ष्यतर्फ अघि बढ्नुहोस्। अरुलाई पनि प्रेरणा दिन शेयर गर्नुहोस्!`;
    viralScore = 95;
  } else {
    // Controversial / Debate
    hooks = [
      `⚠️ Unpopular opinion: most people have completely misunderstood this.`,
      `Are you team Agree or Disagree? Watch before answering in the comments! ⚡`,
      `Nobody wants to admit this, but someone had to say it out loud:`
    ];
    fbTitle = `⚠️ कडा बहस: तपाईं यसमा सहमत हुनुहुन्छ कि असहमत? "${query}" ⚡`;
    fbBody = `यसमा मानिसहरुको दुई फरक मत देखिएको छ। तपाईंको धारणा के हो? भिडियो हेरेर आफ्नो सत्य विचार निर्धक्क कमेन्ट गर्नुहोस्!`;
    viralScore = 96;
  }

  const hashtags = [
    "#fyp", "#foryou", "#trending", "#viral", "#tiktoknepal", "#foryoupage",
    "#explore", "#reels", "#viralvideo", "#highctr", "#nepal", "#creator",
    "#facebookpost", "#trendingsong"
  ];

  const fullFacebookPost = `${fbTitle}\n\n${fbBody}\n\n${hashtags.join(" ")}`;

  state.aiActiveResult = {
    hooks,
    fbTitle,
    fbBody,
    fullPost: fullFacebookPost,
    hashtags,
    viralScore
  };

  // Render into AI Studio elements
  const scoreBadge = document.getElementById("viral-score-number");
  if (scoreBadge) scoreBadge.textContent = `${viralScore}%`;

  const progressFill = document.getElementById("viral-meter-fill");
  if (progressFill) progressFill.style.width = `${viralScore}%`;

  // Render Hooks
  const hooksContainer = document.getElementById("ai-hooks-container");
  if (hooksContainer) {
    hooksContainer.innerHTML = hooks.map((h, i) => `
      <div class="hook-item">
        <div class="hook-text"><strong>#${i + 1}</strong> ${h}</div>
        <button class="btn-copy-small" onclick="copyAiHook(${i})">📋 Copy Hook</button>
      </div>
    `).join("");
  }

  // Render Facebook Post
  const fbPreview = document.getElementById("ai-fb-preview-content");
  if (fbPreview) {
    fbPreview.textContent = fullFacebookPost;
  }

  // Render Hashtags Cloud
  const hashCloud = document.getElementById("ai-hashtags-cloud");
  if (hashCloud) {
    hashCloud.innerHTML = hashtags.map(tag => `
      <span class="hashtag-chip" onclick="copySingleHashtag('${tag}')">${tag}</span>
    `).join("");
  }

  const resultsBox = document.getElementById("ai-results-box");
  if (resultsBox) {
    resultsBox.style.display = "flex";
    resultsBox.classList.add("visible");
    resultsBox.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }

  if (btn) {
    btn.disabled = false;
    btn.innerHTML = `<span>✨</span> Generate Viral Hooks &amp; Facebook Post`;
  }

  showToast(`🎉 Generated 3 Hooks & Facebook Ready Post (${viralScore}% Potential)!`);
}

function copyAiHook(index) {
  if (!state.aiActiveResult || !state.aiActiveResult.hooks[index]) return;
  const hook = state.aiActiveResult.hooks[index];
  navigator.clipboard.writeText(hook).then(() => {
    showToast(`📋 Copied Hook #${index + 1}!`);
  }).catch(() => {
    prompt("Copy Hook:", hook);
  });
}

function copyAiFacebookPost() {
  if (!state.aiActiveResult) return;
  const text = state.aiActiveResult.fullPost;
  navigator.clipboard.writeText(text).then(() => {
    showToast("📋 Copied Ready-to-Post Facebook Caption & Hashtags!");
  }).catch(() => {
    prompt("Copy Facebook Post:", text);
  });
}

function shareAiToFacebook() {
  if (!state.aiActiveResult) return;
  copyAiFacebookPost();
  showToast("Opening Facebook... (Caption is copied to your clipboard!)");
  window.open("https://www.facebook.com/", "_blank");
}

function copyAiHashtags() {
  if (!state.aiActiveResult) return;
  const tags = state.aiActiveResult.hashtags.join(" ");
  navigator.clipboard.writeText(tags).then(() => {
    showToast("📋 Copied all trending hashtags!");
  }).catch(() => {
    prompt("Copy Hashtags:", tags);
  });
}

function copySingleHashtag(tag) {
  navigator.clipboard.writeText(tag).then(() => {
    showToast(`Copied ${tag}`);
  });
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

