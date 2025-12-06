#ifndef VIO_CONFIG_H
#define VIO_CONFIG_H

// VIO Configuration Constants

// Maximum number of tracked features
#define VIO_MAX_FEATURES 200

// Image dimensions (will be set at runtime)
#define VIO_DEFAULT_WIDTH 640
#define VIO_DEFAULT_HEIGHT 480

// IMU data queue size
#define VIO_IMU_QUEUE_SIZE 100

// Feature detection parameters
#define VIO_MIN_FEATURE_DISTANCE 30
#define VIO_MAX_FEATURE_AGE 30

// RANSAC parameters for plane estimation
#define VIO_RANSAC_THRESHOLD 0.05f
#define VIO_RANSAC_ITERATIONS 1000

#endif // VIO_CONFIG_H






