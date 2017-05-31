#include <jni.h>
#include <string>
#include <math.h>
#include <android/log.h>
#include <valarray>
#include <complex>

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "libnav", __VA_ARGS__)

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "libnav", __VA_ARGS__)

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "libnav", __VA_ARGS__)

#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "libnav", __VA_ARGS__)

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "libnav", __VA_ARGS__)



//https://code.tutsplus.com/ko/tutorials/how-to-get-started-with-androids-native-development-kit--cms-27605




extern "C" {

const double PI = 3.141592653589793238460;

typedef std::complex<double> Complex;
typedef std::valarray<Complex> CArray;
using namespace std;

// Cooley–Tukey FFT (in-place, divide-and-conquer)
// Higher memory requirements and redundancy although more intuitive
void fft(CArray& x)
{
    const size_t N = x.size();
    if (N <= 1) return;

    // divide
    CArray even = x[std::slice(0, N/2, 2)];
    CArray  odd = x[std::slice(1, N/2, 2)];

    // conquer
    fft(even);
    fft(odd);

    // combine
    for (size_t k = 0; k < N/2; ++k)
    {
        Complex t = std::polar(1.0, -2 * PI * k / N) * odd[k];
        x[k    ] = even[k] + t;
        x[k+N/2] = even[k] - t;
    }
}

// inverse fft (in-place)
void ifft(CArray &x) {
    // conjugate the complex numbers
    x = x.apply(std::conj);

    // forward fft
    fft(x);

    // conjugate the complex numbers again
    x = x.apply(std::conj);

    // scale the numbers
    x /= x.size();
}

//define function here
#define MAX_SPO_UNIT_SIZE 10
#define DELAY_BUFFER_SIZE 1024

typedef struct {
    int x_size;
    float x[DELAY_BUFFER_SIZE];
    int head;
    float angle;
    float distance;
    float xCor;
    float yCor;

    float *inputSound;
    CArray input;
    int inputSoundSize;
    float mixedOutput[1024];

    CArray buffer;
    int N;

} SoundObejct;

SoundObejct SOList[MAX_SPO_UNIT_SIZE];

typedef struct {
    int HRTF_SIZE;
    float leftHRTF[100][200];
    float rightHRTF[100][200];
    CArray left[100];
    CArray right[100];

} HRTF_DATABASE;

HRTF_DATABASE hrtf_database;

int getAngleIndex(double angle) {
    if (angle < 0) angle = -angle;

    return (int) ((angle / 180.0) * 100);
}

#define MAX_DISTANCE 500
void updateDistance(int handle) {
    SOList[handle].distance = (float) sqrt(SOList[handle].xCor * SOList[handle].xCor
                                           + SOList[handle].yCor * SOList[handle].yCor);
    if (SOList[handle].distance > MAX_DISTANCE) {
        SOList[handle].distance = MAX_DISTANCE;
    }
}

void updateAngle(int handle) {
    SOList[handle].angle = (float) ((atan2(SOList[handle].yCor,
                                           SOList[handle].xCor) * 180) / M_PI);
}


#define PUSHABLE_SIZE_PER_CHANNEL 128
#define PUSHABLE_SIZE PUSHABLE_SIZE_PER_CHANNEL*2

JNIEXPORT jfloatArray JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundProvider_signalProcess(
        JNIEnv *env,
        jobject, /* this */
        jint SOHandle_j) {
    SoundObejct &object = SOList[SOHandle_j];

//    LOGI("JNI log angle : %lf,  angle index: %d ",object.angle, getAngleIndex(object.angle));

    float distance_weight = (MAX_DISTANCE - object.distance) / MAX_DISTANCE;
    int angleIdx = getAngleIndex(object.angle);
    int j;
    float leftOut = 0;
    float rightOut = 0;
    int index = object.head;

    for (int i = 0; i < PUSHABLE_SIZE_PER_CHANNEL; i++) {
        leftOut = 0;
        rightOut = 0;

        // delay
        for (j = hrtf_database.HRTF_SIZE - 1; j >= 1; j--) {
            object.x[j] = object.x[j - 1];
        }
        object.x[0] = object.inputSound[index];

        index++;
        if (index == object.inputSoundSize) index = 0;

        for (int j = 0; j < hrtf_database.HRTF_SIZE; j++) {
            leftOut += object.x[j] * hrtf_database.leftHRTF[angleIdx][j];
            rightOut += object.x[j] * hrtf_database.rightHRTF[angleIdx][j];
        }

        object.mixedOutput[i * 2] = leftOut * distance_weight;
        object.mixedOutput[i * 2 + 1] = rightOut * distance_weight;
    }
    object.head = (PUSHABLE_SIZE_PER_CHANNEL + object.head) % object.inputSoundSize;

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

    SoundObejct &object = SOList[SOHandle_j];

    LOGI("JNI log start bypass ");
    float *buf = (float *) ((char *) env->GetDirectBufferAddress(buf_j));


    int index = object.head;
    LOGI("JNI log bypass  init");


    for (int i = 0; i < PUSHABLE_SIZE_PER_CHANNEL; i++) {
        index++;
        if (index == object.inputSoundSize) index = 0;
        buf[buf_start_index_j + i * 2] = object.inputSound[index];
        buf[buf_start_index_j + i * 2 + 1] = object.inputSound[index];
    }
    object.head = (PUSHABLE_SIZE_PER_CHANNEL + object.head) % object.inputSoundSize;
    LOGI("JNI log bypass  end");
}


JNIEXPORT jfloatArray JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundProvider_sinalProcesssFFT(
        JNIEnv *env,
        jobject instance,
        jint SOHandle_j) {

    SoundObejct &object = SOList[SOHandle_j];

//    LOGI("JNI log angle : %lf,  angle index: %d ",object.angle, getAngleIndex(object.angle));

    float distance_weight = (MAX_DISTANCE - object.distance) / MAX_DISTANCE;
    int angleIdx = getAngleIndex(object.angle);
    int index = object.head;

    int N = 200 * 2;
    int M = 200;
    int L = 200;

    //delay
    for (int i = N - 1; i - L >= 0; i--) {
        object.mixedOutput[i * 2] = object.mixedOutput[(i - L) * 2];
        object.mixedOutput[i * 2 + 1] = object.mixedOutput[(i - L) * 2 + 1];

    }
    LOGI("OA delay");

    // overlap add
    CArray right_hrtf(N);
    CArray left_hrtf(N);
    for (int i = 0; i < 200; i++) {
        right_hrtf[i] = hrtf_database.right[angleIdx][i];
        left_hrtf[i] = hrtf_database.left[angleIdx][i];
    }
    LOGI("OA copy hrtf");

    CArray leftSlice = object.input[std::slice(index, L, 1)];
    CArray xLeft(N);
    for (int i = 0; i < N; i++) {
        xLeft[i] = leftSlice[i];
    }
    LOGI("OA slice input");

    fft(right_hrtf);
    fft(left_hrtf);
    LOGI("OA fft hrtf");

    fft(xLeft);
    CArray xRight(N);
    for(int i =0; i<N; i++)
        xRight[i] = xLeft[i];

    LOGI("OA fft input");

    for (int i = 0; i < N; i++) {
        double real;
        double imag;

        real = xLeft[i].real() * left_hrtf[i].real() - xLeft[i].imag() * left_hrtf[i].imag();
        imag = xLeft[i].real() * left_hrtf[i].real() + xLeft[i].imag() * left_hrtf[i].imag();
        xLeft[i].real(real);
        xLeft[i].imag(imag);


        real = xRight[i].real() * right_hrtf[i].real() - xRight[i].imag() * right_hrtf[i].imag();
        imag = xRight[i].real() * right_hrtf[i].real() + xRight[i].imag() * right_hrtf[i].imag();
        xRight[i].real(real);
        xRight[i].imag(imag);

//
//        xLeft[i] *= left_hrtf[i];
//        xRight[i] *= right_hrtf[i];
//
    }
    LOGI("OA multiple");

    ifft(xLeft);
    ifft(xRight);
    LOGI("OA ifft");



//    mix channel
    for (int i = 0; i < N; i++) {
        object.mixedOutput[i * 2] += xLeft[i].real() * distance_weight;
        object.mixedOutput[i * 2 + 1] += xRight[i].real() * distance_weight;
        LOGI("OA mix %f %f", object.mixedOutput[i * 2], object.mixedOutput[i * 2 + 1]);

    }
    LOGI("OA mix");


    object.head = (PUSHABLE_SIZE_PER_CHANNEL + object.head) % object.inputSoundSize;

    jfloatArray ret = env->NewFloatArray(PUSHABLE_SIZE);
    env->SetFloatArrayRegion(ret, 0, PUSHABLE_SIZE, object.mixedOutput);
    LOGI("OA copy res");

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
        hrtf_database.left[angleIndex_j].resize(512);
        for (int i = 0; i < hrtf_size; i++) {
            hrtf_database.leftHRTF[angleIndex_j][i] = hrtf[i];
            Complex left = hrtf[i];
            hrtf_database.left[angleIndex_j][i] = left;
        }

    } else if (channel == 1) {
        hrtf_database.right[angleIndex_j].resize(512);
        for (int i = 0; i < hrtf_size; i++) {
            hrtf_database.rightHRTF[angleIndex_j][i] = hrtf[i];
            Complex right = hrtf[i];
            hrtf_database.right[angleIndex_j][i] = right;
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
        jfloat x_j,
        jfloat y_j,
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

    unit.input.resize((size_t) (unit.inputSoundSize * 2));
    for (int i = 0; i < unit.inputSoundSize * 2; i++) {
        Complex a = inputSound[i % unit.inputSoundSize];
        unit.input[i] = a;
    }

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


JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_setSOX(
        JNIEnv *env,
        jobject instance,
        jint handle_j,
        jfloat x_j) {

    SoundObejct &object = SOList[handle_j];
    object.xCor = x_j;
    updateAngle(handle_j);
    updateDistance(handle_j);

}

JNIEXPORT jfloat JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_getSOX(
        JNIEnv *env,
        jobject instance,
        jint handle_j) {

    return SOList[handle_j].xCor;
}

JNIEXPORT void JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_setSOY(
        JNIEnv *env,
        jobject instance,
        jint handle_j,
        jfloat y_j) {

    SoundObejct &object = SOList[handle_j];
    object.yCor = y_j;
    updateAngle(handle_j);
    updateDistance(handle_j);

}

JNIEXPORT jfloat JNICALL
Java_me_demetoir_a3dsound_1ndk_SoundEngine_getSOY(
        JNIEnv *env,
        jobject instance,
        jint handle_j) {

    return SOList[handle_j].yCor;
}



///test funciton
////////////////////////////////////////////////////////////////////////////////////////



}