<div align="center">

  <img src="https://github.com/albyalex96/NuvioTV/blob/main/assets/brand/app_logo_wordmark.png" alt="Nuvio Enhanced" width="300" />
  <br />
  <br />

  [![Contributors][contributors-shield]][contributors-url]
  [![Forks][forks-shield]][forks-url]
  [![Stargazers][stars-shield]][stars-url]
  [![Issues][issues-shield]][issues-url]
  [![License][license-shield]][license-url]

  <br />

  **A modern media hub for Android, iOS and Web — powered by the Stremio addon ecosystem.**

  <br />

</div>

## About

**Nuvio Enhanced** is a feature-enhanced fork of [Nuvio](https://github.com/NuvioMedia/NuvioMobile), a Kotlin Multiplatform rewrite of the original React Native app. It delivers a shared Compose UI across Android, iOS, and Web while retaining the playback-focused experience, collection management, watch progress syncing, downloads, and Stremio addon ecosystem integration that shaped the original.

The mobile/web app is built from a single shared codebase in [`composeApp/`](./composeApp) with native entry points for each platform.

---

## Enhanced Features

The following features are unique to this fork and are not present in the upstream NuvioMobile codebase:

### Stream Parser with Animated Badges
Parses and presents video quality tags (4K, 1080p, 720p, etc.), audio codecs, and cached/debrid status with animated badges for instant visual feedback.

### AMOLED Mode
Two nested toggles that turn backgrounds pure black (`#000000`) for OLED power savings. A secondary toggle extends this to all surfaces and cards.

### GlassMorph Navigation Tab
A translucent, glass-styled bottom navigation bar that adapts to the selected theme accent color.

### Live TV with M3U Playlist
Built-in Live TV with user-provided M3U playlists, channel browsing, EPG data, and in-app playback.

### Swipe Gesture Toggle
Disable brightness/volume swipe controls during video playback for users who prefer hardware keys.

### Configurable Skip Interval
Customizable forward/backward seek duration.

### "Still Watching?" Prompt (Netflix-Style)
Inactivity prompt with a nighttime-only mode (22:00–04:00) toggle.

### DNS over HTTPS
Secure DNS resolution via DoH, configurable in app settings.

### TOP 10 Catalogs
Configurable ranked rows with numbered badges from 1 to 10 for trending content.

### Plugin System Enhancements
Per-plugin scraper configuration for fine-grained control over addon media source discovery.

### Episode Code Formatter
Choose your preferred episode display format (e.g. `01x01`, `1x1`, `S01E01`).

### Cloudflare Challenge Solver
Automatically bypasses Cloudflare protection via a hidden WebView that resolves challenge tokens and reuses cookies for subsequent requests.

### Bookmark Badge on Posters
Library items display a bookmark badge overlay on posters across Home, catalogs, and collections.

---

## Quick Start

```bash
git clone -b build/cmp https://github.com/albyalex96/NuvioMobile.git
cd NuvioMobile
./scripts/run-mobile.sh android    # Android emulator
./scripts/run-mobile.sh ios s      # iOS simulator
```

> **Note:** You'll need API keys (Supabase, Trakt, TMDB, debrid services) configured in `local.properties`. See the upstream setup guide for details. Original platform credentials are fully compatible with this fork.

### Build

```bash
# Android debug (full variant — preferred)
./gradlew :composeApp:assembleFullDebug

# iOS compile check
./gradlew :composeApp:compileKotlinIosSimulatorArm64

# Web (WasmJs)
./gradlew :composeApp:wasmJsBrowserDistribution
```

Versioning is driven from [`version.json`](./version.json) at the project root — the single source of truth for both `versionName` and `versionCode` across Android, iOS, and Web. The Xcode config is auto-synced from this file at build time.

---

## Self-Hosting (Web)

The Web (WasmJs) build is distributed as a Docker image. For full self-hosting instructions — including deployment, nginx configuration, environment variables, and API proxying — see **[SELFHOSTING.md](./SELFHOSTING.md)**.

---

## Legal

Nuvio Enhanced functions solely as a client-side interface for browsing metadata and playing media provided by user-installed extensions and/or user-provided sources. It is intended for content the user owns or is otherwise authorized to access.

This fork is not affiliated with the original Nuvio project or any third-party extensions, catalogs, sources, or content providers. It does not host, store, or distribute any media content.

For comprehensive legal information, including our full disclaimer, third-party extension policy, and DMCA/Copyright information, please visit the [original Nuvio Legal Page](https://nuvio.tv/legal).

---

## Built With

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Kotlin](https://kotlinlang.org/)
- [AndroidX Media3 (ExoPlayer)](https://developer.android.com/media/media3)
- [AVFoundation (iOS)](https://developer.apple.com/av-foundation/)
- [Stremio Addon Protocol](https://github.com/Stremio/stremio-addon-sdk)

---

## Star History

<a href="https://www.star-history.com/#NuvioMedia/NuvioMobile&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=NuvioMedia/NuvioMobile&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=NuvioMedia/NuvioMobile&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=NuvioMedia/NuvioMobile&type=date&legend=top-left" />
 </picture>
</a>

---

<div align="center">
  <sub>Built on the shoulders of the Nuvio project. Licensed under the GPLv3.</sub>
</div>

<!-- MARKDOWN LINKS & IMAGES -->
[contributors-shield]: https://img.shields.io/github/contributors/NuvioMedia/NuvioMobile.svg?style=for-the-badge
[contributors-url]: https://github.com/NuvioMedia/NuvioMobile/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/NuvioMedia/NuvioMobile.svg?style=for-the-badge
[forks-url]: https://github.com/NuvioMedia/NuvioMobile/network/members
[stars-shield]: https://img.shields.io/github/stars/NuvioMedia/NuvioMobile.svg?style=for-the-badge
[stars-url]: https://github.com/NuvioMedia/NuvioMobile/stargazers
[issues-shield]: https://img.shields.io/github/issues/NuvioMedia/NuvioMobile.svg?style=for-the-badge
[issues-url]: https://github.com/NuvioMedia/NuvioMobile/issues
[license-shield]: https://img.shields.io/github/license/NuvioMedia/NuvioMobile.svg?style=for-the-badge
[license-url]: https://github.com/NuvioMedia/NuvioMobile/blob/main/LICENSE
