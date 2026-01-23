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

# Fix R8 missing class errors for AutoValue/JavaPoet used by Firebase
-dontwarn javax.lang.model.**
-dontwarn autovalue.shaded.com.squareup.javapoet.**
-dontwarn com.google.auto.value.**

# Additional rules from missing_rules.txt
-dontwarn javax.tools.JavaFileObject$Kind
-dontwarn javax.tools.JavaFileObject
-dontwarn javax.tools.SimpleJavaFileObject

# Keep classes that reference javax.lang.model (annotation processing)
-keep class javax.lang.model.** { *; }
-keep class autovalue.shaded.** { *; }
