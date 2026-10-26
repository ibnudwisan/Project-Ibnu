#include <jni.h>
#include <cstdint>
#include <mutex>
#include <string>
#include <new>
#include <android/log.h>

#define LOG_TAG "Sims2NativeBridge"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
std::mutex g_mutex;
bool g_initialized = false;

// This is a bridge state only, NOT a PS2 emulator or ISO interpreter.
struct RuntimeContext {
    uint32_t abi_version = 1;
    bool initialized = false;
};
RuntimeContext g_context;

bool ensure_initialized() noexcept {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_initialized) {
        g_context.initialized = true;
        g_initialized = true;
    }
    return g_initialized;
}
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_ibnu_sims2launcher_NativeBridge_nativeInitialize(JNIEnv*, jobject) {
    try {
        return ensure_initialized() ? JNI_TRUE : JNI_FALSE;
    } catch (...) {
        LOGE("nativeInitialize failed");
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ibnu_sims2launcher_NativeBridge_nativeGetStatus(JNIEnv* env, jobject) {
    const char* status = ensure_initialized()
        ? "Native bridge ready; emulator core is not integrated."
        : "Native bridge initialization failed.";
    return env->NewStringUTF(status);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_ibnu_sims2launcher_NativeBridge_nativeStartGame(JNIEnv*, jobject, jstring path) {
    if (!path || !ensure_initialized()) return JNI_FALSE;
    // Intentionally fail closed: a real emulator core must be integrated here.
    // Never cast ISO bytes to executable pointers or jump to guest addresses.
    return JNI_FALSE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_ibnu_sims2launcher_NativeBridge_nativeReadGuestWordLE(
    JNIEnv* env, jobject, jbyteArray bytes, jint offset) {
    if (!bytes || offset < 0) return 0;
    const jsize len = env->GetArrayLength(bytes);
    if (offset > len - 4) return 0;
    jbyte raw[4] = {};
    env->GetByteArrayRegion(bytes, offset, 4, raw);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return 0;
    }
    const uint32_t value =
        static_cast<uint32_t>(static_cast<uint8_t>(raw[0])) |
        (static_cast<uint32_t>(static_cast<uint8_t>(raw[1])) << 8) |
        (static_cast<uint32_t>(static_cast<uint8_t>(raw[2])) << 16) |
        (static_cast<uint32_t>(static_cast<uint8_t>(raw[3])) << 24);
    return static_cast<jint>(value);
}
