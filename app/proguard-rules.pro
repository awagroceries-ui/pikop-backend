# Pikop Production ProGuard Rules

# Hilt / Dagger
-keep class com.ng.pikop.** { *; }
-keep class com.google.dagger.** { *; }
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.processor.**

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# GSON & Network DTOs
-keep class com.google.gson.** { *; }
-keep class com.ng.pikop.core.network.** { *; }

# Google Maps / Places
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.android.libraries.places.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }

# Socket.io
-keep class io.socket.** { *; }
-keep class okhttp3.internal.ws.** { *; }

# Gemini AI SDK
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# Compose
-keep class androidx.compose.ui.platform.** { *; }
-keep @androidx.compose.runtime.Composable class *
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Keep data classes that might be used for JSON serialization
-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Prevent obfuscation of R classes for resource resolution
-keep class **.R$* {
    <fields>;
}
