package com.nuvio.app

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.TvLoginConfig
import com.nuvio.app.core.qr.QrCodeGenerator
import com.nuvio.app.core.ui.nuvio
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun TvAuthScreen(
    onSignedIn: () -> Unit = {},
) {
    val authError by AuthRepository.error.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    val colors = MaterialTheme.nuvio.colors
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .background(
                    color = colors.surface,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 40.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = colors.surfaceCard,
                        shape = RoundedCornerShape(12.dp)
                    ),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf("QR Code", "Email / Password").forEachIndexed { index, label ->
                    val isSelected = selectedTabIndex == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .background(
                                color = if (isSelected) colors.accent else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .focusable()
                            .onFocusChanged { if (it.isFocused) selectedTabIndex = index }
                            .clickable { selectedTabIndex = index }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) colors.onAccent else colors.textSecondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            when (selectedTabIndex) {
                0 -> QrCodeTab(
                    onSignedIn = onSignedIn,
                    colors = colors,
                    scheme = scheme,
                )
                1 -> EmailPasswordTab(
                    email = email,
                    onEmailChange = { email = it; AuthRepository.clearError() },
                    password = password,
                    onPasswordChange = { password = it; AuthRepository.clearError() },
                    isLoading = isLoading,
                    authError = authError,
                    onSignIn = {
                        if (email.isNotBlank() && password.length >= 6) {
                            isLoading = true
                            scope.launch {
                                AuthRepository.signInWithEmail(email, password)
                                isLoading = false
                            }
                        }
                    },
                    colors = colors,
                    scheme = scheme,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { scope.launch { AuthRepository.signInAnonymously() } },
                enabled = !isLoading,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .focusable(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surfaceCard,
                    contentColor = colors.textPrimary,
                ),
            ) {
                Text(
                    text = "Continue without account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun QrCodeTab(
    onSignedIn: () -> Unit,
    colors: com.nuvio.app.core.ui.NuvioColorTokens,
    scheme: androidx.compose.material3.ColorScheme,
) {
    var qrLoginCode by remember { mutableStateOf<String?>(null) }
    var qrLoginUrl by remember { mutableStateOf<String?>(null) }
    var qrLoginNonce by remember { mutableStateOf<String?>(null) }
    var qrLoginStatus by remember { mutableStateOf("Preparing...") }
    var qrLoginExpiresAtMillis by remember { mutableStateOf<Long?>(null) }
    var qrLoginPollInterval by remember { mutableStateOf(3) }
    var isQrLoading by remember { mutableStateOf(false) }

    val nowMillis by produceState(initialValue = System.currentTimeMillis(), key1 = qrLoginCode) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val remainingSecs = qrLoginExpiresAtMillis?.let { ((it - nowMillis) / 1000).coerceAtLeast(0) } ?: 0
    val sessionActive = qrLoginCode != null && remainingSecs > 0

    DisposableEffect(Unit) {
        onDispose {
            qrLoginCode = null
            qrLoginUrl = null
            qrLoginNonce = null
        }
    }

    LaunchedEffect(Unit) {
        if (!isQrLoading && qrLoginCode == null) {
            isQrLoading = true
            qrLoginStatus = "Preparing QR code..."
            val nonce = AuthRepository.generateDeviceNonce()
            qrLoginNonce = nonce
            AuthRepository.ensureQrSessionAuthenticated()
            AuthRepository.startTvLoginSession(
                deviceNonce = nonce,
                deviceName = Build.MODEL,
                redirectBaseUrl = TvLoginConfig.WEB_BASE_URL,
            ).onSuccess { result ->
                qrLoginCode = result.code
                qrLoginUrl = result.webUrl
                qrLoginStatus = "Scan this code with your phone"
                qrLoginExpiresAtMillis = runCatching {
                    java.time.Instant.parse(result.expiresAt).toEpochMilli()
                }.getOrNull()
                qrLoginPollInterval = result.pollIntervalSeconds.coerceAtLeast(2)
                isQrLoading = false
            }.onFailure {
                qrLoginStatus = "Failed to start QR login"
                isQrLoading = false
            }
        }
    }

    LaunchedEffect(sessionActive, qrLoginCode, qrLoginNonce) {
        if (!sessionActive) return@LaunchedEffect
        val code = qrLoginCode ?: return@LaunchedEffect
        val nonce = qrLoginNonce ?: return@LaunchedEffect
        var approved = false
        while (isActive && !approved && remainingSecs > 0) {
            delay(qrLoginPollInterval * 1000L)
            AuthRepository.pollTvLoginSession(code = code, deviceNonce = nonce)
                .onSuccess { result ->
                    when (result.status.lowercase()) {
                        "approved" -> {
                            approved = true
                            qrLoginStatus = "QR code approved! Signing in..."
                        }
                        "pending" -> {
                            qrLoginStatus = "Scan this code with your phone"
                        }
                        "expired", "used", "cancelled" -> {
                            qrLoginStatus = "QR code expired"
                            qrLoginCode = null
                        }
                    }
                }
        }
        if (approved) {
            AuthRepository.exchangeTvLoginSession(code = code, deviceNonce = nonce)
                .onSuccess {
                    qrLoginStatus = "Signed in successfully"
                    onSignedIn()
                }.onFailure {
                    qrLoginStatus = "Sign in failed"
                }
        }
    }

    val qrBitmap = remember(qrLoginUrl) {
        qrLoginUrl?.let { QrCodeGenerator.generate(it, 420) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Sign in with QR Code",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = qrLoginStatus,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (qrBitmap != null && qrLoginUrl != null) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .border(2.dp, colors.textMuted, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (remainingSecs > 0) {
                val minutes = remainingSecs / 60
                val seconds = remainingSecs % 60
                Text(
                    text = "Expires in ${minutes}:${seconds.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .border(2.dp, colors.textMuted, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading...",
                    color = colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun EmailPasswordTab(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    authError: String?,
    onSignIn: () -> Unit,
    colors: com.nuvio.app.core.ui.NuvioColorTokens,
    scheme: androidx.compose.material3.ColorScheme,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Sign in with Email",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            singleLine = true,
            placeholder = { Text("Email", color = colors.textSecondary) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = scheme.outline,
                focusedContainerColor = colors.surfaceCard,
                unfocusedContainerColor = colors.surfaceCard,
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            singleLine = true,
            placeholder = { Text("Password", color = colors.textSecondary) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSignIn() }),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = scheme.outline,
                focusedContainerColor = colors.surfaceCard,
                unfocusedContainerColor = colors.surfaceCard,
            ),
        )
        authError?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = error, color = scheme.error)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSignIn,
            enabled = email.isNotBlank() && password.length >= 6 && !isLoading,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .focusable(),
        ) {
            Text(
                text = if (isLoading) "Signing in..." else "Sign In",
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
