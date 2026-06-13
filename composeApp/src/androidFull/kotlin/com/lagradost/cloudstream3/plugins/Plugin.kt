package com.lagradost.cloudstream3.plugins

import android.app.Activity
import android.content.Context
import android.util.Log
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.extractorApis

open class Plugin {
    private val _registeredMainAPIs = mutableListOf<MainAPI>()
    private val _registeredExtractorAPIs = mutableListOf<ExtractorApi>()

    val registeredMainAPIs: List<MainAPI> get() = _registeredMainAPIs
    val registeredExtractorAPIs: List<ExtractorApi> get() = _registeredExtractorAPIs

    var openSettings: ((Context) -> Unit)? = null

    var filename: String? = null

    open fun load() {}

    @Suppress("UNUSED_PARAMETER")
    open fun load(activity: Activity?) {
        load()
    }

    fun registerMainAPI(element: MainAPI) {
        Log.d("CS3Plugin", "registerMainAPI called: ${element.name} (${element.javaClass.name})")
        _registeredMainAPIs.add(element)
        element.sourcePlugin = this.filename
        try {
            com.lagradost.cloudstream3.APIHolder.addPluginMapping(element)
        } catch (_: Exception) {}
    }

    fun registerExtractorAPI(element: ExtractorApi) {
        Log.d("CS3Plugin", "registerExtractorAPI called: ${element.name} (${element.javaClass.name})")
        _registeredExtractorAPIs.add(element)
        element.sourcePlugin = this.filename
        extractorApis.add(element)
    }

    open fun load(context: Context) {
        load(context as? Activity)
    }
}
