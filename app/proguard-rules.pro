# Add project specific ProGuard rules here.
# By default, the flags in this file are applied to
# all build types.

# Hilt
-keep class dagger.hilt.internal.aggregatedroot.codegen.*
-keep class com.hereliesaz.lexorcist.di.*
-keep @dagger.hilt.InstallIn class *
-keep @dagger.hilt.components.SingletonComponent class *
-keep @dagger.hilt.android.HiltAndroidApp class *

# Coroutines
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory {
    public static final kotlinx.coroutines.MainCoroutineDispatcher a;
}

# Data models
-keep class com.hereliesaz.lexorcist.model.** { *; }
-keep class com.hereliesaz.lexorcist.data.** { *; }

# Keep serialization classes
-keep class kotlinx.serialization.** { *; }
-keep class * implements kotlinx.serialization.Serializer { *; }
-keep class * extends kotlinx.serialization.internal.EnumSerializer { *; }

# For Gson
-keep class com.google.gson.** { *; }

# For Retrofit (used by Google API services)
-keep class retrofit2.** { *; }
-keep class com.google.api.services.** { *; }

# For MediaPipe
-keep class com.google.mediapipe.** { *; }
