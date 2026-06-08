# NuvioMobile — Download Manager

## Panoramica App

**NuvioMobile** è un player media multipiattaforma (Android + iOS) per lo streaming e il download di contenuti video via debrid services e addon Stremio. Basato su Kotlin Multiplatform + Compose Multiplatform.

### Funzionalità principali
- Streaming da debrid services (Real-Debrid, AllDebrid, Premiumize, TorBox)
- Supporto protocollo Stremio (addon, cataloghi, stream, sottotitoli)
- Player ExoPlayer (Android) / AVPlayer (iOS)
- Download offline di contenuti (streaming diretti e HLS)
- Trakt scrobbling, liste, sync
- Integrazione TMDB per metadati
- Live Activities / Dynamic Island (iOS)
- Widget stato download (iOS)

---

## Download Manager — Elenco File

### Core Feature (`com.nuvio.app.features.downloads`)

#### commonMain (modelli, repository, UI, dichiarazioni expect)

```
composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/
├── DownloadsRepository.kt          # Repository centrale: enqueue, resume, pause, cancel, logica download HLS, StateFlow
├── DownloadsModels.kt              # Modelli dati: DownloadItem, DownloadStatus, HlsDownloadSelection, DownloadsUiState
├── DownloadsScreen.kt              # Schermata UI download attivi/completati
├── DownloadsHlsSelectionSheet.kt   # Sheet selezione variante HLS (qualità/audio/sottotitoli)
├── HlsPlaylistParser.kt            # Parser playlist HLS master e media
├── DownloadsPlatformDownloader.kt  # Dichiarazione expect per download platform-specifici
├── DownloadsStorage.kt             # Dichiarazione expect per persistenza stato download
├── DownloadsLiveStatusPlatform.kt  # Dichiarazione expect per notifiche/widget stato live
└── DownloadsClock.kt               # Dichiarazione expect per orario piattaforma
```

#### androidMain (implementazioni actual Android)

```
composeApp/src/androidMain/kotlin/com/nuvio/app/features/downloads/
├── DownloadsPlatformDownloader.android.kt   # Download via OkHttp, ExoPlayer probe, AES-128 CBC decrypt, MediaMuxer remux
├── DownloadsStorage.android.kt              # Persistenza via SharedPreferences
├── DownloadsLiveStatusPlatform.android.kt   # Notifica foreground con progress download
├── DownloadsClock.android.kt                # System.currentTimeMillis()
└── DownloadsNotificationActionReceiver.kt   # BroadcastReceiver per azioni notifica (pausa/ripresa)
```

#### iosMain (implementazioni actual iOS)

```
composeApp/src/iosMain/kotlin/com/nuvio/app/features/downloads/
├── DownloadsPlatformDownloader.ios.kt   # Download via URLSession, CommonCrypto AES-128 CBC decrypt
├── DownloadsStorage.ios.kt              # Persistenza via NSUserDefaults
├── DownloadsLiveStatusPlatform.ios.kt   # Payload per Live Activities widget
└── DownloadsClock.ios.kt                # NSDate
```

### iOS Native Swift (Widget Live Activities)

```
iosApp/DownloadsWidgetExtension/
├── DownloadsLiveActivityWidget.swift    # UI widget Dynamic Island / lockscreen
├── DownloadsWidgetBundle.swift          # Entry point widget bundle
└── Info.plist                           # Config widget extension

iosApp/iosApp/
└── DownloadsLiveActivityManager.swift   # Gestione ciclo di vita Live Activities
```

### Consumer del Download Feature (fuori dal package downloads)

```
composeApp/src/commonMain/kotlin/com/nuvio/app/
├── App.kt                                           # Navigazione, auto-play download, DownloadsScreen in nav graph
├── features/streams/StreamsScreen.kt                 # Enqueue da stream, HLS selection sheet
├── features/player/PlayerScreenRuntimeSourceActions.kt  # switchToDownloadedEpisode(), source audio/sottotitoli
├── features/player/PlayerScreenModalHosts.kt         # selectDownloadedEpisodeForPlayback()
├── features/player/PlayerNextEpisodeAutoPlay.kt      # Auto-play prossimo episodio scaricato
├── features/player/PlayerScreenRuntimeUi.kt          # Riferimenti p2pDownloadSpeed, onEpisodeSelectedForDownload
├── features/player/PlayerScreenRuntimeState.kt       # Campo downloadedLabel
├── features/player/PlayerScreenContent.kt            # Imposta downloadedLabel
├── features/settings/SettingsRootPage.kt             # Riga navigazione "Downloads"
├── features/settings/SettingsSearch.kt               # Indice ricerca "downloads"
├── features/profiles/ProfileRepository.kt            # onProfileChanged -> ricarica download
└── core/deeplink/AppUrlBridge.kt                     # Deep link "downloads" -> AppDeepLink.Downloads
```

```
composeApp/src/androidMain/kotlin/com/nuvio/app/
├── MainActivity.kt                     # Inizializza DownloadsPlatformDownloader, Storage, LiveStatus
├── AndroidManifest.xml                 # Registra DownloadsNotificationActionReceiver
└── res/xml/nuvio_file_paths.xml        # FileProvider path per downloads/
```

### Risorse Stringhe Localizzate

```
composeApp/src/commonMain/composeResources/values/
├── strings.xml          # Inglese (57 match download)
├── values-de/strings.xml
├── values-fr/strings.xml
├── values-es/strings.xml
├── values-it/strings.xml
├── values-pt/strings.xml
├── values-pl/strings.xml
├── values-tr/strings.xml
├── values-nb/strings.xml
├── values-cs/strings.xml
├── values-el/strings.xml
├── values-in/strings.xml
└── values-id/strings.xml
```

### Test

Nessun test specifico per il download manager al momento.

---

## Ultimi Cambiamenti (branch `test/hls`)

- AES-128 decryption (Android: javax.crypto, iOS: CommonCrypto)
- Download separato tracce audio/sottotitoli
- Remux video+audio in singolo MP4 via MediaMuxer (Android)
- Parallelizzazione download segmenti (batch 4)
- Fix: downloadTrack ora completa anche se download audio fallisce
- Fix: tracce audio con URI nullo (muxed) non impostano audioUrl separato
- Fix: estensione file audio .ts invece di .aac per compatibilità MediaExtractor
