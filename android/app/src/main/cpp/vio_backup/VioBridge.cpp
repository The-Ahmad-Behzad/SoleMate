#include <jni.h>
#include <android/log.h>
#include "VioEngine.h"
#include <memory>
#include <mutex>

#define LOG_TAG "VioBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global VIO engine instance (thread-safe access)
static std::unique_ptr<VioEngine> g_vioEngine = nullptr;
static std::mutex g_vioMutex;

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_solemate_app_solemate_1app_VioEngine_nativeInit(
    JNIEnv *env, jobject thiz,
    jint imageWidth, jint imageHeight,
    jfloat fx, jfloat fy, jfloat cx, jfloat cy) {
    
    LOGI("nativeInit: %dx%d, fx=%.2f fy=%.2f cx=%.2f cy=%.2f", 
         imageWidth, imageHeight, fx, fy, cx, cy);
    
    std::lock_guard<std::mutex> lock(g_vioMutex);
    
    if (g_vioEngine != nullptr) {
        LOGE("VIO engine already initialized");
        return 0;
    }
    
    g_vioEngine = std::make_unique<VioEngine>();
    
    VioEngine::Config config;
    config.imageWidth = imageWidth;
    config.imageHeight = imageHeight;
    config.fx = fx;
    config.fy = fy;
    config.cx = cx;
    config.cy = cy;
    
    if (!g_vioEngine->init(config)) {
        LOGE("Failed to initialize VIO engine");
        g_vioEngine.reset();
        return 0;
    }
    
    LOGI("VIO engine initialized successfully");
    return reinterpret_cast<jlong>(g_vioEngine.get());
}

JNIEXPORT jboolean JNICALL
Java_com_solemate_app_solemate_1app_VioEngine_nativeStart(JNIEnv *env, jobject thiz) {
    LOGI("nativeStart");
    
    std::lock_guard<std::mutex> lock(g_vioMutex);
    
    if (g_vioEngine == nullptr) {
        LOGE("VIO engine not initialized");
        return JNI_FALSE;
    }
    
    if (!g_vioEngine->start()) {
        LOGE("Failed to start VIO engine");
        return JNI_FALSE;
    }
    
    LOGI("VIO engine started");
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_solemate_app_solemate_1app_VioEngine_nativeStop(JNIEnv *env, jobject thiz) {
    LOGI("nativeStop");
    
    std::lock_guard<std::mutex> lock(g_vioMutex);
    
    if (g_vioEngine != nullptr) {
        g_vioEngine->stop();
    }
}

JNIEXPORT void JNICALL
Java_com_solemate_app_solemate_1app_VioEngine_nativeRelease(JNIEnv *env, jobject thiz) {
    LOGI("nativeRelease");
    
    std::lock_guard<std::mutex> lock(g_vioMutex);
    
    if (g_vioEngine != nullptr) {
        g_vioEngine->stop();
        g_vioEngine.reset();
    }
}

JNIEXPORT jboolean JNICALL
Java_com_solemate_app_solemate_1app_VioEngine_nativeFeedFrame(
    JNIEnv *env, jobject thiz,
    jbyteArray yuvData, jint width, jint height, jlong timestampNs) {
    
    std::lock_guard<std::mutex> lock(g_vioMutex);
    
    if (g_vioEngine == nullptr || !g_vioEngine->isTracking()) {
        return JNI_FALSE;
    }
    
    jbyte* data = env->GetByteArrayElements(yuvData, nullptr);
    if (data == nullptr) {
        LOGE("Failed to get YUV data");
        return JNI_FALSE;
    }
    
    bool result = g_vioEngine->feedFrame(
        reinterpret_cast<const uint8_t*>(data),
        width, height, timestampNs);
    
    env->ReleaseByteArrayElements(yuvData, data, JNI_ABORT);
    
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_solemate_app_solemate_1app_VioEngine_nativeFeedImu(
    JNIEnv *env, jobject thiz,
    jfloat accelX, jfloat accelY, jfloat accelZ,
    jfloat gyroX, jfloat gyroY, jfloat gyroZ,
    jlong timestampNs) {
    
    std::lock_guard<std::mutex> lock(g_vioMutex);
    
    if (g_vioEngine == nullptr || !g_vioEngine->isTracking()) {
        return JNI_FALSE;
    }
    
    VioEngine::ImuData imuData;
    imuData.accel[0] = accelX;
    imuData.accel[1] = accelY;
    imuData.accel[2] = accelZ;
    imuData.gyro[0] = gyroX;
    imuData.gyro[1] = gyroY;
    imuData.gyro[2] = gyroZ;
    imuData.timestampNs = timestampNs;
    
    bool result = g_vioEngine->feedImu(imuData);
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_solemate_app_solemate_1app_VioEngine_nativeGetPose(
    JNIEnv *env, jobject thiz,
    jfloatArray poseMatrix, jfloatArray covariance) {
    
    std::lock_guard<std::mutex> lock(g_vioMutex);
    
    if (g_vioEngine == nullptr) {
        return JNI_FALSE;
    }
    
    VioEngine::Pose pose;
    if (!g_vioEngine->getPose(pose)) {
        return JNI_FALSE;
    }
    
    // Copy pose matrix (16 floats)
    if (poseMatrix != nullptr) {
        jfloat* matrixData = env->GetFloatArrayElements(poseMatrix, nullptr);
        if (matrixData != nullptr) {
            std::memcpy(matrixData, pose.matrix, 16 * sizeof(float));
            env->ReleaseFloatArrayElements(poseMatrix, matrixData, 0);
        }
    }
    
    // Copy covariance matrix (36 floats)
    if (covariance != nullptr) {
        jfloat* covData = env->GetFloatArrayElements(covariance, nullptr);
        if (covData != nullptr) {
            std::memcpy(covData, pose.covariance, 36 * sizeof(float));
            env->ReleaseFloatArrayElements(covariance, covData, 0);
        }
    }
    
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_solemate_app_solemate_1app_VioEngine_nativeGetIntrinsics(
    JNIEnv *env, jobject thiz,
    jfloatArray intrinsics) {
    
    std::lock_guard<std::mutex> lock(g_vioMutex);
    
    if (g_vioEngine == nullptr || intrinsics == nullptr) {
        return;
    }
    
    float fx, fy, cx, cy;
    g_vioEngine->getIntrinsics(fx, fy, cx, cy);
    
    jfloat* data = env->GetFloatArrayElements(intrinsics, nullptr);
    if (data != nullptr) {
        data[0] = fx;
        data[1] = fy;
        data[2] = cx;
        data[3] = cy;
        env->ReleaseFloatArrayElements(intrinsics, data, 0);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_solemate_app_solemate_1app_VioEngine_nativeIsTracking(JNIEnv *env, jobject thiz) {
    std::lock_guard<std::mutex> lock(g_vioMutex);
    
    if (g_vioEngine == nullptr) {
        return JNI_FALSE;
    }
    
    return g_vioEngine->isTracking() ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"



