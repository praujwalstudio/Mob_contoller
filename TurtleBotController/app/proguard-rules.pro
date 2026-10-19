# Keep data classes for Kotlinx Serialization
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepattributes *Annotation*,InnerClasses,EnclosingMethod

# OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**
