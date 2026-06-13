@file:Suppress("unused")

package com.lagradost.cloudstream3

import android.app.Activity
import android.content.Context
import com.lagradost.api.setContext
import java.lang.ref.WeakReference

open class AcraApplication {
    companion object {
        @JvmStatic
        var context: Context? = null
            set(value) {
                field = value
                if (value != null) {
                    setContext(WeakReference(value))
                }
            }

        private var activityRef: WeakReference<Activity>? = null

        @JvmStatic
        fun getActivity(): Activity? = activityRef?.get()

        @JvmStatic
        fun setActivity(activity: Activity?) {
            activityRef = if (activity != null) WeakReference(activity) else null
            if (activity != null) {
                setContext(WeakReference(activity))
            }
        }

        @JvmStatic
        fun <T> getKey(path: String, key: String, default: T? = null): T? = default

        @JvmStatic
        fun <T> getKey(key: String, default: T? = null): T? = default

        @JvmStatic
        fun setKey(path: String, key: String, value: Any?) {}

        @JvmStatic
        fun setKey(key: String, value: Any?) {}

        @JvmStatic
        fun removeKeys(prefix: String) {}

        @JvmStatic
        fun removeKey(path: String, key: String) {}

        @JvmStatic
        fun removeKey(key: String) {}
    }
}
