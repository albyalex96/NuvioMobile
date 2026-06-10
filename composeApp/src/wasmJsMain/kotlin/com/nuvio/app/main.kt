@file:OptIn(ExperimentalComposeUiApi::class)

package com.nuvio.app

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

fun main() {
    ComposeViewport(viewportContainerId = "webApp") {
        var ready by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            runCatching { getString(StringResource("action_ok")) }
            ready = true
        }

        if (ready) {
            App()
        }
    }
}
