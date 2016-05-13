/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class ch_fhnw_ether_avion_Avion */

#ifndef _Included_ch_fhnw_ether_avion_Avion
#define _Included_ch_fhnw_ether_avion_Avion
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     ch_fhnw_ether_avion_Avion
 * Method:    nativeCreate
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_ch_fhnw_ether_avion_Avion_nativeCreate
  (JNIEnv *, jclass, jstring);

/*
 * Class:     ch_fhnw_ether_avion_Avion
 * Method:    nativeDispose
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ch_fhnw_ether_avion_Avion_nativeDispose
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_fhnw_ether_avion_Avion
 * Method:    nativeGetDuration
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_ch_fhnw_ether_avion_Avion_nativeGetDuration
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_fhnw_ether_avion_Avion
 * Method:    nativeGetFrameRate
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_ch_fhnw_ether_avion_Avion_nativeGetFrameRate
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_fhnw_ether_avion_Avion
 * Method:    nativeGetFrameCount
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_ch_fhnw_ether_avion_Avion_nativeGetFrameCount
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_fhnw_ether_avion_Avion
 * Method:    nativeGetWidth
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_ch_fhnw_ether_avion_Avion_nativeGetWidth
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_fhnw_ether_avion_Avion
 * Method:    nativeGetHeight
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_ch_fhnw_ether_avion_Avion_nativeGetHeight
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_fhnw_ether_avion_Avion
 * Method:    nativeRewind
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ch_fhnw_ether_avion_Avion_nativeRewind
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_fhnw_ether_avion_Avion
 * Method:    nativeGetFrame
 * Signature: (JD)[B
 */
JNIEXPORT jbyteArray JNICALL Java_ch_fhnw_ether_avion_Avion_nativeGetFrame
  (JNIEnv *, jclass, jlong, jdouble);

/*
 * Class:     ch_fhnw_ether_avion_Avion
 * Method:    nativeGetNextFrame
 * Signature: (J)[B
 */
JNIEXPORT jbyteArray JNICALL Java_ch_fhnw_ether_avion_Avion_nativeGetNextFrame
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_fhnw_ether_avion_Avion
 * Method:    nativeLoadFrame
 * Signature: (JDI)I
 */
JNIEXPORT jint JNICALL Java_ch_fhnw_ether_avion_Avion_nativeLoadFrame
  (JNIEnv *, jclass, jlong, jdouble, jint);

/*
 * Class:     ch_fhnw_ether_avion_Avion
 * Method:    nativeLoadFrames
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL Java_ch_fhnw_ether_avion_Avion_nativeLoadFrames
  (JNIEnv *, jclass, jlong, jint, jint);

#ifdef __cplusplus
}
#endif
#endif
