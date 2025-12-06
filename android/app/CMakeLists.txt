# Specify the minimum version of CMake required
cmake_minimum_required(VERSION 3.22.1)

# Define your native library name (this will generate libnative-lib.so)
project(native-lib)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add source file(s) — you can start with a stub native-lib.cpp
add_library(
        native-lib
        SHARED
        ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/native-lib.cpp
)

# VIO Library - Shared library for JNI (will be loaded as libvio-engine.so)
add_library(
        vio-engine
        SHARED
        ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/vio/VioEngine.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/vio/VioBridge.cpp
)

# Include directories
target_include_directories(
        native-lib
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp
)

target_include_directories(
        vio-engine
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/vio
)

# Specify libraries from the NDK to link against (log is common)
find_library(
        log-lib
        log
)

# Link the log library to both libraries
target_link_libraries(
        native-lib
        ${log-lib}
)

target_link_libraries(
        vio-engine
        ${log-lib}
)

# Link VIO engine to native-lib (if needed)
# target_link_libraries(native-lib vio-engine)
