//#include <jni.h>
//#include <string>
//#include <android/log.h>
//
//extern "C"
//JNIEXPORT jstring JNICALL
//Java_com_solemate_app_solemate_1app_MainActivity_stringFromJNI(JNIEnv* env, jobject /* this */) {
//    std::string hello = "Hello from native C++";
//    __android_log_print(ANDROID_LOG_INFO, "NativeLib", "Native lib loaded successfully");
//    return env->NewStringUTF(hello.c_str());
//}

#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "SoleMateNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jstring JNICALL
Java_com_solemate_app_solemate_1app_MainActivity_startARSessionNative(
        JNIEnv* env,
        jobject /* this */) {
    LOGI("AR session started (from native-lib.cpp)");
    std::string message = "AR Session Initialized (C++ side)";
    return env->NewStringUTF(message.c_str());
}
