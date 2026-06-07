package com.nuvio.app.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.home.HomeCatalogDefinition
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CatalogPickerRow(
    title: String,
    selectedTitle: String?,
    icon: ImageVector,
    isTablet: Boolean,
    availableCatalogs: List<HomeCatalogDefinition>,
    onSelect: (HomeCatalogDefinition) -> Unit,
    onClear: () -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    SettingsNavigationRow(
        title = title,
        description = selectedTitle ?: stringResource(Res.string.catalog_picker_not_set),
        icon = icon,
        isTablet = isTablet,
        onClick = { showPicker = true },
    )

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(stringResource(Res.string.choose_catalog)) },
            text = {
                Column {
                    if (selectedTitle != null) {
                        Text(
                            text = stringResource(Res.string.clear_selection),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onClear()
                                    showPicker = false
                                }
                                .padding(vertical = 12.dp),
                        )
                    }
                    availableCatalogs.forEach { catalog ->
                        Text(
                            text = catalog.defaultTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(catalog)
                                    showPicker = false
                                }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}