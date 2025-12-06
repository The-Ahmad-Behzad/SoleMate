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

# Fix R8 build errors: Keep annotation processing API classes
# These are needed by AutoValue/JavaPoet at compile time
-keep class javax.lang.model.** { *; }
-keep class javax.tools.** { *; }
-dontwarn javax.lang.model.**
-dontwarn javax.tools.**
-dontwarn javax.tools.Diagnostic$Kind
-dontwarn javax.tools.JavaFileObject

# Keep AutoValue and JavaPoet classes (including shaded versions)
-keep class com.google.auto.value.** { *; }
-keep class com.squareup.javapoet.** { *; }
-keep class autovalue.shaded.com.squareup.javapoet.** { *; }
-dontwarn com.google.auto.value.**
-dontwarn com.squareup.javapoet.**
-dontwarn autovalue.shaded.com.squareup.javapoet.**

# Keep annotation processor generated classes
-keep class * extends com.google.auto.value.processor.AutoValueProcessor { *; }
-keepclassmembers class * {
    @com.google.auto.value.AutoValue <methods>;
}