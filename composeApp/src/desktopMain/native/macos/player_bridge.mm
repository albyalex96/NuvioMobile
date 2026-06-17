#include <jni.h>
#include <AppKit/AppKit.h>
#include <WebKit/WebKit.h>
#include <Metal/Metal.h>
#include <QuartzCore/QuartzCore.h>

extern "C" {

static JavaVM *jvm = nullptr;
static jclass bridgeClass = nullptr;
static jobject bridgeObject = nullptr;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    jvm = vm;
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_8) != JNI_OK) {
        return JNI_ERR;
    }
    return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL Java_com_nuvio_app_features_player_desktop_NativePlayerBridge_nativeInit
  (JNIEnv *env, jobject thiz) {
    bridgeObject = env->NewGlobalRef(thiz);
    jclass cls = env->GetObjectClass(thiz);
    bridgeClass = reinterpret_cast<jclass>(env->NewGlobalRef(cls));
}

JNIEXPORT void JNICALL Java_com_nuvio_app_features_player_desktop_NativePlayerBridge_nativePlay
  (JNIEnv *, jobject, jstring url) {
    const char *urlStr = env->GetStringUTFChars(url, nullptr);
    NSLog(@"Playing URL: %s", urlStr);
    env->ReleaseStringUTFChars(url, urlStr);
}

JNIEXPORT void JNICALL Java_com_nuvio_app_features_player_desktop_NativePlayerBridge_nativePause
  (JNIEnv *, jobject) {
}

JNIEXPORT void JNICALL Java_com_nuvio_app_features_player_desktop_NativePlayerBridge_nativeResume
  (JNIEnv *, jobject) {
}

JNIEXPORT void JNICALL Java_com_nuvio_app_features_player_desktop_NativePlayerBridge_nativeSeek
  (JNIEnv *, jobject, jdouble position) {
}

JNIEXPORT void JNICALL Java_com_nuvio_app_features_player_desktop_NativePlayerBridge_nativeStop
  (JNIEnv *, jobject) {
}

JNIEXPORT void JNICALL Java_com_nuvio_app_features_player_desktop_NativePlayerBridge_nativeSetVolume
  (JNIEnv *, jobject, jdouble volume) {
}

JNIEXPORT jdouble JNICALL Java_com_nuvio_app_features_player_desktop_NativePlayerBridge_nativeGetPosition
  (JNIEnv *, jobject) {
    return 0.0;
}

JNIEXPORT jdouble JNICALL Java_com_nuvio_app_features_player_desktop_NativePlayerBridge_nativeGetDuration
  (JNIEnv *, jobject) {
    return 0.0;
}

JNIEXPORT jboolean JNICALL Java_com_nuvio_app_features_player_desktop_NativePlayerBridge_nativeIsPlaying
  (JNIEnv *, jobject) {
    return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_nuvio_app_features_player_desktop_NativePlayerBridge_nativeDestroy
  (JNIEnv *, jobject) {
    if (bridgeObject) {
        env->DeleteGlobalRef(bridgeObject);
        bridgeObject = nullptr;
    }
    if (bridgeClass) {
        env->DeleteGlobalRef(bridgeClass);
        bridgeClass = nullptr;
    }
}

} // extern "C"
