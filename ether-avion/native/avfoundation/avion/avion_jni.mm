/*
 * Copyright (c) 2013 - 2016 Stefan Muller Arisona, Simon Schubiger
 * Copyright (c) 2013 - 2016 FHNW & ETH Zurich
 * All rights reserved.
 *
 * Contributions by: Filip Schramka, Samuel von Stachelski
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *  Neither the name of FHNW / ETH Zurich nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#import "JavaNativeFoundation/JNFJNI.h"

#import "avion.hpp"

#include <string>

#include "ch_fhnw_ether_avion_Avion.h"

JNIEXPORT jlong JNICALL Java_ch_fhnw_ether_video_avfoundation_Avion_nativeCreate
(JNIEnv * env, jclass, jstring javaURL) {
    JNF_COCOA_ENTER(env);
    
    const char* url = env->GetStringUTFChars(javaURL, JNI_FALSE);
    
    jlong nativeHandle = 0;
    try {
        nativeHandle = (jlong)new AVAssetWrapper(url);
    } catch(std::exception& e) {
        // fall through, return zero
    }
    
    env->ReleaseStringUTFChars(javaURL, url);
    
    return nativeHandle;

    JNF_COCOA_EXIT(env);
}



JNIEXPORT void JNICALL Java_ch_fhnw_ether_avion_Avion_nativeDispose
(JNIEnv * env, jclass, jlong nativeHandle) {
    JNF_COCOA_ENTER(env);
    
    delete (AVAssetWrapper*)nativeHandle;

    JNF_COCOA_EXIT(env);
}

JNIEXPORT jdouble JNICALL Java_ch_fhnw_ether_avion_Avion_nativeGetDuration
(JNIEnv * env, jclass, jlong nativeHandle) {
    JNF_COCOA_ENTER(env);
    
    return ((AVAssetWrapper*)nativeHandle)->getDuration();
    
    JNF_COCOA_EXIT(env);
}

JNIEXPORT jdouble JNICALL Java_ch_fhnw_ether_avion_Avion_nativeGetFrameRate
(JNIEnv * env, jclass, jlong nativeHandle) {
    JNF_COCOA_ENTER(env);
    
    return ((AVAssetWrapper*)nativeHandle)->getFrameRate();
    
    JNF_COCOA_EXIT(env);
}

JNIEXPORT jlong JNICALL Java_ch_fhnw_ether_avion_Avion_nativeGetFrameCount
(JNIEnv * env, jclass, jlong nativeHandle) {
    JNF_COCOA_ENTER(env);
    
    return ((AVAssetWrapper*)nativeHandle)->getFrameCount();
    
    JNF_COCOA_EXIT(env);
}

JNIEXPORT jint JNICALL Java_ch_fhnw_ether_avion_Avion_nativeGetWidth
(JNIEnv * env, jclass, jlong nativeHandle) {
    JNF_COCOA_ENTER(env);
    
    return ((AVAssetWrapper*)nativeHandle)->getWidth();
    
    JNF_COCOA_EXIT(env);
}

JNIEXPORT jint JNICALL Java_ch_fhnw_ether_avion_Avion_nativeGetHeight
(JNIEnv * env, jclass, jlong nativeHandle) {
    JNF_COCOA_ENTER(env);
    
    return ((AVAssetWrapper*)nativeHandle)->getHeight();
    
    JNF_COCOA_EXIT(env);
}

JNIEXPORT void JNICALL Java_ch_fhnw_ether_avion_Avion_nativeRewind
(JNIEnv * env, jclass, jlong nativeHandle) {
    JNF_COCOA_ENTER(env);

    ((AVAssetWrapper*)nativeHandle)->rewind();
    
    JNF_COCOA_EXIT(env);
}

JNIEXPORT jbyteArray JNICALL Java_ch_fhnw_ether_avion_Avion_nativeGetFrame
(JNIEnv * env, jclass, jlong nativeHandle, jdouble time) {
    JNF_COCOA_ENTER(env);

    return ((AVAssetWrapper*)nativeHandle)->getFrame(env, time);
    
    JNF_COCOA_EXIT(env);
}

JNIEXPORT jbyteArray JNICALL Java_ch_fhnw_ether_avion_Avion_nativeGetNextFrame
(JNIEnv * env, jclass, jlong nativeHandle) {
    JNF_COCOA_ENTER(env);

    return ((AVAssetWrapper*)nativeHandle)->getNextFrame(env);
    
    JNF_COCOA_EXIT(env);
}

JNIEXPORT jint JNICALL Java_ch_fhnw_ether_avion_Avion_nativeLoadFrame
(JNIEnv * env, jclass, jlong nativeHandle, jdouble time, jint textureId) {
    JNF_COCOA_ENTER(env);
    
    return ((AVAssetWrapper*)nativeHandle)->loadFrame(time, textureId);
    
    JNF_COCOA_EXIT(env);
}

JNIEXPORT jint JNICALL Java_ch_fhnw_ether_avion_Avion_nativeLoadFrames
(JNIEnv * env, jclass, jlong nativeHandle, jint numFrames, jint textureId) {
    JNF_COCOA_ENTER(env);
    
    return ((AVAssetWrapper*)nativeHandle)->loadFrames(numFrames, textureId);
    
    JNF_COCOA_EXIT(env);
}

