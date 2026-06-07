<div align="center">

  <img src="https://github.com/tapframe/NuvioTV/blob/main/assets/brand/app_logo_wordmark.png" alt="Nuvio" width="300" />
  <br />
  <br />

  [![Contributors][contributors-shield]][contributors-url]
  [![Forks][forks-shield]][forks-url]
  [![Stargazers][stars-shield]][stars-url]
  [![Issues][issues-shield]][issues-url]
  [![License][license-shield]][license-url]

  <p>
    A modern media hub for Android and iOS built with Kotlin Multiplatform and Compose Multiplatform.
    <br />
    Stremio addon ecosystem • Cross-platform
  </p>

</div>

## About

Nuvio is the current Kotlin Multiplatform rewrite of the original React Native app. It delivers a shared Compose UI for Android and iOS while keeping the playback-focused experience, collection tools, watch progress flows, downloads, and Stremio addon ecosystem integration that shaped the earlier app.

The mobile app is built from a single shared codebase in [composeApp](./composeApp), with native platform entry points for Android and iOS.

## Enhanced Version Specific Features

The following features are unique to this mobile repository and are not present in the original NuvioMobile codebase.

### Stream Parser with Animated Badges
A polished stream information display that parses and presents video quality tags (4K, 1080p, 720p, etc.), audio codecs, and cached/debrid status with animated badges. Each badge animates on appearance, drawing attention to stream quality indicators and whether the stream is already cached for instant playback.

### AMOLED Mode
Two nested toggles that turn backgrounds pure black (`#000000`) for OLED power savings. The main toggle sets the base background to black; a second toggle, visible only when the first is active, also blacks out all surfaces, cards, and elevated elements. When AMOLED mode is turned off, the surfaces sub-option is automatically disabled.

### GlassMorph Navigation Tab
A translucent, glass-styled bottom navigation bar that adapts to the selected theme accent color. Gives the interface a modern, layered look while remaining functional with native tab behavior.

### Live TV with Configurable M3U Playlist
Built-in Live TV support powered by user-provided M3U playlists. Browse channels, view EPG data where available, and watch live streams directly inside the app.

### Swipe Gesture Toggle
Ability to disable brightness and volume swipe controls during video playback. Useful for users who prefer dedicated hardware keys or find accidental gesture triggers disruptive.

### Configurable Skip Interval
Customizable forward/backward skip duration during playback. Choose the exact number of seconds to jump when tapping the skip controls, tailoring the experience to your watching habits.

### "Still Watching?" Prompt (Netflix-Style)
After a period of inactivity, a prompt appears asking if you are still watching. Includes an additional toggle to restrict this behavior to nighttime hours (22:00–04:00), preventing interruptions during daytime use.

### DNS over HTTPS
Secure DNS resolution via DNS-over-HTTPS (DoH) for improved privacy and protection against DNS spoofing, all configurable within the app settings.

### TOP 10 Catalogs
Two configurable rows that display posters with a numbered badge from 1 to 10, highlighting the most popular or trending content in a ranked format.

### Plugin System Enhancements
Extended plugin infrastructure with per-plugin scraper configuration, enabling fine-grained control over how each addon discovers and resolves media sources.

### Episode Code Formatter
Choose your preferred episode display format from options like `01x01`, `1x1`, `S01E01`, and more, ensuring episode labels match your personal preference.

## Installation

### Android

Download the latest Android build from [GitHub Releases](https://github.com/NuvioMedia/NuvioMobile/releases/latest).

### iOS

- [TestFlight](https://testflight.apple.com/join/u4y7MHK9)

## Development

```bash
git clone https://github.com/NuvioMedia/NuvioMobile.git
cd NuvioMobile
./scripts/run-mobile.sh android
# or
./scripts/run-mobile.sh ios
```

### Project Structure

- `composeApp/` contains the shared Kotlin Multiplatform and Compose Multiplatform app code.
- `composeApp/src/commonMain/` contains shared UI, features, repositories, and platform-agnostic logic.
- `composeApp/src/androidMain/` contains Android-specific integrations.
- `composeApp/src/iosMain/` contains iOS-specific integrations.
- `iosApp/` contains the native Xcode project and iOS entry point.

Useful commands:

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:compileKotlinIosSimulatorArm64
./scripts/build-distribution.sh
```

Versioning is driven from `iosApp/Configuration/Version.xcconfig`, which is used as the shared source of truth for both iOS and Android builds.

## Legal & DMCA

Nuvio functions solely as a client-side interface for browsing metadata and playing media provided by user-installed extensions and/or user-provided sources. It is intended for content the user owns or is otherwise authorized to access.

Nuvio is not affiliated with any third-party extensions, catalogs, sources, or content providers. It does not host, store, or distribute any media content.

For comprehensive legal information, including our full disclaimer, third-party extension policy, and DMCA/Copyright information, please visit our [Legal & Disclaimer Page](https://nuvioapp.space/legal).

## Built With

- Kotlin Multiplatform
- Compose Multiplatform
- Kotlin
- AndroidX Media3
- AVFoundation and native iOS integrations

## Star History

<a href="https://www.star-history.com/#NuvioMedia/NuvioMobile&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=NuvioMedia/NuvioMobile&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=NuvioMedia/NuvioMobile&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=NuvioMedia/NuvioMobile&type=date&legend=top-left" />
 </picture>
</a>

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