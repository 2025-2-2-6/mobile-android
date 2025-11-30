# ---------------------------
# Retrofit / OkHttp 보호
# ---------------------------
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
# Models 유지
-keep class com.example.mobile_android.model.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
# Annotation 유지 (POST, GET 경로 날아가는 것 방지)
-keepattributes *Annotation*

# Gson SerializedName 유지
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}


-dontwarn com.google.firebase.ktx.**
