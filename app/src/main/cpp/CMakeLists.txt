cmake_minimum_required(VERSION 3.4.1)

add_library(keys SHARED
            keys.c)

# 16KB 페이지 크기 지원을 위한 링커 플래그 추가
# Android 15+ 디바이스 호환성을 위해 필수
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wl,-z,max-page-size=16384")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wl,-z,max-page-size=16384")

# Include libraries needed for native-lib lib
target_link_libraries(keys
                      android
                      log)
