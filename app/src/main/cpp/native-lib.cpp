#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_hereliesaz_lexorcist_MainActivity_getApiKey(
        JNIEnv* env,
        jobject /* this */) {
    std::string api_key = API_KEY;
    return env->NewStringUTF(api_key.c_str());
}
