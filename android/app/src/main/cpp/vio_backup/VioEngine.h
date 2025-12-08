#ifndef VIO_ENGINE_H
#define VIO_ENGINE_H

#include <mutex>
#include <vector>
#include <array>
#include <memory>
#include <atomic>

/**
 * Minimal Visual-Inertial Odometry Engine
 * Provides pose estimation from camera frames and IMU data
 */
class VioEngine {
public:
    struct Config {
        int imageWidth = 640;
        int imageHeight = 480;
        float fx = 500.0f;  // Focal length X
        float fy = 500.0f;  // Focal length Y
        float cx = 320.0f;  // Principal point X
        float cy = 240.0f;  // Principal point Y
    };

    struct ImuData {
        float accel[3];  // Accelerometer [x, y, z] in m/s^2
        float gyro[3];   // Gyroscope [x, y, z] in rad/s
        int64_t timestampNs;  // Timestamp in nanoseconds
    };

    struct Pose {
        float matrix[16];  // 4x4 transformation matrix (column-major)
        float covariance[36];  // 6x6 covariance matrix (x, y, z, roll, pitch, yaw)
        int64_t timestampNs;
        bool isValid;
    };

    VioEngine();
    ~VioEngine();

    // Initialize with configuration
    bool init(const Config& config);

    // Start/stop tracking
    bool start();
    void stop();

    // Feed camera frame (YUV420 format)
    bool feedFrame(const uint8_t* yuvData, int width, int height, int64_t timestampNs);

    // Feed IMU data
    bool feedImu(const ImuData& imuData);

    // Get current pose (thread-safe)
    bool getPose(Pose& pose) const;

    // Get camera intrinsics
    void getIntrinsics(float& fx, float& fy, float& cx, float& cy) const;

    // Get tracking state
    bool isTracking() const { return isTracking_.load(); }

private:
    Config config_;
    std::atomic<bool> isInitialized_;
    std::atomic<bool> isTracking_;
    mutable std::mutex poseMutex_;
    
    // Current pose estimate
    Pose currentPose_;
    
    // IMU data buffer
    std::vector<ImuData> imuBuffer_;
    mutable std::mutex imuMutex_;
    
    // Feature tracking state
    struct Feature {
        float x, y;
        int age;
        bool isValid;
    };
    std::vector<Feature> features_;
    
    // Simple visual odometry state
    float translation_[3];
    float rotation_[4];  // Quaternion [x, y, z, w]
    
    // Helper methods
    void updatePoseFromFeatures(const uint8_t* yuvData, int width, int height);
    void integrateImu(const ImuData& imuData);
    void detectFeatures(const uint8_t* yuvData, int width, int height);
    void trackFeatures(const uint8_t* yuvData, int width, int height);
};

#endif // VIO_ENGINE_H



