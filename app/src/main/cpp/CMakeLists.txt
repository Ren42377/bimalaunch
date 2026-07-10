cmake_minimum_required(VERSION 3.22.1)

project(nativecore LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(nativecore SHARED
    jni_bridge.cpp
    core/Core.cpp
    core/Registry.cpp
    core/Dispatcher.cpp
    core/License.cpp
    core/WebsiteConfig.cpp
    core/ScriptManager.cpp
    core/AntiCheat.cpp
)

target_include_directories(nativecore PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party
)

target_compile_definitions(nativecore PRIVATE JSON_NOEXCEPTION=1)

target_compile_options(nativecore PRIVATE
    -fvisibility=hidden
    -fvisibility-inlines-hidden
    -fno-rtti
    -fno-exceptions
    -ffunction-sections
    -fdata-sections
    -Wall
    -Wextra
)

target_link_options(nativecore PRIVATE
    -Wl,--gc-sections
)

if(CMAKE_BUILD_TYPE STREQUAL "Release" OR CMAKE_BUILD_TYPE STREQUAL "RelWithDebInfo")
    target_compile_options(nativecore PRIVATE -O3 -flto)
    target_link_options(nativecore PRIVATE -O3 -flto -Wl,-s)
endif()

find_library(log-lib log)
target_link_libraries(nativecore ${log-lib})
