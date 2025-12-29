# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses, Signature, SourceFile, LineNumberTable
-dontnote kotlinx.serialization.AnnotationsKt

# Keep all classes with @Serializable annotation
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data classes
-keep class io.github.kpedal.data.** { *; }
-keep class io.github.kpedal.engine.** { *; }
-keep class io.github.kpedal.api.** { *; }

# Keep Karoo extension classes
-keep class io.github.kpedal.KPedalExtension { *; }
-keep class io.github.kpedal.datatypes.** { *; }
-keep class io.hammerhead.karooext.** { *; }

# Keep coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# Google Tink / security-crypto
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**
-keep class com.google.crypto.tink.** { *; }
