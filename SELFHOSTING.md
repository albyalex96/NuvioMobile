# Self-Hosting Nuvio Enhanced (Web)

Nuvio Enhanced's Web (WasmJs) build is distributed as a Docker image via GitHub Container Registry. This guide covers deployment, configuration, and API proxying.

---

## Quick Start (Docker)

```bash
docker run -d \
  --name nuvio-web \
  -p 8080:80 \
  ghcr.io/NuvioMedia/NuvioMobile/web:latest
```

Open `http://localhost:8080` in a browser.

---

## Building the Image Yourself

```bash
# Build the WasmJs web distribution
./gradlew :composeApp:wasmJsBrowserDistribution

# Collect artifacts
mkdir -p web-dist
cp -r composeApp/build/dist/wasmJs/productionExecutable/* web-dist/

# Build the Docker image
docker build -t nuvio-web -f docker/Dockerfile web-dist/
```

---

## Nginx Configuration

The image includes a preconfigured nginx template at `/etc/nginx/templates/default.conf.template` with:

- **COEP credentialless** — enables `SharedArrayBuffer` (required for Compose Wasm threading) without requiring `Cross-Origin-Resource-Policy` headers from every resource
- **Global CORS headers** — `Access-Control-Allow-Origin: *` on all routes
- **Proxied endpoints:**
  - `/tmdb-proxy/` → `https://image.tmdb.org/t/p/` (TMDB images)
  - `/extendedratings-proxy/` → `https://www.extendedratings.com/` (external ratings)
- **Compression:** brotli + gzip for JS, Wasm, CSS
- **Caching:** immutable cache headers for hashed assets, no-cache for `index.html`

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `PORT` | `80` | Container listen port |
| `NGINX_CONFIG` | *(unset)* | Path to a custom `default.conf.template` to override the built-in config |

### Custom Nginx Config

Mount your own template to override the default:

```bash
docker run -d \
  -p 8080:80 \
  -v /path/to/my-custom.conf.template:/etc/nginx/templates/default.conf.template \
  ghcr.io/NuvioMedia/NuvioMobile/web:latest
```

Or set `NGINX_CONFIG` to an alternative path.

### Example Custom Config (adding another proxy)

```nginx
server {
    listen ${PORT};
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # COEP credentialless — required for Wasm threading
    add_header Cross-Origin-Opener-Policy "same-origin" always;
    add_header Cross-Origin-Embedder-Policy "credentialless" always;

    # Global CORS
    add_header Access-Control-Allow-Origin "*" always;
    add_header Access-Control-Allow-Methods "GET, OPTIONS" always;
    add_header Access-Control-Allow-Headers "*" always;

    # Compression
    gzip on;
    gzip_types text/plain text/css application/javascript application/wasm image/svg+xml;
    gzip_min_length 1024;
    gzip_vary on;
    gzip_static on;
    brotli on;
    brotli_types text/plain text/css application/javascript application/wasm image/svg+xml;

    # Proxy: add your own upstream
    location /my-proxy/ {
        proxy_pass https://api.example.com/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /tmdb-proxy/ {
        proxy_pass https://image.tmdb.org/t/p/;
        proxy_set_header Host image.tmdb.org;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /extendedratings-proxy/ {
        proxy_pass https://www.extendedratings.com/;
        proxy_set_header Host www.extendedratings.com;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location ~* \.(?:js|wasm)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    location /composeResources/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    location = /index.html {
        expires -1;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }

    location / {
        try_files $uri /index.html;
    }
}
```

---

## Architecture

```
┌──────────────┐      ┌──────────────────────────────────────┐
│   Browser    │─────▶│  nginx (reverse proxy + static host) │
└──────────────┘      └──────────────────────────────────────┘
                               │
                    ┌──────────┼──────────┐
                    ▼          ▼          ▼
             TMDB API   ExtendedRatings   Wasm App
             (proxied)   (proxied)     (static files)
```

The nginx container serves the Compose Wasm app as static files and proxies external API calls that lack CORS headers, resolving cross-origin issues in the browser.

---

## Notes

- The Web build does **not** support DRM-protected content (Widevine, FairPlay) due to Wasm sandbox limitations.
- Some addons that require native platform APIs (e.g., file system access) may not function in the Web build.
- Original platform credentials (Supabase, Trakt, debrid services) work identically on the Web build.
