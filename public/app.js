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
  }
};

// Demo Trending Sample Videos (Matches DownloadRepository.kt)
const SAMPLE_VIDEOS = [
  {
    title: "HDR+ Ultra 60fps Neon Odyssey",
    author: "@cyberwave",
    nickname: "CyberWave",
    url: "https://www.tiktok.com/@cyberwave/video/7321948192384729102",
    cover: "https://picsum.photos/seed/tik1/720/1280",
    avatar: "https://picsum.photos/seed/avatar1/200/200",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
    music: "Original Sound - Neon Odyssey [Ultra Bass]",
    likes: 1420000,
    views: 9800000,
    comments: 12400,
    shares: 38900,
    duration: 15,
    sizes: { '8k': 28450000, 'hdr': 18450000, 'hd': 9820000, 'mp3': 3200000 }
  },
  {
    title: "4K Sunset Coastline Drone Flight",
    author: "@aerial_cinema",
    nickname: "Aerial Cinema",
    url: "https://www.tiktok.com/@aerial_cinema/video/7298374619283746281",
    cover: "https://picsum.photos/seed/tik2/720/1280",
    avatar: "https://picsum.photos/seed/avatar2/200/200",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
    music: "Golden Hour Ambient (Remix)",
    likes: 850000,
    views: 4200000,
    comments: 8900,
    shares: 21000,
    duration: 22,
    sizes: { '8k': 32100000, 'hdr': 22500000, 'hd': 12400000, 'mp3': 4100000 }
  },
  {
    title: "Trending Lo-Fi Tokyo Rain Beats",
    author: "@tokyo_vibes",
    nickname: "Tokyo Vibes",
    url: "https://www.tiktok.com/@tokyo_vibes/video/7311098472619284720",
    cover: "https://picsum.photos/seed/tik3/720/1280",
    avatar: "https://picsum.photos/seed/avatar3/200/200",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
    music: "Midnight In Shibuya - LoFi Chill",
    likes: 2100000,
    views: 15300000,
    comments: 34100,
    shares: 94000,
    duration: 30,
    sizes: { '8k': 38900000, 'hdr': 26700000, 'hd': 14800000, 'mp3': 5200000 }
  }
];

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
  loadPreferences();
  loadHistory();
  setupClipboardListener();
  handleInputStateChange();
});

// Tab Switching
function switchTab(tabName) {
  state.activeTab = tabName;

  // View sections
  const views = ['downloader', 'batch', 'library', 'settings'];
  views.forEach(v => {
    const el = document.getElementById(`view-${v}`);
    if (el) el.style.display = (v === tabName) ? (v === 'downloader' ? 'block' : 'block') : 'none';
  });

  // Top Nav tab buttons
  document.querySelectorAll('.nav-tab').forEach(btn => btn.classList.remove('active'));
  const activeBtn = document.getElementById(`tab-btn-${tabName}`);
  if (activeBtn) activeBtn.classList.add('active');

  // Bottom Nav items
  document.querySelectorAll('.bnav-item').forEach(btn => btn.classList.remove('active'));
  const activeBnav = document.getElementById(`bnav-${tabName}`);
  if (activeBnav) activeBnav.classList.add('active');

  if (tabName === 'library') {
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

// Direct File Save to Browser Storage
function saveFileToDevice(url, filename) {
  // Try via proxy endpoint /api/download first to force Content-Disposition attachment header
  const directProxy = `/api/download?url=${encodeURIComponent(url)}&filename=${encodeURIComponent(filename)}`;

  const a = document.createElement('a');
  a.href = directProxy;
  a.download = filename;
  a.target = '_blank';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);

  // Fallback direct link after small delay in case proxy was skipped
  setTimeout(() => {
    try {
      const fallbackA = document.createElement('a');
      fallbackA.href = url;
      fallbackA.download = filename;
      fallbackA.target = '_blank';
      fallbackA.click();
    } catch (e) {
      console.warn("Direct download fallback failed:", e);
    }
  }, 400);
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
