package com.nuvio.app.features.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DnsProvider(val displayName: String) {
    SYSTEM("System Default (IPv4 Preferred)"),
    CLOUDFLARE("Cloudflare (DoH)"),
    GOOGLE("Google (DoH)"),
    QUAD9("Quad9 (DoH)"),
    ADGUARD("AdGuard (DoH)"),
    NEXTDNS("NextDNS (DoH)"),
    MULLVAD("Mullvad (DoH)"),
    OPEN_DNS("OpenDNS (DoH)")
}

interface NetworkSettingsStorage {
    fun getDnsProvider(): String?
    fun setDnsProvider(provider: String)
    fun getCustomUserAgent(): String?
    fun setCustomUserAgent(value: String)
    fun getOverrideForAddons(): Boolean
    fun setOverrideForAddons(enabled: Boolean)
    fun getOverrideForPlugins(): Boolean
    fun setOverrideForPlugins(enabled: Boolean)
    fun getOverrideForBoth(): Boolean
    fun setOverrideForBoth(enabled: Boolean)
}

class NetworkSettingsRepository(
    private val storage: NetworkSettingsStorage
) {
    private val _dnsProvider = MutableStateFlow(
        runCatching {
            val name = storage.getDnsProvider() ?: DnsProvider.SYSTEM.name
            DnsProvider.valueOf(name)
        }.getOrDefault(DnsProvider.SYSTEM)
    )
    val dnsProvider: StateFlow<DnsProvider> = _dnsProvider.asStateFlow()

    private val _customUserAgent = MutableStateFlow(storage.getCustomUserAgent() ?: "")
    val customUserAgent: StateFlow<String> = _customUserAgent.asStateFlow()

    private val _overrideForAddons = MutableStateFlow(storage.getOverrideForAddons())
    val overrideForAddons: StateFlow<Boolean> = _overrideForAddons.asStateFlow()

    private val _overrideForPlugins = MutableStateFlow(storage.getOverrideForPlugins())
    val overrideForPlugins: StateFlow<Boolean> = _overrideForPlugins.asStateFlow()

    private val _overrideForBoth = MutableStateFlow(storage.getOverrideForBoth())
    val overrideForBoth: StateFlow<Boolean> = _overrideForBoth.asStateFlow()

    fun setDnsProvider(provider: DnsProvider) {
        storage.setDnsProvider(provider.name)
        _dnsProvider.value = provider
    }

    fun setCustomUserAgent(value: String) {
        storage.setCustomUserAgent(value)
        _customUserAgent.value = value
    }

    fun setOverrideForAddons(enabled: Boolean) {
        if (enabled) {
            storage.setOverrideForPlugins(false)
            storage.setOverrideForBoth(false)
            _overrideForPlugins.value = false
            _overrideForBoth.value = false
        }
        storage.setOverrideForAddons(enabled)
        _overrideForAddons.value = enabled
    }

    fun setOverrideForPlugins(enabled: Boolean) {
        if (enabled) {
            storage.setOverrideForAddons(false)
            storage.setOverrideForBoth(false)
            _overrideForAddons.value = false
            _overrideForBoth.value = false
        }
        storage.setOverrideForPlugins(enabled)
        _overrideForPlugins.value = enabled
    }

    fun setOverrideForBoth(enabled: Boolean) {
        if (enabled) {
            storage.setOverrideForAddons(false)
            storage.setOverrideForPlugins(false)
            _overrideForAddons.value = false
            _overrideForPlugins.value = false
        }
        storage.setOverrideForBoth(enabled)
        _overrideForBoth.value = enabled
    }
}

var globalNetworkSettingsRepository: NetworkSettingsRepository? = null
