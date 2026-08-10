# Add project specific ProGuard rules here.

# Preserve Retrofit and Moshi annotations and models
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Preserve Moshi JSON data classes & generated adapters
-keep class com.example.network.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# Preserve Room entities and DAOs
-keep class com.example.data.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public *;
}

# Preserve Firebase classes
-keep class com.google.firebase.** { *; }

# Preserve line numbers for release stack trace mapping without exposing raw paths
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

