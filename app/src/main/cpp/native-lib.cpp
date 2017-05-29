#include <jni.h>
#include <string>
#include <math.h>
#include <android/log.h>

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "libnav", __VA_ARGS__)

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "libnav", __VA_ARGS__)

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "libnav", __VA_ARGS__)

#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "libnav", __VA_ARGS__)

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "libnav", __VA_ARGS__)



//https://code.tutsplus.com/ko/tutorials/how-to-get-started-with-androids-native-development-kit--cms-27605


extern "C" {
//define function here
#define MAX_SPO_UNIT_SIZE 10
#define DELAY_BUFFER_SIZE 1024

typedef struct {
    int x_size;
    float x[DELAY_BUFFER_SIZE];
    int head;
    double angle;
    double distance;
    double xCor;
    double yCor;

    float *inputSound;
    int inputSoundSize;
    float mixedOutput[400];
} SoundObejct;

SoundObejct SOList[MAX_SPO_UNIT_SIZE];

typedef struct {
    int HRTF_SIZE;
    float leftHRTF[100][200];
    float rightHRTF[100][200];

} HRTF_DATABASE;

HRTF_DATABASE hrtf_database;

//TODO implement here
int getAngleIndex(double angle) {
    if (angle < 0) angle = -angle;

    return (int)((angle/180.0)*100);
}


void updateDistance(int handle){
    double x = SOList[handle].xCor;
    double y = SOList[handle].yCor;
    SOList[handle].distance = sqrt(x * x + y * y);
}

void updateAngle(int handle){
    double x = SOList[handle].xCor;
    double y = SOList[handle].yCor;
    SOList[handle].angle = (atan2(y, x) * 180) / M_PI;
}



#define MAX_DISTANCE 300

#define PUSHABLE_SIZE_PER_CHANNEL 64
#define PUSHABLE_SIZE 128
JNIEXPORT jfloatArray JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundProvider_convProcess(
        JNIEnv *env,
        jobject instance, /* this */
        jint SOHandle_j) {
    SoundObejct &obejct = SOList[SOHandle_j];

//    LOGI("JNI log angle : %lf,  angle index: %d ",obejct.angle, getAngleIndex(obejct.angle));

    for (int i = 0; i < PUSHABLE_SIZE_PER_CHANNEL; i++) {
        // delay
        for (int j = hrtf_database.HRTF_SIZE - 1; j >= 0; j--) {
            obejct.x[j] = obejct.x[j - 1];
        }
        obejct.x[0] = obejct.inputSound[(i + obejct.head) % obejct.inputSoundSize];

        float leftOut = 0;
        float rightOut = 0;
        int angleIdx = getAngleIndex(obejct.angle);
        for (int j = 0; j < hrtf_database.HRTF_SIZE; j++) {
            leftOut += obejct.x[j] * hrtf_database.leftHRTF[angleIdx][j];
            rightOut += obejct.x[j] * hrtf_database.rightHRTF[angleIdx][j];
        }
        obejct.mixedOutput[i * 2] = (float) (leftOut *
                                             ((MAX_DISTANCE - obejct.distance) / MAX_DISTANCE));
        obejct.mixedOutput[i * 2 + 1] = (float) (rightOut *
                                                 ((MAX_DISTANCE - obejct.distance) / MAX_DISTANCE));
    }

    jfloatArray ret = env->NewFloatArray(PUSHABLE_SIZE);
    env->SetFloatArrayRegion(ret, 0, PUSHABLE_SIZE, obejct.mixedOutput);
    obejct.head = (PUSHABLE_SIZE_PER_CHANNEL + obejct.head) % obejct.inputSoundSize;

    return ret;
}


JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_loadHRTF(
        JNIEnv *env,
        jobject/* this */,
        jfloatArray HRTF_database_j,
        jint angleIndex_j,
        jboolean channel) {
    int hrtf_size = env->GetArrayLength(HRTF_database_j);
    hrtf_database.HRTF_SIZE = hrtf_size;

    float *hrtf = env->GetFloatArrayElements(HRTF_database_j, NULL);

    if (channel == 0) {
        for (int i = 0; i < hrtf_size; i++) {
            hrtf_database.leftHRTF[angleIndex_j][i] = hrtf[i];
        }
    } else if (channel == 1) {
        for (int i = 0; i < hrtf_size; i++) {
            hrtf_database.rightHRTF[angleIndex_j][i] = hrtf[i];
        }
    }

    env->ReleaseFloatArrayElements(HRTF_database_j, hrtf, 0);
    return;
}


//TODO 파일 분리 하는방법 알아두기
int getNewSPOHANDLE() {
    return 0;
}

JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_initSoundObject(
        JNIEnv *env,
        jobject/* this */,
        jint x_size_j,
        jdouble x_j,
        jdouble y_j,
        jfloatArray sound_j) {

    SoundObejct &unit = SOList[getNewSPOHANDLE()];

    unit.x_size = x_size_j;
    for (int i = 0; i < x_size_j; i++) {
        unit.x[i] = 0;
    }
    for (int i = 0; i < x_size_j * 2; i++) {
        unit.mixedOutput[i] = 0;
    }

    unit.angle = 0;
    unit.distance = 0;
    unit.xCor = x_j;
    unit.yCor = y_j;
    unit.head = 0;

    unit.inputSoundSize = env->GetArrayLength(sound_j);
    unit.inputSound = (float *) malloc(sizeof(float) * unit.inputSoundSize);
    float *inputSound = env->GetFloatArrayElements(sound_j, 0);
    for (int i = 0; i < unit.inputSoundSize; i++) {
        unit.inputSound[i] = inputSound[i];
    }

}




// get set angle , distance

JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_setSOAngle
        (JNIEnv *env,
         jobject instance,
         jint handle_j,
         jdouble angle_j) {

    // TODO
    SOList[handle_j].angle = angle_j;
}

JNIEXPORT jdouble JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_getSOAngle(
        JNIEnv *env,
        jobject instance,
        jint handle_j) {

    // TODO
    return SOList[handle_j].angle;
}

JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_setSODistance(
        JNIEnv *env,
        jobject instance,
        jint handle_j,
        jdouble distance_j) {

    // TODO
    SOList[handle_j].distance = distance_j;
}

JNIEXPORT jdouble JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_getSODistance(
        JNIEnv *env,
        jobject instance,
        jint handle_j) {

    // TODO
    return (jint) SOList[handle_j].distance;
}


JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_setSOX(
        JNIEnv *env,
        jobject instance,
        jint handle_j,
        jdouble x_j) {

    if(x_j> MAX_DISTANCE)
        x_j = MAX_DISTANCE;

    SoundObejct &object = SOList[handle_j];
    object.xCor = x_j;
    updateAngle(handle_j);
    updateDistance(handle_j);
}

JNIEXPORT jdouble JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_getSOX(
        JNIEnv *env,
        jobject instance,
        jint handle_j) {

    SoundObejct &object = SOList[handle_j];
    return object.xCor;
}

JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_setSOY(
        JNIEnv *env,
        jobject instance,
        jint handle_j,
        jdouble y_j) {

    if(y_j > MAX_DISTANCE)
        y_j = MAX_DISTANCE;

    SoundObejct &object = SOList[handle_j];
    object.yCor = y_j;
    updateAngle(handle_j);
    updateDistance(handle_j);
}

JNIEXPORT jdouble JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_getSOY(
        JNIEnv *env,
        jobject instance,
        jint handle_j) {

    SoundObejct &object = SOList[handle_j];
    return object.yCor;
}


///test funciton
////////////////////////////////////////////////////////////////////////////////////////



}