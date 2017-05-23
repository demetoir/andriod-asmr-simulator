#include <jni.h>
#include <string>

//https://code.tutsplus.com/ko/tutorials/how-to-get-started-with-androids-native-development-kit--cms-27605


extern "C"{
//define function here



}

JNIEXPORT jstring JNICALL
Java_me_demetoir_a3dsound_1ndk_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
