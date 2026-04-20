# ═══════════════════════════════════════════════════════════════════════════════
# GENERAL ATTRIBUTES
# ═══════════════════════════════════════════════════════════════════════════════
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions

# ═══════════════════════════════════════════════════════════════════════════════
# HILT / DAGGER
# ═══════════════════════════════════════════════════════════════════════════════
-keep class dagger.** { *; }
-keep interface dagger.** { *; }
-keep class dagger.hilt.** { *; }
-keep interface dagger.hilt.** { *; }

-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# HiltViewModel — keep annotated ViewModels and their constructors
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Hilt generated classes — must never be renamed
-keep class **Hilt_* { *; }
-keep class **_HiltModules { *; }
-keep class **_HiltModules$* { *; }
-keep class **_HiltComponents { *; }
-keep class **_HiltComponents$* { *; }
-keep class **$$HiltComponents { *; }
-keep class **_ComponentTreeDeps { *; }
-keep class *_GeneratedInjector { *; }
-keep class **_MembersInjector { *; }
-keep class **_Factory { *; }
-keep class **_Provide*Factory { *; }
-keep class **_Inject*Factory { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Hilt internal
-keep class dagger.hilt.android.internal.** { *; }
-keep class dagger.hilt.internal.** { *; }

# ═══════════════════════════════════════════════════════════════════════════════
# JAVAX INJECT
# ═══════════════════════════════════════════════════════════════════════════════
-keep class javax.inject.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
    @javax.inject.Inject <methods>;
}

# ═══════════════════════════════════════════════════════════════════════════════
# VIEWMODEL / LIFECYCLE
# ═══════════════════════════════════════════════════════════════════════════════
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class androidx.lifecycle.** { *; }
-keep interface androidx.lifecycle.** { *; }

# ═══════════════════════════════════════════════════════════════════════════════
# RETROFIT
# ═══════════════════════════════════════════════════════════════════════════════
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# ═══════════════════════════════════════════════════════════════════════════════
# OKHTTP
# ═══════════════════════════════════════════════════════════════════════════════
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okio.** { *; }

# ═══════════════════════════════════════════════════════════════════════════════
# GSON
# ═══════════════════════════════════════════════════════════════════════════════
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }
-keepclassmembers enum * { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }

# ═══════════════════════════════════════════════════════════════════════════════
# APP DATA MODELS (your package)
# ═══════════════════════════════════════════════════════════════════════════════
-keep class com.share_manager.data.model.** { *; }
-keepclassmembers class com.share_manager.data.model.** {
    <init>(...);
    <fields>;
}

# ═══════════════════════════════════════════════════════════════════════════════
# ROOM
# ═══════════════════════════════════════════════════════════════════════════════
-keep class androidx.room.** { *; }
-keep interface androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keepclassmembers @androidx.room.Entity class * {
    <init>(...);
    <fields>;
}

# ═══════════════════════════════════════════════════════════════════════════════
# KOTLIN
# ═══════════════════════════════════════════════════════════════════════════════
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Kotlin serialization (if used)
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}

# ═══════════════════════════════════════════════════════════════════════════════
# ANDROIDX / COMPOSE
# ═══════════════════════════════════════════════════════════════════════════════
-keep class androidx.compose.** { *; }
-keep class androidx.navigation.** { *; }
-keep class androidx.activity.** { *; }
-dontwarn androidx.compose.**

# ═══════════════════════════════════════════════════════════════════════════════
# ANDROID SECURITY CRYPTO
# ═══════════════════════════════════════════════════════════════════════════════
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ═══════════════════════════════════════════════════════════════════════════════
# SUPPRESS WARNINGS
# ═══════════════════════════════════════════════════════════════════════════════
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**