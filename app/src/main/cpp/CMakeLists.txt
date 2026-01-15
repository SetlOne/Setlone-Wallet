cmake_minimum_required(VERSION 3.4.1)

add_library(keys SHARED
            keys.c)

# 16KB 페이지 크기 지원을 위한 링커 플래그 추가
# Android 15+ 디바이스 호환성을 위해 필수
# NDK r28+ 에서는 자동으로 16KB 정렬되지만, 명시적으로 설정하여 보장
if(ANDROID_SUPPORT_16KB_PAGE_SIZE)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wl,-z,max-page-size=16384")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wl,-z,max-page-size=16384")
else()
    # 기본적으로도 16KB 정렬 적용 (NDK r28+)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wl,-z,max-page-size=16384")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wl,-z,max-page-size=16384")
endif()

# Include libraries needed for native-lib lib
target_link_libraries(keys
                      android
                      log)
