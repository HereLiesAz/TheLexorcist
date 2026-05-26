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

# Keep Hilt DI subpackages (qualifiers, generated modules), not just the top level.
-keep class com.hereliesaz.lexorcist.di.** { *; }

# Mozilla Rhino — the scripting engine reflects over its own classes at runtime.
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**

# Apache POI (XLSX read/write) — heavy reflection + optional deps.
-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.openxmlformats.**
-dontwarn org.apache.xmlbeans.**

# iText (PDF generation).
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# Google API client (models are accessed reflectively by the JSON parser).
-keep class com.google.api.client.** { *; }
-keep class com.google.api.** { *; }
-dontwarn com.google.api.client.**

# gRPC (used transitively by Google/Firebase clients).
-keep class io.grpc.** { *; }
-dontwarn io.grpc.**

# Microsoft MSAL / identity (OneDrive auth — disabled but still linked).
-keep class com.microsoft.identity.** { *; }
-dontwarn com.microsoft.identity.**
-dontwarn com.microsoft.graph.**

# Dropbox SDK.
-keep class com.dropbox.** { *; }
-dontwarn com.dropbox.**

# Jakarta Mail (IMAP).
-keep class jakarta.mail.** { *; }
-keep class com.sun.mail.** { *; }
-dontwarn jakarta.mail.**
-dontwarn com.sun.mail.**

# Vosk speech-to-text (JNI bindings).
-keep class org.vosk.** { *; }
-dontwarn org.vosk.**

# TensorFlow Lite / AI Edge runtime.
-keep class org.tensorflow.** { *; }
-keep class com.google.ai.edge.** { *; }
-dontwarn org.tensorflow.**

# Keep annotated model fields used by reflective (de)serialization.
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
