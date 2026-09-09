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

# GSON
-keep class com.google.gson.** { *; }
-keep class com.ng.pikop.core.network.** { *; }

# Google Maps / Places
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.android.libraries.places.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }

# Compose
-keep class androidx.compose.ui.platform.** { *; }

# Keep data classes that might be used for JSON serialization
-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}
