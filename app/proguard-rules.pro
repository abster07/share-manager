# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.google.gson.** { *; }
-keep class com.share_manager.data.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Room
-keep class androidx.room.** { *; }

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep data classes used in API responses
-keepclassmembers class com.share_manager.data.model.** {
    <init>(...);
    <fields>;
}

# ── Hilt ViewModels (CRITICAL — prevents ClassCastException) ──────────────────
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclassmembers @dagger.hilt.android.lifecycle.HiltViewModel class * {
    <init>(...);
}

# Keep all ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Hilt internal generated components (must not be renamed)
-keep class **_HiltComponents { *; }
-keep class **_ComponentTreeDeps { *; }
-keep class *_GeneratedInjector { *; }
-keep class **Hilt_* { *; }

# Hilt — keep all generated and injected members
-keepclasseswithmembers class * {
    @dagger.hilt.* <fields>;
    @dagger.hilt.* <methods>;
}

# javax.inject
-keep class javax.inject.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}