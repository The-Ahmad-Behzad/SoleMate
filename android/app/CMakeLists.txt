# Specify the minimum version of CMake required
cmake_minimum_required(VERSION 3.22.1)

# Define your native library name (this will generate libnative-lib.so)
project(native-lib)

# Add source file(s) — you can start with a stub native-lib.cpp
add_library(
        native-lib
        SHARED
        ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/native-lib.cpp
)

# Specify libraries from the NDK to link against (log is common)
find_library(
        log-lib
        log
)

# Link the log library
target_link_libraries(
        native-lib
        ${log-lib}
)
