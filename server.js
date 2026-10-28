// TikSave Web Server & Video Downloader Gateway
const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');
const url = require('url');

const PORT = process.env.APP_PORT || 3000;
const PUBLIC_DIR = path.join(__dirname, 'public');

const MIME_TYPES = {
  '.html': 'text/html; charset=UTF-8',
  '.css': 'text/css; charset=UTF-8',
  '.js': 'application/javascript; charset=UTF-8',
  '.json': 'application/json; charset=UTF-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.mp4': 'video/mp4',
  '.mp3': 'audio/mpeg',
  '.webp': 'image/webp'
};

const server = http.createServer(async (req, res) => {
  // CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Range, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;

  // 1. API Endpoint: /api/extract
  if (pathname === '/api/extract') {
    const targetUrl = parsedUrl.query.url;
    const hd = parsedUrl.query.hd || '1';

    if (!targetUrl) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ code: -1, msg: 'Missing url parameter' }));
      return;
    }

    try {
      const apiUrl = `https://www.tikwm.com/api/?url=${encodeURIComponent(targetUrl)}&hd=${hd}`;
      const options = {
        headers: {
          'User-Agent': 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36',
          'Accept': 'application/json'
        }
      };

      https.get(apiUrl, options, (apiRes) => {
        let body = '';
        apiRes.on('data', chunk => body += chunk);
        apiRes.on('end', () => {
          try {
            const data = JSON.parse(body);
            // Fix relative URLs if any
            if (data && data.data) {
              const d = data.data;
              const fix = u => u && u.startsWith('/') ? 'https://www.tikwm.com' + u : u;
              d.play = fix(d.play);
              d.hdplay = fix(d.hdplay);
              d.wmplay = fix(d.wmplay);
              d.music = fix(d.music);
              d.cover = fix(d.cover);
              if (d.author) d.author.avatar = fix(d.author.avatar);
            }
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify(data));
          } catch (e) {
            res.writeHead(500, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ code: -1, msg: 'Error parsing TikWM response' }));
          }
        });
      }).on('error', (err) => {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ code: -1, msg: 'Failed to contact resolver: ' + err.message }));
      });
    } catch (e) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ code: -1, msg: e.message }));
    }
    return;
  }

  // 2. API Endpoint: /api/download (Direct stream proxy forcing attachment header)
  if (pathname === '/api/download') {
    const fileUrl = parsedUrl.query.url;
    const filename = parsedUrl.query.filename || 'tiksave_video.mp4';

    if (!fileUrl) {
      res.writeHead(400, { 'Content-Type': 'text/plain' });
      res.end('Missing file URL');
      return;
    }

    try {
      const client = fileUrl.startsWith('https') ? https : http;
      const downloadReq = client.get(fileUrl, {
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36',
          'Referer': 'https://www.tikwm.com/'
        }
      }, (streamRes) => {
        // Follow redirect if 301/302
        if (streamRes.statusCode === 301 || streamRes.statusCode === 302) {
          const redirectUrl = streamRes.headers.location;
          res.writeHead(302, { 'Location': `/api/download?url=${encodeURIComponent(redirectUrl)}&filename=${encodeURIComponent(filename)}` });
          res.end();
          return;
        }

        const headers = {
          'Content-Disposition': `attachment; filename="${encodeURIComponent(filename)}"`,
          'Content-Type': streamRes.headers['content-type'] || 'application/octet-stream'
        };
        if (streamRes.headers['content-length']) {
          headers['Content-Length'] = streamRes.headers['content-length'];
        }

        res.writeHead(200, headers);
        streamRes.pipe(res);
      });

      downloadReq.on('error', (e) => {
        res.writeHead(500, { 'Content-Type': 'text/plain' });
        res.end('Download failed: ' + e.message);
      });
    } catch (e) {
      res.writeHead(500, { 'Content-Type': 'text/plain' });
      res.end('Error: ' + e.message);
    }
    return;
  }

  // 3. Static File Serving from /public
  let reqPath = pathname === '/' ? '/index.html' : pathname;
  let safePath = path.normalize(reqPath).replace(/^(\.\.[\/\\])+/, '');
  let filePath = path.join(PUBLIC_DIR, safePath);

  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      // Fallback to index.html for SPA
      filePath = path.join(PUBLIC_DIR, 'index.html');
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';

    // Support Video Range Requests
    const range = req.headers.range;
    if (range && stats && stats.isFile()) {
      const parts = range.replace(/bytes=/, "").split("-");
      const start = parseInt(parts[0], 10);
      const end = parts[1] ? parseInt(parts[1], 10) : stats.size - 1;
      const chunksize = (end - start) + 1;
      const file = fs.createReadStream(filePath, { start, end });
      res.writeHead(206, {
        'Content-Range': `bytes ${start}-${end}/${stats.size}`,
        'Accept-Ranges': 'bytes',
        'Content-Length': chunksize,
        'Content-Type': contentType,
      });
      file.pipe(res);
      return;
    }

    fs.readFile(filePath, (readErr, content) => {
      if (readErr) {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('404 Not Found');
        return;
      }
      res.writeHead(200, {
        'Content-Type': contentType,
        'Cache-Control': ext === '.html' ? 'no-cache' : 'public, max-age=86400'
      });
      res.end(content);
    });
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[TikSave Web] Server running on http://0.0.0.0:${PORT}`);
});
