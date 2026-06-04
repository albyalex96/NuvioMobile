package com.nuvio.app.core.share

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSArray
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject

internal actual object SharePlatform {
    actual fun shareFile(filePath: String, title: String, mimeType: String) {
        val url = NSURL.fileURLWithPath(filePath) ?: return
        val activityItems = listOf(url)
        val controller = UIActivityViewController(
            activityItems = activityItems as List<NSObject>,
            applicationActivities = null,
        )
        controller.setValue(title, forKey = "subject")
        topMostViewController()?.presentViewController(controller, animated = true, completion = null)
    }

    private fun topMostViewController(): UIViewController? {
        val keyWindow = UIApplication.sharedApplication.keyWindow ?: return null
        var topController = keyWindow.rootViewController
        while (topController?.presentedViewController != null) {
            topController = topController.presentedViewController
        }
        return topController
    }
}
