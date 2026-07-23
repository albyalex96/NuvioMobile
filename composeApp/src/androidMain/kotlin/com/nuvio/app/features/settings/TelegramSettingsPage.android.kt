package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.nuvio.app.features.telegram.TelegramAuthState
import com.nuvio.app.features.telegram.TelegramRepository
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_telegram_2fa_card_title
import nuvio.composeapp.generated.resources.settings_telegram_2fa_instruction
import nuvio.composeapp.generated.resources.settings_telegram_2fa_label
import nuvio.composeapp.generated.resources.settings_telegram_clear_cache
import nuvio.composeapp.generated.resources.settings_telegram_code_card_title
import nuvio.composeapp.generated.resources.settings_telegram_code_instruction
import nuvio.composeapp.generated.resources.settings_telegram_code_label
import nuvio.composeapp.generated.resources.settings_telegram_connected_title
import nuvio.composeapp.generated.resources.settings_telegram_description
import nuvio.composeapp.generated.resources.settings_telegram_disconnect
import nuvio.composeapp.generated.resources.settings_telegram_error_title
import nuvio.composeapp.generated.resources.settings_telegram_heading
import nuvio.composeapp.generated.resources.settings_telegram_initializing
import nuvio.composeapp.generated.resources.settings_telegram_logged_in_as
import nuvio.composeapp.generated.resources.settings_telegram_media_cache
import nuvio.composeapp.generated.resources.settings_telegram_phone_card_title
import nuvio.composeapp.generated.resources.settings_telegram_phone_instruction
import nuvio.composeapp.generated.resources.settings_telegram_phone_label
import nuvio.composeapp.generated.resources.settings_telegram_phone_placeholder
import nuvio.composeapp.generated.resources.settings_telegram_qr_scan
import nuvio.composeapp.generated.resources.settings_telegram_retry
import nuvio.composeapp.generated.resources.settings_telegram_send_code
import nuvio.composeapp.generated.resources.settings_telegram_submit_password
import nuvio.composeapp.generated.resources.settings_telegram_verify_code
import org.jetbrains.compose.resources.stringResource

internal actual fun LazyListScope.telegramSettingsContent(
    isTablet: Boolean,
) {
    item {
        val authState by TelegramRepository.authState.collectAsState()
        var phoneInput by remember { mutableStateOf("+") }
        var codeInput by remember { mutableStateOf("") }
        var passwordInput by remember { mutableStateOf("") }
        var cacheSize by remember { mutableStateOf(TelegramRepository.getCacheSize()) }
        var waitingForClient by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            TelegramRepository.startAuth()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.settings_telegram_heading),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = stringResource(Res.string.settings_telegram_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (val state = authState) {
                is TelegramAuthState.Idle, is TelegramAuthState.WaitPhone -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(Res.string.settings_telegram_phone_card_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.settings_telegram_phone_instruction),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = {
                                    val cleaned = it.filter { c -> c.isDigit() || c == '+' }
                                    phoneInput = if (cleaned.isEmpty() || cleaned.first() != '+') "+$cleaned" else cleaned
                                },
                                label = { Text(stringResource(Res.string.settings_telegram_phone_label)) },
                                placeholder = { Text(stringResource(Res.string.settings_telegram_phone_placeholder)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (phoneInput.isNotBlank()) TelegramRepository.submitPhone(phoneInput)
                                }),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (phoneInput.isNotBlank() && !waitingForClient) {
                                        if (TelegramRepository.isClientReady()) {
                                            TelegramRepository.submitPhone(phoneInput)
                                        } else {
                                            waitingForClient = true
                                            TelegramRepository.startAuth()
                                            coroutineScope.launch {
                                                TelegramRepository.awaitClient()
                                                TelegramRepository.submitPhone(phoneInput)
                                                waitingForClient = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                if (waitingForClient) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(stringResource(Res.string.settings_telegram_send_code))
                                }
                            }
                        }
                    }
                }

                is TelegramAuthState.Initializing -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp))
                            Text(stringResource(Res.string.settings_telegram_initializing))
                        }
                    }
                }

                is TelegramAuthState.WaitCode -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(Res.string.settings_telegram_code_card_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.settings_telegram_code_instruction, state.codeLength),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = codeInput,
                                onValueChange = { codeInput = it },
                                label = { Text(stringResource(Res.string.settings_telegram_code_label)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (codeInput.isNotBlank()) TelegramRepository.submitCode(codeInput)
                                }),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (codeInput.isNotBlank()) TelegramRepository.submitCode(codeInput)
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(stringResource(Res.string.settings_telegram_verify_code))
                            }
                        }
                    }
                }

                is TelegramAuthState.WaitPassword -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(Res.string.settings_telegram_2fa_card_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.settings_telegram_2fa_instruction),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text(stringResource(Res.string.settings_telegram_2fa_label)) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (passwordInput.isNotBlank()) TelegramRepository.submitPassword(passwordInput)
                                }),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (passwordInput.isNotBlank()) TelegramRepository.submitPassword(passwordInput)
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(stringResource(Res.string.settings_telegram_submit_password))
                            }
                        }
                    }
                }

                is TelegramAuthState.Ready -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(Res.string.settings_telegram_connected_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.settings_telegram_logged_in_as, state.firstName, state.userId),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(Res.string.settings_telegram_media_cache, formatBytes(cacheSize)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = {
                                        TelegramRepository.clearCache()
                                        cacheSize = TelegramRepository.getCacheSize()
                                    }
                                ) {
                                    Text(stringResource(Res.string.settings_telegram_clear_cache))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    TelegramRepository.disconnect()
                                    cacheSize = 0L
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(stringResource(Res.string.settings_telegram_disconnect))
                            }
                        }
                    }
                }

                is TelegramAuthState.WaitQr -> {
                    Text(stringResource(Res.string.settings_telegram_qr_scan, state.link))
                }

                is TelegramAuthState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(Res.string.settings_telegram_error_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { TelegramRepository.startAuth() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(stringResource(Res.string.settings_telegram_retry))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> "0 KB"
    bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    else -> "%.0f KB".format(bytes / 1_000.0)
}
