package com.nuvio.app.core.platform

import kotlin.JsFun

@JsFun("() => Date.now()")
private external fun jsNowEpochMs(): Double

@JsFun("() => new Date().toISOString().slice(0, 10)")
private external fun jsTodayIsoDate(): String

@JsFun("(epochMs) => new Date(Number(epochMs)).toISOString().slice(0, 10)")
private external fun jsIsoDateFromEpochMs(epochMs: Double): String

@JsFun("(value) => Date.parse(value)")
private external fun jsParseEpochMs(value: String): Double

@JsFun("(key) => { try { return globalThis.localStorage?.getItem(key) ?? null; } catch (_) { return null; } }")
private external fun jsStorageGet(key: String): String?

@JsFun("(key, value) => { try { globalThis.localStorage?.setItem(key, value); } catch (_) {} }")
private external fun jsStorageSet(key: String, value: String)

@JsFun("(key) => { try { globalThis.localStorage?.removeItem(key); } catch (_) {} }")
private external fun jsStorageRemove(key: String)

@JsFun("() => { try { return globalThis.localStorage?.length ?? 0; } catch (_) { return 0; } }")
private external fun jsStorageLength(): Int

@JsFun("(index) => { try { return globalThis.localStorage?.key(index) ?? null; } catch (_) { return null; } }")
private external fun jsStorageKey(index: Int): String?

@JsFun("(url) => { try { globalThis.open(url, '_blank', 'noopener,noreferrer'); return true; } catch (_) { return false; } }")
internal external fun openWebUrl(url: String): Boolean

@JsFun("() => { try { return (globalThis.navigator?.languages ?? [globalThis.navigator?.language]).filter(Boolean).join(','); } catch (_) { return ''; } }")
private external fun jsNavigatorLanguages(): String

@JsFun("(url, headersJson, startPositionMs) => { try { return globalThis.nuvioQtPlayNative?.(url, headersJson, Number(startPositionMs || 0)) === true; } catch (_) { return false; } }")
internal external fun playQtNativeMedia(url: String, headersJson: String, startPositionMs: Double): Boolean

@JsFun("() => { try { return globalThis.nuvioQtNativeHost === true || typeof globalThis.nuvioQtPlayNative === 'function'; } catch (_) { return false; } }")
internal external fun isQtNativePlayerHost(): Boolean

@JsFun("(command, value) => { try { return globalThis.nuvioQtCommandNative?.(command, Number(value || 0)) === true; } catch (_) { return false; } }")
internal external fun commandQtNativePlayer(command: String, value: Double): Boolean

@JsFun("(command, value) => { try { return globalThis.nuvioQtStringCommandNative?.(command, String(value || '')) === true; } catch (_) { return false; } }")
internal external fun commandQtNativePlayerString(command: String, value: String): Boolean

@JsFun("(contextJson) => { try { return globalThis.nuvioQtSetPlayerContext?.(contextJson) === true; } catch (_) { return false; } }")
internal external fun setQtNativePlayerContext(contextJson: String): Boolean

@JsFun("() => { try { return globalThis.nuvioQtConsumePlayerAction?.() || ''; } catch (_) { return ''; } }")
internal external fun consumeQtNativePlayerAction(): String

@JsFun("() => { try { return globalThis.nuvioQtTakePlayerSnapshot?.() || ''; } catch (_) { return ''; } }")
internal external fun takeQtNativePlayerSnapshot(): String

@JsFun("() => { try { return globalThis.nuvioQtTakePlayerTracks?.() || ''; } catch (_) { return ''; } }")
internal external fun takeQtNativePlayerTracks(): String

internal fun webNowEpochMs(): Long = jsNowEpochMs().toLong()

internal fun webTodayIsoDate(): String = jsTodayIsoDate()

internal fun webIsoDateFromEpochMs(epochMs: Long): String = jsIsoDateFromEpochMs(epochMs.toDouble())

internal fun webParseIsoDateTimeToEpochMs(value: String): Long? {
    val parsed = jsParseEpochMs(value)
    return if (parsed.isNaN()) null else parsed.toLong()
}

internal fun webPreferredLanguageCodes(): List<String> =
    jsNavigatorLanguages()
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

internal object WebKeyValueStorage {
    private const val prefix = "nuvio:"

    fun getString(namespace: String, key: String): String? =
        jsStorageGet(storageKey(namespace, key))

    fun setString(namespace: String, key: String, value: String) {
        jsStorageSet(storageKey(namespace, key), value)
    }

    fun remove(namespace: String, key: String) {
        jsStorageRemove(storageKey(namespace, key))
    }

    fun contains(namespace: String, key: String): Boolean =
        getString(namespace, key) != null

    fun getBoolean(namespace: String, key: String): Boolean? =
        getString(namespace, key)?.toBooleanStrictOrNull()

    fun setBoolean(namespace: String, key: String, value: Boolean) {
        setString(namespace, key, value.toString())
    }

    fun getInt(namespace: String, key: String): Int? =
        getString(namespace, key)?.toIntOrNull()

    fun setInt(namespace: String, key: String, value: Int) {
        setString(namespace, key, value.toString())
    }

    fun getFloat(namespace: String, key: String): Float? =
        getString(namespace, key)?.toFloatOrNull()

    fun setFloat(namespace: String, key: String, value: Float) {
        setString(namespace, key, value.toString())
    }

    fun removeScoped(namespace: String, keys: Iterable<String>) {
        keys.forEach { remove(namespace, it) }
    }

    fun wipeAll() {
        val keys = mutableListOf<String>()
        for (index in 0 until jsStorageLength()) {
            val key = jsStorageKey(index)
            if (key != null && key.startsWith(prefix)) {
                keys += key
            }
        }
        keys.forEach(::jsStorageRemove)
    }

    private fun storageKey(namespace: String, key: String): String =
        "$prefix$namespace:$key"
}
