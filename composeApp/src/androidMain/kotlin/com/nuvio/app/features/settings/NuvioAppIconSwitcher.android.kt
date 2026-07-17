package com.nuvio.app.features.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object NuvioAppIconSwitcher {
    private const val TAG = "IconSwitcher"

    private val aliases = mapOf(
        NuvioAppIconOption.Default.id to "com.nuvio.enhanced.IconDefault",
        NuvioAppIconOption.Enhanced.id to "com.nuvio.enhanced.IconEnhanced",
        NuvioAppIconOption.Monochrome.id to "com.nuvio.enhanced.IconMonochrome",
        NuvioAppIconOption.Neon.id to "com.nuvio.enhanced.IconNeon",
        NuvioAppIconOption.Gear.id to "com.nuvio.enhanced.IconGear",
        NuvioAppIconOption.Chrome.id to "com.nuvio.enhanced.IconChrome",
        NuvioAppIconOption.Aurora.id to "com.nuvio.enhanced.IconAurora",
        NuvioAppIconOption.Emerald.id to "com.nuvio.enhanced.IconEmerald",
    )

    private var appContext: Context? = null
    private var pendingIconId: String? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun apply(iconId: String): Boolean {
        val context = appContext ?: return false.also { Log.w(TAG, "apply: appContext is null") }
        val targetAlias = aliases[iconId] ?: aliases[NuvioAppIconOption.Default.id]!!
        pendingIconId = iconId
        return runCatching {
            val pm = context.packageManager
            Log.i(TAG, "apply: target=$targetAlias iconId=$iconId DONT_KILL_APP")

            aliases.values.forEach { alias ->
                val cn = ComponentName(context, alias)
                val oldState = pm.getComponentEnabledSetting(cn)
                val newState = if (alias == targetAlias) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                pm.setComponentEnabledSetting(cn, newState, PackageManager.DONT_KILL_APP)
                val verified = pm.getComponentEnabledSetting(cn)
                Log.i(TAG, "$alias: $oldState -> $verified")
            }
            Log.i(TAG, "apply done - all DONT_KILL_APP")
        }.onFailure { e ->
            Log.e(TAG, "apply failed", e)
        }.isSuccess
    }

    actual fun reapply(iconId: String): Boolean {
        val context = appContext ?: return false.also { Log.w(TAG, "reapply: appContext is null") }
        val targetAlias = aliases[iconId] ?: aliases[NuvioAppIconOption.Default.id]!!
        return runCatching {
            val pm = context.packageManager
            Log.i(TAG, "reapply: target=$targetAlias iconId=$iconId DONT_KILL_APP")

            aliases.values.forEach { alias ->
                val cn = ComponentName(context, alias)
                val oldState = pm.getComponentEnabledSetting(cn)
                val newState = if (alias == targetAlias) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                pm.setComponentEnabledSetting(cn, newState, PackageManager.DONT_KILL_APP)
                val verified = pm.getComponentEnabledSetting(cn)
                Log.i(TAG, "$alias: $oldState -> $verified")
            }
            Log.i(TAG, "reapply done - all DONT_KILL_APP")
        }.onFailure { e ->
            Log.e(TAG, "reapply failed", e)
        }.isSuccess
    }

    actual fun closeAfterApply() {
        val context = appContext ?: run {
            Log.w(TAG, "closeAfterApply: appContext is null")
            return
        }
        val iconId = pendingIconId ?: run {
            Log.w(TAG, "closeAfterApply: no pending icon")
            return
        }
        val targetAlias = aliases[iconId] ?: aliases[NuvioAppIconOption.Default.id]!!
        runCatching {
            Log.i(TAG, "closeAfterApply: saving prefs with commit()")
            context.getSharedPreferences("nuvio_theme_settings", Context.MODE_PRIVATE)
                .edit()
                .putString(ProfileScopedKey.of("selected_app_icon_id"), iconId)
                .commit()

            Log.i(TAG, "closeAfterApply: enabling $targetAlias with flags=0 (system will kill + broadcast)")
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, targetAlias),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                0,
            )
            Log.i(TAG, "closeAfterApply: enable call returned - process may be killed")
        }.onFailure { e ->
            Log.e(TAG, "closeAfterApply failed", e)
        }
    }
}
