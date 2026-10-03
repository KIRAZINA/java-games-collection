// Small dependency-free static + proxy server for the Playwright suite.
// Serves games-frontend/dist on a fixed port and forwards /api/* to the
// backend so the app runs same-origin (API_BASE is '' -> relative URLs,
// no CORS involved). A dead backend produces 502 (never 404) so the app's
// poll-error logic treats it as a failed poll, not a missing room.
import http from 'node:http';
import { promises as fs } from 'node:fs';
import path from 'node:path';

const PORT = Number(process.env.E2E_DIST_PORT || 4179);
const BACKEND = process.env.E2E_BACKEND_URL || 'http://127.0.0.1:8080';
const DIST = path.resolve(process.cwd(), 'dist');

const TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.map': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
};

function proxyApi(req, res) {
  const target = new URL(req.url, BACKEND);
  const upstream = http.request(
    {
      hostname: target.hostname,
      port: target.port,
      path: target.pathname + target.search,
      method: req.method,
      headers: { ...req.headers, host: target.host },
    },
    (upRes) => {
      res.writeHead(upRes.statusCode || 502, upRes.headers);
      upRes.pipe(res);
    }
  );
  upstream.on('error', () => {
    if (!res.headersSent) {
      res.writeHead(502, { 'content-type': 'application/json' });
    }
    res.end(JSON.stringify({ error: 'Bad Gateway: backend unavailable', status: 502 }));
  });
  req.pipe(upstream);
}

async function serveStatic(req, res) {
  let urlPath;
  try {
    urlPath = decodeURIComponent(new URL(req.url, 'http://x').pathname);
  } catch {
    res.writeHead(400);
    res.end('bad request');
    return;
  }
  if (urlPath === '/') urlPath = '/index.html';
  const file = path.normalize(path.join(DIST, urlPath));
  if (!file.startsWith(DIST)) {
    res.writeHead(403);
    res.end('forbidden');
    return;
  }
  try {
    const data = await fs.readFile(file);
    res.writeHead(200, { 'content-type': TYPES[path.extname(file)] || 'application/octet-stream' });
    res.end(data);
  } catch {
    res.writeHead(404, { 'content-type': 'text/plain' });
    res.end('not found');
  }
}

const server = http.createServer((req, res) => {
  if (req.url === '/__e2e-health') {
    res.writeHead(200, { 'content-type': 'application/json' });
    res.end(JSON.stringify({ ok: true, backend: BACKEND, dist: DIST }));
    return;
  }
  if (req.url.startsWith('/api')) {
    proxyApi(req, res);
    return;
  }
  if (req.method !== 'GET' && req.method !== 'HEAD') {
    res.writeHead(405);
    res.end();
    return;
  }
  serveStatic(req, res);
});

server.listen(PORT, '127.0.0.1', () => {
  console.log(`[serve-dist] http://127.0.0.1:${PORT} -> dist=${DIST} api=${BACKEND}`);
});
