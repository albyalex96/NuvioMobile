package com.nuvio.app.features.settings

import android.content.Context
import android.content.SharedPreferences

class AndroidNetworkSettingsStorage(context: Context) : NetworkSettingsStorage {
    private val prefs: SharedPreferences = context.getSharedPreferences("nuvio_network_settings", Context.MODE_PRIVATE)
    private val DNS_PROVIDER_KEY = "dns_provider"
    private val CUSTOM_USER_AGENT_KEY = "custom_user_agent"
    private val OVERRIDE_FOR_ADDONS_KEY = "override_for_addons"
    private val OVERRIDE_FOR_PLUGINS_KEY = "override_for_plugins"
    private val OVERRIDE_FOR_BOTH_KEY = "override_for_both"

    override fun getDnsProvider(): String? =
        prefs.getString(DNS_PROVIDER_KEY, null)

    override fun setDnsProvider(provider: String) {
        prefs.edit().putString(DNS_PROVIDER_KEY, provider).apply()
    }

    override fun getCustomUserAgent(): String? =
        prefs.getString(CUSTOM_USER_AGENT_KEY, null)

    override fun setCustomUserAgent(value: String) {
        prefs.edit().putString(CUSTOM_USER_AGENT_KEY, value).apply()
    }

    override fun getOverrideForAddons(): Boolean =
        prefs.getBoolean(OVERRIDE_FOR_ADDONS_KEY, false)

    override fun setOverrideForAddons(enabled: Boolean) {
        prefs.edit().putBoolean(OVERRIDE_FOR_ADDONS_KEY, enabled).apply()
    }

    override fun getOverrideForPlugins(): Boolean =
        prefs.getBoolean(OVERRIDE_FOR_PLUGINS_KEY, false)

    override fun setOverrideForPlugins(enabled: Boolean) {
        prefs.edit().putBoolean(OVERRIDE_FOR_PLUGINS_KEY, enabled).apply()
    }

    override fun getOverrideForBoth(): Boolean =
        prefs.getBoolean(OVERRIDE_FOR_BOTH_KEY, false)

    override fun setOverrideForBoth(enabled: Boolean) {
        prefs.edit().putBoolean(OVERRIDE_FOR_BOTH_KEY, enabled).apply()
    }
}
