# Nuvio Enhanced — Feature List

## Stream Parser with Animated Badges
A polished stream information display that parses and presents video quality tags (4K, 1080p, 720p, etc.), audio codecs, and cached/debrid status with animated badges. Each badge animates on appearance, drawing attention to stream quality indicators and whether the stream is already cached for instant playback.

## AMOLED Mode
Two nested toggles that turn backgrounds pure black (`#000000`) for OLED power savings. The main toggle sets the base background to black; a second toggle, visible only when the first is active, also blacks out all surfaces, cards, and elevated elements. When AMOLED mode is turned off, the surfaces sub-option is automatically disabled.

## GlassMorph Navigation Tab
A translucent, glass-styled bottom navigation bar that adapts to the selected theme accent color. Gives the interface a modern, layered look while remaining functional with native tab behavior.

## Live TV with Configurable M3U Playlist
Built-in Live TV support powered by user-provided M3U playlists. Browse channels, view EPG data where available, and watch live streams directly inside the app. Live TV playback does **not** create entries in the "Continue Watching" section.

## Custom Theme Color Picker
Choose any accent color for the app theme. The color picker includes:
- A text field to input a hex color code manually
- A grid of preset colors for quick selection
- Three fine-tuning sliders (Hue, Saturation, Value) for precise customization
- A live preview of the selected color

## Date Format Option
Customize how dates are displayed throughout the app. Choose from various date format presets (e.g., DD/MM/YYYY, MM/DD/YYYY, etc.) to match your regional preference.

## Swipe Gesture Toggle
Ability to disable brightness and volume swipe controls during video playback. Useful for users who prefer dedicated hardware keys or find accidental gesture triggers disruptive.

## Configurable Skip Interval
Customizable forward/backward skip duration during playback. Choose the exact number of seconds to jump when tapping the skip controls, tailoring the experience to your watching habits.

## "Still Watching?" Prompt (Netflix-Style)
After a period of inactivity, a prompt appears asking if you are still watching. Includes an additional toggle to restrict this behavior to nighttime hours (22:00–04:00), preventing interruptions during daytime use.

## DNS over HTTPS
Secure DNS resolution via DNS-over-HTTPS (DoH) for improved privacy and protection against DNS spoofing, all configurable within the app settings.

## Custom User Agent
Override the User-Agent header sent by addon and plugin HTTP requests. Configure a custom UA string and choose where to apply it via three toggles: **Override for addons only**, **Override for plugins only**, or **Override for both**. When override is disabled, the custom UA is still used as a fallback if a request has no User-Agent header. When override is enabled, the custom UA forcibly replaces any existing User-Agent header, allowing precise control over how the app identifies itself to streaming sources.

## TOP 10 Catalogs
Two configurable rows that display posters with a numbered badge from 1 to 10, highlighting the most popular or trending content in a ranked format.

## Plugin System Enhancements
Extended plugin infrastructure with per-plugin scraper configuration, enabling fine-grained control over how each addon discovers and resolves media sources.

## CloudStream DEX Plugin Support
Android-only integration of the CloudStream 3 plugin ecosystem. DEX-based repositories (`.cs3`) can be installed from the same plugin management screen as JS plugins, with automatic detection of the manifest format and a dedicated "Cloudstream" badge on repository cards. Built-in TMDB title resolution enables search-based content discovery from CloudStream providers, and the existing Test Provider button works for both JS and DEX scrapers.

## Episode Code Formatter
Choose your preferred episode display format from options like `01x01`, `1x1`, `S01E01`, and more, ensuring episode labels match your personal preference.

## Cloudflare Challenge Solver
Automatically bypasses Cloudflare protection on streaming sources. When a request receives a 403/503 response with a Cloudflare challenge, a hidden WebView solves the challenge (executes JS, computes the challenge token, sets cookies). The resolved cookies are then reused for subsequent requests, enabling playback from add-ons that rely on Cloudflare-protected CDNs.

## Bookmark Badge on Posters
Items saved to the user's library are visually identified by a bookmark badge overlay on their poster across the Home screen, catalog pages, and collection folders. The badge uses the app's accent color scheme and fades in with animation, providing immediate visual feedback about which content is already in the library.

## Chromecast / DLNA Casting
Built-in casting support combining Google Cast and DLNA/UPnP protocols. Discover devices on your local network, stream video directly to Chromecast-enabled devices or any DLNA/UPnP renderer. Includes a local HTTP proxy server that forwards authentication headers to the cast receiver, enabling playback from token-authenticated or debrid-protected streams. Available on both Android and iOS (Google Cast SDK + native DLNA). Note: HLS playback via Chromecast has very limited support and may not work reliably.

## SponsorBlock Integration
Privacy-preserving SponsorBlock API integration that identifies and skips sponsored segments, intros, outros, filler, self-promotions, and interaction reminders. Uses SHA-256 hashing to avoid sending full video URLs. Skip intervals are merged with existing IntroDb/AniSkip data with overlap deduplication, and each category can be individually toggled in settings.

## Skip Segments on Timeline
Visual markers drawn directly on the playback progress bar showing where intro, recap, and outro segments are located. Colored rounded blocks appear along the slider track, making it easy to see upcoming skip segments at a glance while scrubbing through the video.
