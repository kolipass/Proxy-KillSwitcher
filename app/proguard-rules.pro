# Application Proguard/R8 Rules for ProxyKillSwitcher

# -----------------------------------------------------------------------------
# Kotlin & Coroutines
# -----------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, SourceFile, LineNumberTable

# Coroutines internal stacktrace recovery
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# -----------------------------------------------------------------------------
# Kotlin Serialization
# -----------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepclassmembers class * {
    companion <fields>;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    public static final ** Companion;
}

# -----------------------------------------------------------------------------
# Metro DI
# -----------------------------------------------------------------------------
-dontwarn dev.zacsweers.metro.**
-keep class dev.zacsweers.metro.** { *; }
-keep @dev.zacsweers.metro.* class * { *; }
-keepclasseswithmembers class * {
    @dev.zacsweers.metro.* <methods>;
    @dev.zacsweers.metro.* <fields>;
    @dev.zacsweers.metro.* <init>(...);
}

# -----------------------------------------------------------------------------
# Orbit MVI
# -----------------------------------------------------------------------------
-dontwarn org.orbitmvi.orbit.**
-keep class org.orbitmvi.orbit.** { *; }

# -----------------------------------------------------------------------------
# AndroidX Navigation & Compose
# -----------------------------------------------------------------------------
-keepclassmembers class * implements androidx.navigation.NavArgs {
    public static ** fromBundle(android.os.Bundle);
}
