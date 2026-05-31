package com.nuvio.app.features.home

internal expect object Top10CatalogStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}