#include <jni.h>
#include <string>
#include "NativeBridge.h"

using namespace std;

extern "C" {

// Test Connection
JNIEXPORT jstring JNICALL Java_NativeBridge_testConnection
  (JNIEnv* env, jobject obj) {
    return env->NewStringUTF("JNI Connected Successfully!");
}

// Placeholder for shortest path
JNIEXPORT jstring JNICALL Java_NativeBridge_getShortestPath
  (JNIEnv* env, jobject obj, jstring src, jstring dest) {

    const char* a = env->GetStringUTFChars(src, NULL);
    const char* b = env->GetStringUTFChars(dest, NULL);

    string result = string("Shortest path from ") + a + " to " + b;

    env->ReleaseStringUTFChars(src, a);
    env->ReleaseStringUTFChars(dest, b);

    return env->NewStringUTF(result.c_str());
}

}
