package com.nuvio.app.core.build

object AppVersionPolicy {
    val displayVersionName: String = AppVersionConfig.DESKTOP_VERSION_NAME
    val displayVersionCode: Int = AppVersionConfig.DESKTOP_VERSION_CODE
    val basedOnVersionName: String? = AppVersionConfig.VERSION_NAME.takeIf { it != displayVersionName }
}
