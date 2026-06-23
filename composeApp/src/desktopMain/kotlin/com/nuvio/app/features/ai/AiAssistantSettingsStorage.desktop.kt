package com.nuvio.app.features.ai

import com.nuvio.app.core.storage.ProfileScopedKey
import java.util.prefs.Preferences

internal actual object AiAssistantSettingsStorage {
    private val prefs: Preferences = Preferences.userNodeForPackage(AiAssistantSettingsStorage::class.java)

    actual fun loadEnabled(): Boolean? = loadBoolean("ai_assistant_enabled")
    actual fun saveEnabled(value: Boolean) = saveBoolean("ai_assistant_enabled", value)
    actual fun loadWebSearchEnabled(): Boolean? = loadBoolean("ai_assistant_web_search_enabled")
    actual fun saveWebSearchEnabled(value: Boolean) = saveBoolean("ai_assistant_web_search_enabled", value)
    actual fun loadProvider(): String? = loadString("ai_assistant_provider")
    actual fun saveProvider(value: String) = saveString("ai_assistant_provider", value)
    actual fun loadTavilyApiKey(): String? = loadString("ai_assistant_tavily_api_key")
    actual fun saveTavilyApiKey(value: String) = saveString("ai_assistant_tavily_api_key", value)
    actual fun loadCerebrasApiKey(): String? = loadString("ai_assistant_cerebras_api_key")
    actual fun saveCerebrasApiKey(value: String) = saveString("ai_assistant_cerebras_api_key", value)
    actual fun loadGroqApiKey(): String? = loadString("ai_assistant_groq_api_key")
    actual fun saveGroqApiKey(value: String) = saveString("ai_assistant_groq_api_key", value)
    actual fun loadGeminiApiKey(): String? = loadString("ai_assistant_gemini_api_key")
    actual fun saveGeminiApiKey(value: String) = saveString("ai_assistant_gemini_api_key", value)
    actual fun loadOpenRouterApiKey(): String? = loadString("ai_assistant_openrouter_api_key")
    actual fun saveOpenRouterApiKey(value: String) = saveString("ai_assistant_openrouter_api_key", value)
    actual fun loadCerebrasModel(): String? = loadString("ai_assistant_cerebras_model")
    actual fun saveCerebrasModel(value: String) = saveString("ai_assistant_cerebras_model", value)
    actual fun loadGroqModel(): String? = loadString("ai_assistant_groq_model")
    actual fun saveGroqModel(value: String) = saveString("ai_assistant_groq_model", value)
    actual fun loadGeminiModel(): String? = loadString("ai_assistant_gemini_model")
    actual fun saveGeminiModel(value: String) = saveString("ai_assistant_gemini_model", value)
    actual fun loadOpenRouterModel(): String? = loadString("ai_assistant_openrouter_model")
    actual fun saveOpenRouterModel(value: String) = saveString("ai_assistant_openrouter_model", value)

    private fun loadString(key: String): String? {
        val fullKey = ProfileScopedKey.of(key)
        return prefs.get(fullKey, null)
    }

    private fun saveString(key: String, value: String) {
        prefs.put(ProfileScopedKey.of(key), value)
    }

    private fun loadBoolean(key: String): Boolean? {
        val fullKey = ProfileScopedKey.of(key)
        if (prefs.get(fullKey, null) == null) return null
        return prefs.getBoolean(fullKey, false)
    }

    private fun saveBoolean(key: String, value: Boolean) {
        prefs.putBoolean(ProfileScopedKey.of(key), value)
    }
}
