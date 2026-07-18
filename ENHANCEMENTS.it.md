# Nuvio Enhanced — Elenco Funzionalità

## Stream Parser con Badge Animati
Un sistema avanzato di visualizzazione delle informazioni sullo stream che analizza e presenta tag di qualità video (4K, 1080p, 720p, ecc.), codec audio e stato di cache/debrid con badge animati. Ogni badge appare con un'animazione, attirando l'attenzione sugli indicatori di qualità e sulla disponibilità immediata dello stream.

## Modalità AMOLED
Due toggle innestati che impostano gli sfondi su nero puro (`#000000`) per il risparmio energetico su schermi OLED. Il toggle principale imposta lo sfondo base su nero; un secondo toggle, visibile solo quando il primo è attivo, annerisce anche tutte le superfici, le card e gli elementi elevati. Quando la modalità AMOLED viene disattivata, la sotto-opzione delle superfici viene automaticamente disabilitata.

## Barra di Navigazione GlassMorph
Una barra di navigazione inferiore traslucida in stile vetro che si adatta al colore accent del tema selezionato. Dona all'interfaccia un aspetto moderno e a strati, rimanendo funzionale con il comportamento nativo delle tab.

## Live TV con Playlist M3U Configurabile
Supporto Live TV integrato tramite playlist M3U fornite dall'utente. Sfoglia i canali, visualizza i dati EPG dove disponibili e guarda gli stream live direttamente nell'app. La riproduzione Live TV **non** crea voci nella sezione "Continua la visione".

### Live TV — Ricerca, Preferiti e Filtri per Categoria
Barra di ricerca canali con filtro in tempo reale, toggle per i preferiti e badge per categorie/gruppi per una navigazione rapida. L'intestazione mostra il conteggio dinamico dei canali: totale canali quando la ricerca è vuota, risultati filtrati durante la ricerca.

## Selettore Colore Tema Personalizzato
Scegli qualsiasi colore accent per il tema dell'app. Il selettore include:
- Un campo di testo per inserire manualmente un codice hex
- Una griglia di colori predefiniti per selezione rapida
- Tre slider di regolazione fine (Tonalità, Saturazione, Valore)
- Un'anteprima live del colore selezionato

## Formato Data Opzionale
Personalizza la visualizzazione delle date nell'app. Scegli tra vari formati predefiniti (es. DD/MM/YYYY, MM/DD/YYYY, ecc.).

## Toggle Gesture Scorrimento
Possibilità di disabilitare i controlli di luminosità e volume tramite swipe durante la riproduzione video. Utile per chi preferisce i tasti hardware dedicati o trova i gesti accidentali fastidiosi.

## Intervallo Salto Configurabile
Durata personalizzabile del salto avanti/indietro durante la riproduzione. Scegli il numero esatto di secondi da saltare quando si toccano i controlli.

## Prompt "Ancora in visione?" (Stile Netflix)
Dopo un periodo di inattività, appare un prompt che chiede se stai ancora guardando. Include un toggle per limitare questo comportamento alle ore notturne (22:00–04:00).

## DNS over HTTPS
Risoluzione DNS sicura tramite DNS-over-HTTPS (DoH) per una maggiore privacy e protezione contro lo spoofing DNS, configurabile nelle impostazioni dell'app.

## User Agent Personalizzato
Sostituisci lo User-Agent inviato dalle richieste HTTP di addon e plugin. Configura una stringa UA personalizzata e scegli dove applicarla tramite tre toggle: **Solo addon**, **Solo plugin** o **Entrambi**. Quando l'override è disattivo, lo UA personalizzato viene comunque usato come fallback. Quando attivo, sostituisce forzatamente qualsiasi User-Agent esistente.

## Cataloghi TOP 10
Due righe configurabili che mostrano poster con badge numerato da 1 a 10, evidenziando i contenuti più popolari o di tendenza in formato classifica.

## Miglioramenti al Sistema Plugin
Infrastruttura plugin estesa con configurazione per scraper singoli, consentendo un controllo granulare su come ogni addon scopre e risolve le sorgenti multimediali.

## Supporto Plugin DEX CloudStream
Integrazione Android dell'ecosistema plugin CloudStream 3. I repository DEX (`.cs3`) possono essere installati dalla stessa schermata di gestione plugin dei JS, con rilevamento automatico del formato manifest e badge "Cloudstream" dedicato sulle card dei repository. La risoluzione titoli TMDB integrata consente la scoperta di contenuti basata su ricerca dai provider CloudStream.

## Formattatore Codice Episodi
Scegli il formato di visualizzazione degli episodi tra opzioni come `01x01`, `1x1`, `S01E01` e altri.

## Risolutore Sfide Cloudflare
Aggira automaticamente la protezione Cloudflare sulle sorgenti di streaming. Quando una richiesta riceve una risposta 403/503 con una sfida Cloudflare, una WebView nascosta risolve la sfida. I cookie risolti vengono riutilizzati per le richieste successive.

## Badge Segnalibro sui Poster
Gli elementi salvati nella libreria vengono identificati visivamente da un badge segnalibro sovrapposto al poster nelle schermate Home, cataloghi e raccolte. Il badge usa il colore accent dell'app e appare con un'animazione.

## Chromecast / DLNA Casting
Supporto casting integrato che combina Google Cast e DLNA/UPnP. Scopri dispositivi sulla rete locale, riproduci video direttamente su dispositivi Chromecast o render DLNA/UPnP. Include un server proxy HTTP locale che inoltra gli header di autenticazione al ricevitore cast.

## Integrazione SponsorBlock
Integrazione dell'API SponsorBlock che rispetta la privacy, identificando e saltando segmenti sponsorizzati, intro, outro, filler, autopromozioni e reminder. Usa hash SHA-256 per evitare di inviare URL video completi. Gli intervalli vengono uniti ai dati IntroDb/AniSkip esistenti.

## Segmenti Saltati sulla Timeline
Indicatori visivi disegnati direttamente sulla barra di avanzamento della riproduzione che mostrano dove si trovano intro, recap e outro. Blocchi arrotondati colorati appaiono lungo la traccia dello slider.

## Calendario Libreria
Calendario in-app integrato nella sezione Libreria per navigare i contenuti per data di rilascio.

## Integrazione AI
Assistente AI accessibile dalle impostazioni e dalle pagine dettaglio dei media. Chiedi informazioni su un film o una serie TV — trama, dettagli cast, data di uscita e altro — direttamente nell'app.

## Visualizzatore Log di Debug
Un visualizzatore di log diagnostici accessibile da Impostazioni → Avanzate → Log di Debug. Include:
- **InAppLogger** — buffer circolare in memoria (3000 voci) con `StateFlow` per UI in tempo reale
- **Filtri** — filtra per livello (Tutti/Debug/Info/Attenzione/Errore)
- **Scroll** — scorrimento orizzontale + verticale, copia negli appunti
- **Categorie** — i tag vengono auto-categorizzati (Player, Rete, Metadati, ecc.)
- **Retenzione** — ultime 3000 voci mantenute in memoria

### Acquisizione Log Estesa
Un `LogWriter` Kermit personalizzato inoltra tutte le 200–400+ chiamate di log da 47 file feature (Auth, Trakt, TMDB, Sync, Cast, P2P, ecc.) al visualizzatore in-app, senza sostituire i logger di piattaforma (Logcat/OSLog rimangono attivi). Anche le chiamate `Log.w/e` Android, `print()`/`NSLog()` iOS e `println()` Desktop vengono collegate.

## Integrazione AniList (Sola Lettura)
Sfoglia la tua libreria AniList direttamente in Nuvio — visualizza le liste "In visione", "Completati", "Da vedere" e altre. L'integrazione è in sola lettura: puoi esplorare e navigare i tuoi titoli salvati ma non aggiungerne di nuovi dall'app. Le funzionalità per modificare le liste verranno aggiunte in seguito.

## Ricerca Sottotitoli OpenSubtitles
Integrazione con OpenSubtitles per la ricerca manuale di sottotitoli durante la riproduzione. Una scheda dedicata nel modal sottotitoli permette di cercare su OpenSubtitles per lingua e scaricare sottotitoli su richiesta. La chiave API OpenSubtitles si configura in Impostazioni → Integrazioni → OpenSubtitles, e le impostazioni vengono sincronizzate tra i dispositivi tramite Nuvio Sync.

## Accelerazione Ritardo Sottotitoli con Pressione Prolungata
I pulsanti stepper del ritardo sottotitoli nella scheda Stile supportano l'accelerazione a pressione prolungata. Un tap singolo regola il ritardo di 100 ms. Tenendo premuto oltre 300 ms, la velocità di regolazione accelera progressivamente (moltiplicatore 1×→10×, intervallo tick 250 ms→60 ms) per una regolazione rapida nell'intervallo esteso di ±120 s. Il player e le preferenze sono debounce — l'UI si aggiorna immediatamente durante la pressione, mentre il ritardo effettivo viene applicato 300 ms dopo che l'utente smette di premere.
