package com.nuvio.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.ui.NuvioTheme

@Composable
fun TvApp() {
    val authState by AuthRepository.state.collectAsState()
    val isAuthenticated = authState != null

    NuvioTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = isAuthenticated,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "authGate"
            ) { signedIn ->
                if (signedIn) {
                    TvMainContent()
                } else {
                    TvAuthScreen()
                }
            }
        }
    }
}

@Composable
private fun TvMainContent() {
    TextOnTv(
        text = "Nuvio Enhanced TV - Ready",
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    )
}

@Composable
private fun TextOnTv(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = text,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
