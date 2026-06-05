# AMOLED Mode — Implementazione in NuvioTV

## Architettura generale

Il sistema ha **2 toggle** annidati, entrambi salvati in DataStore:

1. **`amoledMode`** (toggle principale) — `ThemeDataStore.kt:29` / `ThemeSettingsScreen.kt:171-178`
2. **`amoledSurfacesMode`** (sottopzione, visibile solo se `amoledMode = true`) — `ThemeDataStore.kt:30` / `ThemeSettingsScreen.kt:179-189`

Quando `amoledMode` viene disattivato, anche `amoledSurfacesMode` viene forzato a `false` (`ThemeDataStore.kt:81-83`).

## Logica dei colori (`Color.kt:13-57`)

La classe `NuvioColorScheme` riceve i due booleani e calcola i colori dinamicamente:

```kotlin
private val pureBlack = Color(0xFF000000)
private val pureBlackSurfaces = amoledMode && amoledSurfacesMode

Background         = if (amoledMode)         pureBlack       else palette.background
BackgroundElevated = if (pureBlackSurfaces)  pureBlack       else palette.backgroundElevated
BackgroundCard     = if (pureBlackSurfaces)  pureBlack       else palette.backgroundCard
Surface            = if (pureBlackSurfaces)  pureBlack       else Color(0xFF1E1E1E)
SurfaceVariant     = if (pureBlackSurfaces)  pureBlack       else Color(0xFF2D2D2D)
```

| Impostazione | Background | BackgroundElevated | BackgroundCard | Surface | SurfaceVariant |
|---|---|---|---|---|---|
| **Normale** | colore tema | colore tema | colore tema | `#1E1E1E` | `#2D2D2D` |
| **AMOLED on** | **`#000000`** | colore tema | colore tema | `#1E1E1E` | `#2D2D2D` |
| **AMOLED + Surfaces on** | **`#000000`** | **`#000000`** | **`#000000`** | **`#000000`** | **`#000000`** |

Tutti gli altri colori (testo, accenti, bordo, focus, rating, error, success) **rimangono invariati**.

## Propagazione nell'UI

1. **Persistenza**: `ThemeDataStore` salva i due booleani in DataStore con chiavi `amoled_mode` / `amoled_surfaces_mode` (`ThemeDataStore.kt:29-30`).
2. **ViewModel**: `ThemeSettingsViewModel` espone `amoledMode` e `amoledSurfacesMode` in `ThemeSettingsUiState` e reagisce agli eventi `ToggleAmoledMode` / `ToggleAmoledSurfacesMode` (`ThemeSettingsViewModel.kt:23-24, 108-120`).
3. **Tema**: `MainActivity.kt:404-408` legge i flow da `ThemeDataStore` e li passa a `NuvioTheme(amoledMode, amoledSurfacesMode)`.
4. **ColorScheme**: `NuvioColorScheme` in `Color.kt` calcola i colori dinamici — **non c'è un "tema AMOLED" separato, i colori si calcolano in runtime a partire dalla palette del tema selezionato**.
5. **Consumo**: l'intera UI usa `NuvioColors.Background`, `NuvioColors.BackgroundElevated`, `NuvioColors.BackgroundCard`, `NuvioColors.Surface`, `NuvioColors.SurfaceVariant` — tutti questi puntano a `NuvioColorScheme` (tramite CompositionLocal `LocalNuvioColors`). Oltre 300 occorrenze in tutto il codice.

## Cosa cambia visivamente

- **AMOLED Mode ON**: solo lo sfondo principale (`Background`) diventa nero assoluto. Cards, elevazioni e superfici mantengono i loro colori scuri ma non puri.
- **AMOLED + Surfaces ON**: **tutti** gli sfondi (principale, cards, elevati, superfici) diventano nero assoluto (`#000000`). Massimo risparmio energetico su schermi OLED/AMOLED ma riduce la gerarchia visiva (profondità/depth degli elementi).

## Cosa NON cambia

- Testi, colori primari/secondari, focus, bordi, rating, error/success — tutti invariati.
- I colori accent/secondari continuano a seguire il tema selezionato (Ocean, Forest, ecc.).
- Il font selezionato non è coinvolto.

## Schema delle modifiche da replicare in Compose Multiplatform

1. **Settings UI**: due `Switch` annidati (il secondo visibile solo se il primo è ON) con label "AMOLED Mode" / "AMOLED Surfaces Mode"
2. **Persistenza**: salvare due booleani (es. in Settings/DataStore)
3. **Theme/ColorScheme**: ricevere i due booleani, usare `0xFF000000` per `Background` (se amoled) e per `BackgroundElevated`/`BackgroundCard`/`Surface`/`SurfaceVariant` (se anche surfaces)
4. **Tutti i componenti UI** devono referenziare colori dinamici dal tema (non hardcoded) per propagare le modifiche automaticamente
