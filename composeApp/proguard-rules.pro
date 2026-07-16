# Keep @Serializable enum used as navigation route argument — must retain FQN for serialization.
-keep class com.nuvio.app.features.catalog.CatalogTargetKind { *; }

# Project-specific ProGuard rules for composeApp Android release builds.

# Keep useful metadata for crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Kotlin metadata/signatures needed by reflection/generics-heavy libraries.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations

# Ktor / Supabase client stack (runtime reflective paths in serializers/plugins).
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep @Serializable generated serializers.
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class com.nuvio.app.features.catalog.CatalogTargetKind { *; }

# Avoid R8 merging/optimizing the stream badge chip used in lazy stream rows.
-keep class com.nuvio.app.features.streams.StreamBadgeChipKt { *; }
-keep class com.nuvio.app.features.streams.StreamBadgeChipSize { *; }
-keep class com.nuvio.app.features.streams.StreamBadgeChipDefaults { *; }

-keep class com.nuvio.app.features.streams.StreamsScreenKt { *; }
-keep class com.nuvio.app.features.streams.StreamsScreenKt$* { *; }

# Avoid R8 producing verifier-invalid bytecode for the large player composable.
-keep class com.nuvio.app.features.player.PlayerScreenKt { *; }
-keep class com.nuvio.app.features.player.PlayerScreenKt$* { *; }

# QuickJS plugin runtime is dynamic; keep runtime and app plugin classes.
-keep class com.dokar.quickjs.** { *; }
-keep class com.nuvio.app.features.plugins.** { *; }

# CloudStream3 DEX extension compatibility stubs (loaded via DexClassLoader)
-keep class com.lagradost.cloudstream3.** { *; }
-keepclassmembers class com.lagradost.cloudstream3.** { *; }
-keep class com.lagradost.nicehttp.** { *; }
-keepclassmembers class com.lagradost.nicehttp.** { *; }
-keep class com.lagradost.api.** { *; }
-keepclassmembers class com.lagradost.api.** { *; }

# CloudStream integration runtime — UI state, serialization, plugin metadata
-keep class com.nuvio.app.features.cloudstream.** { *; }
-keep interface com.nuvio.app.features.cloudstream.** { *; }
-keep enum com.nuvio.app.features.cloudstream.** { *; }

# Jackson (used by CloudStream DataStore)
-keep class com.fasterxml.jackson.** { *; }
-keepclassmembers class com.fasterxml.jackson.** { *; }
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient

# DexClassLoader runtime deps — extensions resolve by FQN from host classloader
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }

# CloudStream skipped optional deps
-dontwarn org.mozilla.javascript.**
-dontwarn com.google.re2j.**

# TorrServer based P2P streaming.
-keep class com.nuvio.app.features.p2p.** { *; }

-keep class androidx.work.impl.WorkDatabase_Impl { *; }

# Media3 / ExoPlayer classes from local AAR decoders and stock modules.
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }
-keep interface com.google.android.exoplayer2.** { *; }

-keep class is.xyz.mpv.** { *; }
-keep interface is.xyz.mpv.** { *; }

# Common optional security providers used by okhttp on some devices.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Fix for NoClassDefFoundError in PlayerScreen onDispose lambda classes
-keep class com.nuvio.app.features.player.PlayerScreenKt** { *; }
-keep class * implements androidx.compose.runtime.DisposableEffectResult { *; }
