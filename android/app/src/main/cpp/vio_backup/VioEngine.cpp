#include "VioEngine.h"
#include "vio_config.h"
#include <cmath>
#include <algorithm>
#include <cstring>

VioEngine::VioEngine()
    : isInitialized_(false)
    , isTracking_(false)
    , translation_{0.0f, 0.0f, 0.0f}
    , rotation_{0.0f, 0.0f, 0.0f, 1.0f}
{
    // Initialize pose matrix to identity
    std::memset(currentPose_.matrix, 0, sizeof(currentPose_.matrix));
    currentPose_.matrix[0] = 1.0f;
    currentPose_.matrix[5] = 1.0f;
    currentPose_.matrix[10] = 1.0f;
    currentPose_.matrix[15] = 1.0f;
    
    // Initialize covariance to large values (uncertainty)
    std::fill(currentPose_.covariance, currentPose_.covariance + 36, 1.0f);
    currentPose_.isValid = false;
    currentPose_.timestampNs = 0;
}

VioEngine::~VioEngine() {
    stop();
}

bool VioEngine::init(const Config& config) {
    if (isInitialized_.load()) {
        return false;  // Already initialized
    }
    
    config_ = config;
    features_.clear();
    features_.reserve(VIO_MAX_FEATURES);
    imuBuffer_.clear();
    imuBuffer_.reserve(VIO_IMU_QUEUE_SIZE);
    
    isInitialized_.store(true);
    return true;
}

bool VioEngine::start() {
    if (!isInitialized_.load()) {
        return false;
    }
    
    isTracking_.store(true);
    
    // Reset pose
    std::lock_guard<std::mutex> lock(poseMutex_);
    std::memset(translation_, 0, sizeof(translation_));
    rotation_[0] = 0.0f;
    rotation_[1] = 0.0f;
    rotation_[2] = 0.0f;
    rotation_[3] = 1.0f;
    
    currentPose_.isValid = false;
    currentPose_.timestampNs = 0;
    
    return true;
}

void VioEngine::stop() {
    isTracking_.store(false);
    
    std::lock_guard<std::mutex> lock(poseMutex_);
    currentPose_.isValid = false;
}

bool VioEngine::feedFrame(const uint8_t* yuvData, int width, int height, int64_t timestampNs) {
    if (!isTracking_.load() || yuvData == nullptr) {
        return false;
    }
    
    // Update features and estimate motion
    detectFeatures(yuvData, width, height);
    trackFeatures(yuvData, width, height);
    updatePoseFromFeatures(yuvData, width, height);
    
    // Update pose matrix
    {
        std::lock_guard<std::mutex> lock(poseMutex_);
        
        // Build transformation matrix from translation and rotation
        float qx = rotation_[0], qy = rotation_[1], qz = rotation_[2], qw = rotation_[3];
        
        // Rotation matrix from quaternion
        currentPose_.matrix[0] = 1.0f - 2.0f * (qy * qy + qz * qz);
        currentPose_.matrix[1] = 2.0f * (qx * qy - qz * qw);
        currentPose_.matrix[2] = 2.0f * (qx * qz + qy * qw);
        currentPose_.matrix[3] = 0.0f;
        
        currentPose_.matrix[4] = 2.0f * (qx * qy + qz * qw);
        currentPose_.matrix[5] = 1.0f - 2.0f * (qx * qx + qz * qz);
        currentPose_.matrix[6] = 2.0f * (qy * qz - qx * qw);
        currentPose_.matrix[7] = 0.0f;
        
        currentPose_.matrix[8] = 2.0f * (qx * qz - qy * qw);
        currentPose_.matrix[9] = 2.0f * (qy * qz + qx * qw);
        currentPose_.matrix[10] = 1.0f - 2.0f * (qx * qx + qy * qy);
        currentPose_.matrix[11] = 0.0f;
        
        // Translation
        currentPose_.matrix[12] = translation_[0];
        currentPose_.matrix[13] = translation_[1];
        currentPose_.matrix[14] = translation_[2];
        currentPose_.matrix[15] = 1.0f;
        
        currentPose_.timestampNs = timestampNs;
        currentPose_.isValid = true;
        
        // Update covariance (simplified - would be more sophisticated in real implementation)
        // Increase uncertainty over time
        for (int i = 0; i < 36; i++) {
            currentPose_.covariance[i] *= 1.001f;  // Slight increase
            if (currentPose_.covariance[i] > 10.0f) {
                currentPose_.covariance[i] = 10.0f;  // Cap at reasonable value
            }
        }
    }
    
    return true;
}

bool VioEngine::feedImu(const ImuData& imuData) {
    if (!isTracking_.load()) {
        return false;
    }
    
    {
        std::lock_guard<std::mutex> lock(imuMutex_);
        imuBuffer_.push_back(imuData);
        
        // Keep buffer size manageable
        if (imuBuffer_.size() > VIO_IMU_QUEUE_SIZE) {
            imuBuffer_.erase(imuBuffer_.begin());
        }
    }
    
    integrateImu(imuData);
    return true;
}

bool VioEngine::getPose(Pose& pose) const {
    if (!isTracking_.load() || !currentPose_.isValid) {
        return false;
    }
    
    std::lock_guard<std::mutex> lock(poseMutex_);
    pose = currentPose_;
    return true;
}

void VioEngine::getIntrinsics(float& fx, float& fy, float& cx, float& cy) const {
    fx = config_.fx;
    fy = config_.fy;
    cx = config_.cx;
    cy = config_.cy;
}

void VioEngine::updatePoseFromFeatures(const uint8_t* yuvData, int width, int height) {
    // Simplified visual odometry: estimate motion from feature displacement
    // In a real implementation, this would use more sophisticated algorithms
    
    if (features_.size() < 10) {
        return;  // Not enough features
    }
    
    // Simple translation estimate based on feature movement
    // This is a placeholder - real implementation would use more sophisticated tracking
    float avgDx = 0.0f, avgDy = 0.0f;
    int validCount = 0;
    
    for (const auto& feat : features_) {
        if (feat.isValid && feat.age > 0) {
            // Simplified: assume features move proportionally to camera motion
            // Real implementation would track feature correspondences
            validCount++;
        }
    }
    
    if (validCount > 0) {
        // Update translation (very simplified)
        // Real implementation would estimate 3D motion
        translation_[0] += avgDx * 0.001f;  // Scale factor
        translation_[1] += avgDy * 0.001f;
        // translation_[2] would be estimated from scale/depth
    }
}

void VioEngine::integrateImu(const ImuData& imuData) {
    // Simplified IMU integration
    // Real implementation would use proper sensor fusion (e.g., complementary filter, EKF)
    
    // Use accelerometer for gravity alignment
    float ax = imuData.accel[0];
    float ay = imuData.accel[1];
    float az = imuData.accel[2];
    
    float norm = std::sqrt(ax * ax + ay * ay + az * az);
    if (norm > 0.1f) {  // Avoid division by zero
        // Estimate gravity direction (simplified)
        // Real implementation would filter this properly
    }
    
    // Use gyroscope for rotation update
    float gx = imuData.gyro[0];
    float gy = imuData.gyro[1];
    float gz = imuData.gyro[2];
    
    // Simple quaternion integration (simplified - real implementation would be more accurate)
    float dt = 0.033f;  // Assume ~30 FPS
    float halfDt = dt * 0.5f;
    
    float qx = rotation_[0], qy = rotation_[1], qz = rotation_[2], qw = rotation_[3];
    
    // Quaternion derivative
    float dqx = halfDt * (qw * gx + qy * gz - qz * gy);
    float dqy = halfDt * (qw * gy - qx * gz + qz * gx);
    float dqz = halfDt * (qw * gz + qx * gy - qy * gx);
    float dqw = halfDt * (-qx * gx - qy * gy - qz * gz);
    
    // Update quaternion
    rotation_[0] = qx + dqx;
    rotation_[1] = qy + dqy;
    rotation_[2] = qz + dqz;
    rotation_[3] = qw + dqw;
    
    // Normalize quaternion
    float qnorm = std::sqrt(rotation_[0] * rotation_[0] + rotation_[1] * rotation_[1] +
                           rotation_[2] * rotation_[2] + rotation_[3] * rotation_[3]);
    if (qnorm > 0.0001f) {
        rotation_[0] /= qnorm;
        rotation_[1] /= qnorm;
        rotation_[2] /= qnorm;
        rotation_[3] /= qnorm;
    }
}

void VioEngine::detectFeatures(const uint8_t* yuvData, int width, int height) {
    // Simplified feature detection (corner detection on Y channel)
    // Real implementation would use FAST, ORB, or similar
    
    if (features_.size() >= VIO_MAX_FEATURES) {
        return;  // Already have enough features
    }
    
    // Simple grid-based feature detection
    int gridSize = 20;
    int featuresToAdd = VIO_MAX_FEATURES - features_.size();
    
    for (int y = gridSize; y < height - gridSize && featuresToAdd > 0; y += gridSize * 2) {
        for (int x = gridSize; x < width - gridSize && featuresToAdd > 0; x += gridSize * 2) {
            // Check if this location is far enough from existing features
            bool tooClose = false;
            for (const auto& feat : features_) {
                if (feat.isValid) {
                    float dx = feat.x - x;
                    float dy = feat.y - y;
                    if (dx * dx + dy * dy < VIO_MIN_FEATURE_DISTANCE * VIO_MIN_FEATURE_DISTANCE) {
                        tooClose = true;
                        break;
                    }
                }
            }
            
            if (!tooClose) {
                Feature feat;
                feat.x = static_cast<float>(x);
                feat.y = static_cast<float>(y);
                feat.age = 0;
                feat.isValid = true;
                features_.push_back(feat);
                featuresToAdd--;
            }
        }
    }
}

void VioEngine::trackFeatures(const uint8_t* yuvData, int width, int height) {
    // Simplified feature tracking
    // Real implementation would use optical flow (Lucas-Kanade, etc.)
    
    // Age features and remove old ones
    for (auto& feat : features_) {
        if (feat.isValid) {
            feat.age++;
            if (feat.age > VIO_MAX_FEATURE_AGE) {
                feat.isValid = false;
            }
        }
    }
    
    // Remove invalid features
    features_.erase(
        std::remove_if(features_.begin(), features_.end(),
                      [](const Feature& f) { return !f.isValid; }),
        features_.end()
    );
}



