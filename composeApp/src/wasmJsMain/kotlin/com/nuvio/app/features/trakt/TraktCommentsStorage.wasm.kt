package com.nuvio.app.features.trakt

import com.nuvio.app.WebStorage
import kotlinx.serialization.json.*

internal actual object TraktCommentsStorage {
    private const val KEY = "nuvio_trakt_comments"

    private fun prefs(): JsonObject {
        val raw = WebStorage.getString(KEY) ?: return JsonObject(emptyMap())
        return Json.parseToJsonElement(raw).jsonObject
    }
    private fun save(obj: JsonObject) { WebStorage.setString(KEY, Json.encodeToString(JsonObject.serializer(), obj)) }

    actual fun loadEnabled(): Boolean? = prefs()["enabled"]?.jsonPrimitive?.booleanOrNull
    actual fun saveEnabled(enabled: Boolean) { val p = prefs().toMutableMap(); p["enabled"] = JsonPrimitive(enabled); save(JsonObject(p)) }
    actual fun exportToSyncPayload(): JsonObject = prefs()
    actual fun replaceFromSyncPayload(payload: JsonObject) { WebStorage.setString(KEY, Json.encodeToString(JsonObject.serializer(), payload)) }
}
