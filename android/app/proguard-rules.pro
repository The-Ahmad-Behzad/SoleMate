# Keep Play Core / Split Install APIs
-keep class com.google.android.play.core.** { *; }
-dontwarn com.google.android.play.core.**

# Keep TensorFlow Lite (GPU + core)
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# ARCore & Filament keep rules
-keep class com.google.ar.core.** { *; }
-dontwarn com.google.ar.core.**

-keep class com.google.android.filament.** { *; }
-dontwarn com.google.android.filament.**

-keep class com.google.firebase.** { *; }
-keep class org.tensorflow.** { *; }
-keep class com.google.ar.** { *; }
-keep class com.solemate.app.solemate_app.** { *; }
