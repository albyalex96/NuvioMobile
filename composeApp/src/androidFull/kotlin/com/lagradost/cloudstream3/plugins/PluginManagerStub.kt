@file:Suppress("unused")

package com.lagradost.cloudstream3.plugins

object PluginManager {
    data class PluginData(
        val name: String = "",
        val url: String = "",
        val internalName: String = "",
        val version: Int = 0
    )

    fun getPluginsOnline(): Array<PluginData> = emptyArray()
    fun unloadPlugin(filePath: String) {}
}
