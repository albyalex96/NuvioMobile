package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_telegram_heading
import nuvio.composeapp.generated.resources.settings_telegram_unavailable
import org.jetbrains.compose.resources.stringResource

internal actual fun LazyListScope.telegramSettingsContent(
    isTablet: Boolean,
) {
    item {
        Text(
            text = stringResource(Res.string.settings_telegram_heading),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
    item {
        Text(
            text = stringResource(Res.string.settings_telegram_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
    }
}
