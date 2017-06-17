#include <jni.h>
#include <string>
#include <math.h>
#include <android/log.h>
#include <valarray>

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "libnav", __VA_ARGS__)

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "libnav", __VA_ARGS__)

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "libnav", __VA_ARGS__)

#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "libnav", __VA_ARGS__)

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "libnav", __VA_ARGS__)



//https://code.tutsplus.com/ko/tutorials/how-to-get-started-with-androids-native-development-kit--cms-27605





extern "C" {

//const double PI = 3.141592653589793238460;
//typedef std::complex<double> Complex;
//typedef std::valarray<Complex> CArray;
//using namespace std;
//
//// Cooley–Tukey FFT (in-place, divide-and-conquer)
//// Higher memory requirements and redundancy although more intuitive
//void fft(CArray& x)
//{
//    const size_t N = x.size();
//    if (N <= 1) return;
//
//    // divide
//    CArray even = x[std::slice(0, N/2, 2)];
//    CArray  odd = x[std::slice(1, N/2, 2)];
//
//    // conquer
//    fft(even);
//    fft(odd);
//
//    // combine
//    for (size_t k = 0; k < N/2; ++k)
//    {
//        Complex t = std::polar(1.0, -2 * PI * k / N) * odd[k];
//        x[k    ] = even[k] + t;
//        x[k+N/2] = even[k] - t;
//    }
//}
//
//// inverse fft (in-place)
//void ifft(CArray &x) {
//    // conjugate the complex numbers
//    x = x.apply(std::conj);
//
//    // forward fft
//    fft(x);
//
//    // conjugate the complex numbers again
//    x = x.apply(std::conj);
//
//    // scale the numbers
//    x /= x.size();
//}
//


//define function here
#define SO_ARRAY_SIZE 10
#define DELAY_BUFFER_SIZE 1024

#define MODE_NONE 0
#define MODE_CIRCLE 1
#define MODE_LINE 2
#define MODE_RANDOM 3
#define DEFAULT_MODE MODE_CIRCLE

typedef struct {
    float x;
    float y;
} Point2D;

typedef struct {
    int x_size;
    float x[DELAY_BUFFER_SIZE];
    int idxHead;

    float angle;
    float distance;

    float *inputSound;
    int inputSoundSize;

    float mixedOutput[1024];
    int N;


    int Orbitmode;
    float radius;
    Point2D objectP;
    float centerAngle;
    Point2D centerP;
    Point2D lineStartP;
    Point2D lineEndP;

} SoundObject;

SoundObject SOList[SO_ARRAY_SIZE];

#define HRTF_ANGLE_INDEX_SIZE 100
#define HRTF_SIGNAL_SIZE 200
typedef struct {
    int HRTF_SIZE;
    float leftHRTF[HRTF_ANGLE_INDEX_SIZE][HRTF_SIGNAL_SIZE];
    float rightHRTF[HRTF_ANGLE_INDEX_SIZE][HRTF_SIGNAL_SIZE];
} HRTF_DATABASE;
HRTF_DATABASE hrtf_database;

int angleToAngleIndex(double angle) {
    if (angle < 0) angle = -angle;

    return (int) ((angle / 180.0) * HRTF_ANGLE_INDEX_SIZE);
}

#define MAX_DISTANCE 500
void updateDistance(int handle) {
    float x = SOList[handle].objectP.x;
    float y = SOList[handle].objectP.y;
    SOList[handle].distance = (float) sqrt(x * x + y * y);
    if (SOList[handle].distance > MAX_DISTANCE) {
        SOList[handle].distance = MAX_DISTANCE;
    }
}

void updateAngle(int handle) {
    float x = SOList[handle].objectP.x;
    float y = SOList[handle].objectP.y;
    SOList[handle].angle = (float) ((atan2(y, x) * 180) / M_PI);
}

void updateCenterAngle(int handle) {
    float x = SOList[handle].objectP.x - SOList[handle].centerP.x;
    float y = SOList[handle].objectP.y - SOList[handle].centerP.y;
    SOList[handle].centerAngle = (float) ((atan2(y, x) * 180) / M_PI);
}

void updateRadius(int handle) {
    float x = SOList[handle].objectP.x;
    float y = SOList[handle].objectP.y;
    float cx = SOList[handle].centerP.x;
    float cy = SOList[handle].centerP.y;
    float dx = x - cx;
    float dy = y - cy;
    SOList[handle].radius = (float) sqrt(dx * dx + dy * dy);
}

#define PUSHABLE_SIZE_PER_CHANNEL 32
#define PUSHABLE_SIZE PUSHABLE_SIZE_PER_CHANNEL*2

JNIEXPORT jfloatArray JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundProvider_signalProcess(
        JNIEnv *env,
        jobject, /* this */
        jint SOHandle_j) {
    SoundObject &object = SOList[SOHandle_j];

//    LOGI("JNI log angle : %lf,  angle index: %d ",object.angle, angleToAngleIndex(object.angle));

    float distance_weight = (MAX_DISTANCE - object.distance) / MAX_DISTANCE;
    int angleIdx = angleToAngleIndex(object.angle);
    float leftOut = 0;
    float rightOut = 0;
    int index = object.idxHead;

    for (int i = 0; i < PUSHABLE_SIZE_PER_CHANNEL; i++) {
        leftOut = 0;
        rightOut = 0;

        // delay
        for (int j = hrtf_database.HRTF_SIZE - 1; j >= 1; j--) {
            object.x[j] = object.x[j - 1];
        }
        object.x[0] = object.inputSound[index];

        // move index
        index++;
        if (index == object.inputSoundSize) index = 0;

        // apply convolution
        for (int j = 0; j < hrtf_database.HRTF_SIZE; j++) {
            leftOut += object.x[j] * hrtf_database.leftHRTF[angleIdx][j];
            rightOut += object.x[j] * hrtf_database.rightHRTF[angleIdx][j];
        }

        // apply distance weight and mix channel
        object.mixedOutput[i * 2] = leftOut * distance_weight;
        object.mixedOutput[i * 2 + 1] = rightOut * distance_weight;
    }
    object.idxHead = (PUSHABLE_SIZE_PER_CHANNEL + object.idxHead) % object.inputSoundSize;

    jfloatArray ret = env->NewFloatArray(PUSHABLE_SIZE);
    env->SetFloatArrayRegion(ret, 0, PUSHABLE_SIZE, object.mixedOutput);

    return ret;
}


JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundProvider_bypassSignalProcess(
        JNIEnv *env,
        jobject instance,
        jint SOHandle_j,
        jobject buf_j,
        jint buf_start_index_j) {

    SoundObject &object = SOList[SOHandle_j];

    LOGI("JNI log start bypass ");
    float *buf = (float *) ((char *) env->GetDirectBufferAddress(buf_j));

    int index = object.idxHead;
    LOGI("JNI log bypass  init");

    for (int i = 0; i < PUSHABLE_SIZE_PER_CHANNEL; i++) {
        // move index
        index++;
        if (index == object.inputSoundSize) index = 0;

        // mix channel
        buf[buf_start_index_j + i * 2] = object.inputSound[index];
        buf[buf_start_index_j + i * 2 + 1] = object.inputSound[index];
    }
    object.idxHead = (PUSHABLE_SIZE_PER_CHANNEL + object.idxHead) % object.inputSoundSize;
    LOGI("JNI log bypass  end");
}

//
//JNIEXPORT jfloatArray JNICALL
//Java_me_demetoir_a3dsound_1ndk_SoundProvider_sinalProcesssFFT(
//        JNIEnv *env,
//        jobject instance,
//        jint SOHandle_j) {
//
//    SoundObject &object = SOList[SOHandle_j];
//
////    LOGI("JNI log angle : %lf,  angle index: %d ",object.angle, getAngleIndex(object.angle));
//
//    float distance_weight = (MAX_DISTANCE - object.distance) / MAX_DISTANCE;
//    int angleIdx = angleToAngleIndex(object.angle);
//    int index = object.idxHead;
//
//    int N = 200 * 2;
//    int M = 200;
//    int L = 200;
//
//    //delay
//    for (int i = N - 1; i - L >= 0; i--) {
//        object.mixedOutput[i * 2] = object.mixedOutput[(i - L) * 2];
//        object.mixedOutput[i * 2 + 1] = object.mixedOutput[(i - L) * 2 + 1];
//
//    }
//    LOGI("OA delay");
//
//    // overlap add
//    CArray right_hrtf(N);
//    CArray left_hrtf(N);
//    for (int i = 0; i < 200; i++) {
//        right_hrtf[i] = hrtf_database.right[angleIdx][i];
//        left_hrtf[i] = hrtf_database.left[angleIdx][i];
//    }
//    LOGI("OA copy hrtf");
//
//    CArray leftSlice = object.input[std::slice(index, L, 1)];
//    CArray xLeft(N);
//    for (int i = 0; i < N; i++) {
//        xLeft[i] = leftSlice[i];
//    }
//    LOGI("OA slice input");
//
//    fft(right_hrtf);
//    fft(left_hrtf);
//    LOGI("OA fft hrtf");
//
//    fft(xLeft);
//    CArray xRight(N);
//    for (int i = 0; i < N; i++)
//        xRight[i] = xLeft[i];
//
//    LOGI("OA fft input");
//
//    for (int i = 0; i < N; i++) {
//        double real;
//        double imag;
//
//        real = xLeft[i].real() * left_hrtf[i].real() - xLeft[i].imag() * left_hrtf[i].imag();
//        imag = xLeft[i].real() * left_hrtf[i].real() + xLeft[i].imag() * left_hrtf[i].imag();
//        xLeft[i].real(real);
//        xLeft[i].imag(imag);
//
//
//        real = xRight[i].real() * right_hrtf[i].real() - xRight[i].imag() * right_hrtf[i].imag();
//        imag = xRight[i].real() * right_hrtf[i].real() + xRight[i].imag() * right_hrtf[i].imag();
//        xRight[i].real(real);
//        xRight[i].imag(imag);
//
////
////        xLeft[i] *= left_hrtf[i];
////        xRight[i] *= right_hrtf[i];
////
//    }
//    LOGI("OA multiple");
//
//    ifft(xLeft);
//    ifft(xRight);
//    LOGI("OA ifft");
//
//
//
////    mix channel
//    for (int i = 0; i < N; i++) {
//        object.mixedOutput[i * 2] += xLeft[i].real() * distance_weight;
//        object.mixedOutput[i * 2 + 1] += xRight[i].real() * distance_weight;
//        LOGI("OA mix %f %f", object.mixedOutput[i * 2], object.mixedOutput[i * 2 + 1]);
//
//    }
//    LOGI("OA mix");
//
//
//    object.idxHead = (PUSHABLE_SIZE_PER_CHANNEL + object.idxHead) % object.inputSoundSize;
//
//    jfloatArray ret = env->NewFloatArray(PUSHABLE_SIZE);
//    env->SetFloatArrayRegion(ret, 0, PUSHABLE_SIZE, object.mixedOutput);
//    LOGI("OA copy res");
//
//    return ret;
//}


#define HRTF_CHANNEL_LEFT 0
#define HRTF_CHANNEL_RIGHT 1
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

    if (channel == HRTF_CHANNEL_LEFT) {
        for (int i = 0; i < hrtf_size; i++) {
            hrtf_database.leftHRTF[angleIndex_j][i] = hrtf[i];
        }

    } else if (channel == HRTF_CHANNEL_RIGHT) {
        for (int i = 0; i < hrtf_size; i++) {
            hrtf_database.rightHRTF[angleIndex_j][i] = hrtf[i];
        }
    }

    env->ReleaseFloatArrayElements(HRTF_database_j, hrtf, 0);
    return;
}


//TODO 파일 분리 하는방법 알아두기
int genNewSOHANDLE() {

    return 0;
}

JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_initSoundObject(
        JNIEnv *env,
        jobject/* this */,
        jint x_size_j,
        jfloat x_j,
        jfloat y_j,
        jfloatArray sound_j) {

    SoundObject &unit = SOList[genNewSOHANDLE()];

    unit.x_size = x_size_j;
    for (int i = 0; i < x_size_j; i++) {
        unit.x[i] = 0;
    }
    for (int i = 0; i < x_size_j * 2; i++) {
        unit.mixedOutput[i] = 0;
    }

    unit.angle = 0;
    unit.distance = 0;
    unit.objectP.x = x_j;
    unit.objectP.y = y_j;
    unit.idxHead = 0;

    unit.inputSoundSize = env->GetArrayLength(sound_j);
    unit.inputSound = (float *) malloc(sizeof(float) * unit.inputSoundSize);
    float *inputSound = env->GetFloatArrayElements(sound_j, 0);
    for (int i = 0; i < unit.inputSoundSize; i++) {
        unit.inputSound[i] = inputSound[i];
    }

}

JNIEXPORT jint JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_loadSound(
        JNIEnv *env,
        jobject instance,
        jint handle_j,
        jfloatArray sound_j_) {
    jfloat *sound_j = env->GetFloatArrayElements(sound_j_, NULL);

    SoundObject &unit = SOList[handle_j];

    unit.inputSoundSize = env->GetArrayLength(sound_j_);

    if (unit.inputSound != NULL)
        free(unit.inputSound);
    unit.inputSound = (float *) malloc(sizeof(float) * unit.inputSoundSize);
    for (int i = 0; i < unit.inputSoundSize; i++) {
        unit.inputSound[i] = sound_j[i];
    }

    env->ReleaseFloatArrayElements(sound_j_, sound_j, 0);
}



// get set angle , distance

JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_setSOAngle
        (JNIEnv *env,
         jobject instance,
         jint handle_j,
         jfloat angle_j) {

    SOList[handle_j].angle = angle_j;
}

JNIEXPORT jfloat JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_getSOAngle(
        JNIEnv *env,
        jobject instance,
        jint handle_j) {

    return SOList[handle_j].angle;
}

JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_setSODistance(
        JNIEnv *env,
        jobject instance,
        jint handle_j,
        jfloat distance_j) {

    SOList[handle_j].distance = distance_j;
}

JNIEXPORT jfloat JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_getSODistance(
        JNIEnv *env,
        jobject instance,
        jint handle_j) {

    return SOList[handle_j].distance;
}


// get set point SO, center, line start, line end

void CPoint2DTojPoint2D(JNIEnv *env, jobject p_j, Point2D p) {
    jclass cls = env->GetObjectClass(p_j);

    jfieldID x_fid = env->GetFieldID(cls, "x", "F");
    env->SetFloatField(p_j, x_fid, p.x);

    jfieldID y_fid = env->GetFieldID(cls, "y", "F");
    env->SetFloatField(p_j, y_fid, p.y);
}

Point2D jPoint2DToCPoint2D(JNIEnv *env, jobject p_j) {
    Point2D newP;
    jclass cls = env->GetObjectClass(p_j);

    jfieldID x_fid = env->GetFieldID(cls, "x", "F");
    jfloat x_j = env->GetFloatField(p_j, x_fid);

    jfieldID y_fid = env->GetFieldID(cls, "y", "F");
    jfloat y_j = env->GetFloatField(p_j, y_fid);

    newP.x = x_j;
    newP.y = y_j;

    return newP;
}

JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_getSOPoint(
        JNIEnv *env,
        jobject instance,
        jint SOHandle_j,
        jobject p_j) {

    CPoint2DTojPoint2D(env, p_j, SOList[SOHandle_j].objectP);
}

JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_setSOPoint(
        JNIEnv *env,
        jobject instance,
        jint SOHandle_j,
        jobject p_j) {

    Point2D p = jPoint2DToCPoint2D(env, p_j);
    SOList[SOHandle_j].objectP.x = p.x;
    SOList[SOHandle_j].objectP.y = p.y;
    updateRadius(SOHandle_j);
    updateCenterAngle(SOHandle_j);
    updateDistance(SOHandle_j);
    updateAngle(SOHandle_j);
}


JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_getSOCenterPoint(
        JNIEnv *env,
        jobject instance,
        jint SOHandle_j,
        jobject p_j) {

    CPoint2DTojPoint2D(env, p_j, SOList[SOHandle_j].centerP);
}

JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_setSOCenterPoint(
        JNIEnv *env,
        jobject instance,
        jint SOHandle_j,
        jobject p_j) {

    Point2D p = jPoint2DToCPoint2D(env, p_j);
    SOList[SOHandle_j].centerP.x = p.x;
    SOList[SOHandle_j].centerP.y = p.y;
    updateRadius(SOHandle_j);
    updateCenterAngle(SOHandle_j);
}

JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_getSOStartPoint(
        JNIEnv *env,
        jobject instance,
        jint SOHandle_j,
        jobject p_j) {

    CPoint2DTojPoint2D(env, p_j, SOList[SOHandle_j].lineStartP);
}

JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_setSOStartPoint(
        JNIEnv *env,
        jobject instance,
        jint SOHandle_j,
        jobject p_j) {

    Point2D p = jPoint2DToCPoint2D(env, p_j);
    SOList[SOHandle_j].lineStartP.x = p.x;
    SOList[SOHandle_j].lineStartP.y = p.y;
}

JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_getSOEndPoint(
        JNIEnv *env,
        jobject instance,
        jint SOHandle_j,
        jobject p_j) {

    CPoint2DTojPoint2D(env, p_j, SOList[SOHandle_j].lineEndP);
}

JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_setSOEndPoint(
        JNIEnv *env,
        jobject instance,
        jint SOHandle_j,
        jobject p_j) {

    Point2D p = jPoint2DToCPoint2D(env, p_j);
    SOList[SOHandle_j].lineEndP.x = p.x;
    SOList[SOHandle_j].lineEndP.y = p.y;

}


JNIEXPORT jfloat JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_getSORadius(
        JNIEnv *env,
        jobject instance,
        jint SOHandle_j) {
    updateRadius(SOHandle_j);
    return SOList[SOHandle_j].radius;
}

JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_setSORadius(
        JNIEnv *env,
        jobject instance,
        jint SOHandle_j,
        jfloat radius_j) {

    SOList[SOHandle_j].radius = radius_j;
}


JNIEXPORT jfloat JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_getSOCenterAngle(
        JNIEnv *env,
        jobject instance,
        jint SOHandle_j) {

    return SOList[SOHandle_j].centerAngle;
}



JNIEXPORT jint JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_getOrbitMode(
        JNIEnv *env,
        jobject instance,
        jint SOHandle_j) {

    return SOList[SOHandle_j].Orbitmode;
}

JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_setOrbitMode(
        JNIEnv *env,
        jobject instance,
        jint SOHandle_j,
        jint mode_j) {

    SOList[SOHandle_j].Orbitmode = mode_j;
}


///test funciton
////////////////////////////////////////////////////////////////////////////////////////



}