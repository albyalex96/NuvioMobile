package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.opensubtitles.OpenSubtitlesSettings
import com.nuvio.app.features.opensubtitles.OpenSubtitlesSettingsRepository
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_save
import nuvio.composeapp.generated.resources.settings_opensubtitles_add_api_key_first
import nuvio.composeapp.generated.resources.settings_opensubtitles_api_key_hint
import nuvio.composeapp.generated.resources.settings_opensubtitles_api_key_label
import nuvio.composeapp.generated.resources.settings_opensubtitles_attribution_body
import nuvio.composeapp.generated.resources.settings_opensubtitles_attribution_title
import nuvio.composeapp.generated.resources.settings_opensubtitles_enable
import nuvio.composeapp.generated.resources.settings_opensubtitles_enable_description
import nuvio.composeapp.generated.resources.settings_opensubtitles_languages
import nuvio.composeapp.generated.resources.settings_opensubtitles_languages_description
import nuvio.composeapp.generated.resources.settings_opensubtitles_personal_api_key
import nuvio.composeapp.generated.resources.settings_opensubtitles_section_attribution
import nuvio.composeapp.generated.resources.settings_opensubtitles_section_credentials
import nuvio.composeapp.generated.resources.settings_opensubtitles_section_languages
import org.jetbrains.compose.resources.stringResource

private val SubtitleLanguages = listOf(
    "en" to "English",
    "it" to "Italiano",
    "fr" to "Fran\u00e7ais",
    "de" to "Deutsch",
    "es" to "Espa\u00f1ol",
    "pt" to "Portugu\u00eas",
    "nl" to "Nederlands",
    "pl" to "Polski",
    "ro" to "Rom\u00e2n\u0103",
    "sv" to "Svenska",
    "da" to "Dansk",
    "no" to "Norsk",
    "fi" to "Suomi",
    "cs" to "\u010ce\u0161tina",
    "hu" to "Magyar",
    "el" to "\u0395\u03bb\u03bb\u03b7\u03bd\u03b9\u03ba\u03ac",
    "tr" to "T\u00fcrk\u00e7e",
    "ar" to "\u0627\u0644\u0639\u0631\u0628\u064a\u0629",
    "ja" to "\u65e5\u672c\u8a9e",
    "zh" to "\u4e2d\u6587",
    "ko" to "\ud55c\uad6d\uc5b4",
    "ru" to "\u0420\u0443\u0441\u0441\u043a\u0438\u0439",
    "hi" to "\u0939\u093f\u0928\u094d\u0926\u0940",
    "id" to "Bahasa Indonesia",
    "ms" to "Bahasa Melayu",
    "th" to "\u0e44\u0e17\u0e22",
    "vi" to "Ti\u1ebfng Vi\u1ec7t",
    "he" to "\u05e2\u05d1\u05e8\u05d9\u05ea",
    "uk" to "\u0423\u043a\u0440\u0430\u0457\u043d\u0441\u044c\u043a\u0430",
    "bg" to "\u0411\u044a\u043b\u0433\u0430\u0440\u0441\u043a\u0438",
    "hr" to "Hrvatski",
    "sk" to "Sloven\u010dina",
    "sl" to "Sloven\u0161\u010dina",
    "lt" to "Lietuvi\u0173",
    "lv" to "Latvie\u0161u",
)

internal fun LazyListScope.openSubtitlesSettingsContent(
    isTablet: Boolean,
    settings: OpenSubtitlesSettings,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_opensubtitles_section_attribution),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                OpenSubtitlesAttributionRow(isTablet = isTablet)
            }
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_opensubtitles_enable),
                    description = stringResource(Res.string.settings_opensubtitles_enable_description),
                    checked = settings.enabled,
                    enabled = settings.hasApiKey,
                    isTablet = isTablet,
                    onCheckedChange = OpenSubtitlesSettingsRepository::setEnabled,
                )
                if (!settings.hasApiKey) {
                    SettingsGroupDivider(isTablet = isTablet)
                    Text(
                        text = stringResource(Res.string.settings_opensubtitles_add_api_key_first),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (isTablet) 20.dp else 16.dp,
                                vertical = if (isTablet) 12.dp else 10.dp,
                            ),
                    )
                }
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_opensubtitles_section_credentials),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                OpenSubtitlesApiKeyRow(
                    isTablet = isTablet,
                    value = settings.apiKey,
                    onApiKeyCommitted = OpenSubtitlesSettingsRepository::setApiKey,
                )
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_opensubtitles_section_languages),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                OpenSubtitlesLanguageSelector(
                    isTablet = isTablet,
                    selectedLanguages = settings.languages,
                    enabled = settings.hasApiKey,
                    onLanguagesChanged = OpenSubtitlesSettingsRepository::setLanguages,
                )
            }
        }
    }
}

@Composable
private fun OpenSubtitlesAttributionRow(isTablet: Boolean) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        Text(
            text = stringResource(Res.string.settings_opensubtitles_attribution_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(Res.string.settings_opensubtitles_attribution_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun OpenSubtitlesApiKeyRow(
    isTablet: Boolean,
    value: String,
    onApiKeyCommitted: (String) -> Unit,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    var draft by rememberSaveable(value) { mutableStateOf(value) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(Res.string.settings_opensubtitles_personal_api_key),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(Res.string.settings_opensubtitles_api_key_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val normalizedDraft = draft.trim()

        SettingsSecretTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.settings_opensubtitles_api_key_label),
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    draft = normalizedDraft
                    onApiKeyCommitted(normalizedDraft)
                },
                enabled = normalizedDraft != value,
            ) {
                Text(stringResource(Res.string.action_save))
            }
        }
    }
}

@Composable
private fun OpenSubtitlesLanguageSelector(
    isTablet: Boolean,
    selectedLanguages: Set<String>,
    enabled: Boolean,
    onLanguagesChanged: (Set<String>) -> Unit,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 12.dp else 10.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_opensubtitles_languages),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = stringResource(Res.string.settings_opensubtitles_languages_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SubtitleLanguages.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { (code, label) ->
                    val isChecked = code in selectedLanguages
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { newChecked ->
                                val updated = if (newChecked) {
                                    selectedLanguages + code
                                } else {
                                    selectedLanguages - code
                                }
                                onLanguagesChanged(updated)
                            },
                            enabled = enabled,
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (enabled) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}


