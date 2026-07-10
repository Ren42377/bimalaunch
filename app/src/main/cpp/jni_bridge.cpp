#include <jni.h>
#include <string>

#include "core/Core.h"

namespace {

std::string toStdString(JNIEnv* env, jstring value) {
    if (value == nullptr) {
        return std::string();
    }
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) {
        return std::string();
    }
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

jstring toJavaString(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}

}

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_ren42377_bimalaunch_core_NativeBridge_nativeInitialize(
    JNIEnv* env, jobject, jstring configJson) {
    const std::string config = toStdString(env, configJson);
    const std::string status = bimalaunch::Core::instance().initialize(config);
    return toJavaString(env, status);
}

JNIEXPORT jstring JNICALL
Java_com_ren42377_bimalaunch_core_NativeBridge_nativeGetManifest(
    JNIEnv* env, jobject) {
    const std::string manifest = bimalaunch::Core::instance().manifest();
    return toJavaString(env, manifest);
}

JNIEXPORT jstring JNICALL
Java_com_ren42377_bimalaunch_core_NativeBridge_nativeDispatch(
    JNIEnv* env, jobject, jstring action, jstring payload) {
    const std::string actionStr = toStdString(env, action);
    const std::string payloadStr = toStdString(env, payload);
    const std::string result = bimalaunch::Core::instance().dispatch(actionStr, payloadStr);
    return toJavaString(env, result);
}

JNIEXPORT void JNICALL
Java_com_ren42377_bimalaunch_core_NativeBridge_nativeShutdown(
    JNIEnv*, jobject) {
    bimalaunch::Core::instance().shutdown();
}

}
