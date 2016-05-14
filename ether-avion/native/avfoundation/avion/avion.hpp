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

#pragma once

#define MSG(...) { printf(__VA_ARGS__); fflush(stdout); }

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>

#include <string>

class AVAssetWrapper {
private:
    std::string url;
    AVAssetReader* reader;
    
    double duration;
    double frameRate;
    CGSize size;
    
public:
    AVAssetWrapper(std::string url) : url(url), reader(nullptr) {
        rewind();
        //MSG("avfoundation asset: %s: duration=%f framerate=%f size=%dx%d\n", url.c_str(), duration, frameRate, (int)size.width, (int)size.height);
    }
    
    ~AVAssetWrapper() {
        [reader release];
    }
    
    double getDuration() {
        return duration;
    }
    
    double getFrameRate() {
        return frameRate;
    }
    
    long getFrameCount() {
        return duration * frameRate;
    }
    
    int getWidth() {
        return size.width;
    }
    
    int getHeight() {
        return size.height;
    }
    
    void rewind() {
        if (reader != nullptr) {
            [reader release];
        }
        
        NSURL* nsUrl = [NSURL URLWithString:[NSString stringWithCString:url.c_str() encoding:NSUTF8StringEncoding]];
        if (!nsUrl) {
            MSG("avassetwrapper: invalid url '%s'\n", url.c_str());
            throw std::invalid_argument("invalid url");
        }
        
        NSDictionary* options = @{ AVURLAssetPreferPreciseDurationAndTimingKey : @YES };
        
        AVAsset* asset = [AVURLAsset URLAssetWithURL:nsUrl options:options];
        if (!asset) {
            MSG("avassetwrapper: invalid url '%s'\n", url.c_str());
            throw std::invalid_argument("invalid url");
        }
        
        NSArray* tracks = [asset tracksWithMediaType:AVMediaTypeVideo];
        if ([tracks count] < 1) {
            MSG("avassetwrapper: no video track for '%s'\n", url.c_str());
            throw std::invalid_argument("no video track");
        }
        AVAssetTrack* videoTrack = [tracks objectAtIndex:0];
        
        duration = CMTimeGetSeconds([asset duration]);
        frameRate = [videoTrack nominalFrameRate];
        size = [videoTrack naturalSize];
        
        // create reader (for sequential frame-by-frame access)
        
        NSError* error = nil;
        reader = [[AVAssetReader alloc] initWithAsset:asset error:&error];
        if (!reader || error) {
            MSG("avassetwrapper: could not initialize reader for '%s'\n", url.c_str());
            throw std::invalid_argument("could not initialize reader");
        }
        
        NSDictionary* settings = [NSDictionary dictionaryWithObjectsAndKeys:
                                  [NSNumber numberWithUnsignedInt:kCVPixelFormatType_32BGRA],
                                  (NSString*)kCVPixelBufferPixelFormatTypeKey,
                                  nil];
        [reader addOutput:[AVAssetReaderTrackOutput assetReaderTrackOutputWithTrack:videoTrack outputSettings:settings]];
        if ([reader startReading] != YES) {
            [reader release];
            MSG("avfoundation: could not start reading from '%s': %s\n", url.c_str(), [[[reader error] localizedDescription] UTF8String]);
            throw std::invalid_argument("could not start reading");
        }
    }

    jbyteArray getNextFrame(JNIEnv* env) {
        if ([reader status] != AVAssetReaderStatusReading) {
            MSG("get next frame: reached end of movie\n");
            return nullptr;
        }
        
        AVAssetReaderTrackOutput* output = [reader.outputs objectAtIndex:0];
        CMSampleBufferRef sampleBuffer = [output copyNextSampleBuffer];
        if (!sampleBuffer) {
            MSG("get next frame: could not copy sample buffer\n");
            return nullptr;
        }
        
        CVImageBufferRef imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);
        
        // lock the image buffer
        CVPixelBufferLockBaseAddress(imageBuffer, 0);
        
        // XXX: note if movie width cannot be divided by 4 it seems the movie is scaled up to the next width that can
        // i.e. if you open a moive with 1278 pixels width, here, the imageBuffer will have a width of 1280. this of course
        // screws up our interface further up a bit, which relies on movie.getWidth ... thus we for now just ignore the scaling.
        //int width = (int)CVPixelBufferGetWidth(imageBuffer);
        //int height = (int)CVPixelBufferGetHeight(imageBuffer);
        int width = getWidth();
        int height = getHeight();
        int bytesPerRow = (int)CVPixelBufferGetBytesPerRow(imageBuffer);
        int skip = bytesPerRow - width * 4;
        int length = width * height * 4;
        //MSG("w=%d h=%d bpr=%d skip=%d length=%d\n", width, height, bytesPerRow, skip, length);
        
        jbyteArray array = env->NewByteArray((int)length);
        uint8_t* arrayElements = (uint8_t*)env->GetByteArrayElements(array, nullptr);
        uint8_t* dst = arrayElements;
        for (int y = height; --y >= 0;) {
            uint8_t* src = (uint8_t*)CVPixelBufferGetBaseAddress(imageBuffer) + y * bytesPerRow;
            for (int x = 0; x < width; ++x) {
                uint8_t b = *src++;
                uint8_t g = *src++;
                uint8_t r = *src++;
                uint8_t a = *src++;
                *dst++ = r;
                *dst++ = g;
                *dst++ = b;
                *dst++ = a;
            }
            src += skip;
        }
        env->ReleaseByteArrayElements(array, (jbyte*)arrayElements, 0);
        
        // unlock the image buffer & cleanup
        CVPixelBufferUnlockBaseAddress(imageBuffer, 0);
        CFRelease(sampleBuffer);
        
        return array;
    }
};
