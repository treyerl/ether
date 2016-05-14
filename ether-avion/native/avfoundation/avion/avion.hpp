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
    AVAsset* asset;
    AVAssetTrack* audioTrack;
    AVAssetTrack* videoTrack;
    
    AVAssetReader* audioReader;
    AVAssetReader* videoReader;
    
    double duration;
    double videoFrameRate;
    CGSize videoSize;
    
public:
    AVAssetWrapper(std::string url) : url(url), asset(nullptr), audioTrack(nullptr), videoTrack(nullptr), audioReader(nullptr), videoReader(nullptr) {

        NSURL* nsUrl = [NSURL URLWithString:[NSString stringWithCString:url.c_str() encoding:NSUTF8StringEncoding]];
        if (!nsUrl) {
            MSG("avassetwrapper: invalid url '%s'\n", url.c_str());
            throw std::invalid_argument("invalid url " + url);
        }
        
        NSDictionary* options = @{ AVURLAssetPreferPreciseDurationAndTimingKey : @YES };
        
        //---- asset
        asset = [AVURLAsset URLAssetWithURL:nsUrl options:options];
        if (!asset) {
            MSG("avassetwrapper: invalid url '%s'\n", url.c_str());
            throw std::invalid_argument("invalid url " + url);
        }
        
        //--- audio track
        NSArray* audioTracks = [asset tracksWithMediaType:AVMediaTypeAudio];
        if ([audioTracks count] < 1) {
            MSG("avassetwrapper: no audio track for '%s'\n", url.c_str());
            throw std::invalid_argument("no audio track");
        }
        audioTrack = [audioTracks objectAtIndex:0];
        
        //--- video track
        NSArray* videoTracks = [asset tracksWithMediaType:AVMediaTypeVideo];
        if ([videoTracks count] < 1) {
            MSG("avassetwrapper: no video track for '%s'\n", url.c_str());
            throw std::invalid_argument("no video track");
        }
        videoTrack = [videoTracks objectAtIndex:0];
        
        duration = CMTimeGetSeconds([asset duration]);

        videoFrameRate = [videoTrack nominalFrameRate];
        videoSize = [videoTrack naturalSize];
                
        seek(0.0);

        MSG("avfoundation asset: %s: duration=%f framerate=%f size=%dx%d\n",
            url.c_str(), duration, videoFrameRate, (int)videoSize.width, (int)videoSize.height);
    }
    
    ~AVAssetWrapper() {
        [audioReader release];
        [videoReader release];
    }
    
    double getDuration() {
        return duration;
    }
    
    double getVideoFrameRate() {
        return videoFrameRate;
    }
    
    int getVideoWidth() {
        return videoSize.width;
    }
    
    int getVideoHeight() {
        return videoSize.height;
    }
    
    void seek(double time) {
        NSError* error = nil;
        CMTimeRange timeRange = CMTimeRangeMake(CMTimeMakeWithSeconds(time, 1), kCMTimePositiveInfinity);
        
        //---- setup audio reader
        if (audioReader != nullptr)
            [audioReader release];
        
        audioReader = [[AVAssetReader alloc] initWithAsset:asset error:&error];
        if (!audioReader || error) {
            MSG("avassetwrapper: could not initialize audio reader for '%s'\n", url.c_str());
            throw std::invalid_argument("could not initialize audio reader");
        }
        
        //NSDictionary* audioSettings = @{ AVFormatIDKey : [NSNumber numberWithUnsignedInt:kAudioFormatLinearPCM] };
        //[audioReader addOutput:[AVAssetReaderTrackOutput assetReaderTrackOutputWithTrack:audioTrack outputSettings:audioSettings]];
        
        NSDictionary* audioSettings = @{
            AVFormatIDKey : [NSNumber numberWithUnsignedInt:kAudioFormatLinearPCM],
            AVSampleRateKey : [NSNumber numberWithFloat:44100.0],
            AVNumberOfChannelsKey : [NSNumber numberWithInt:2],
            AVLinearPCMBitDepthKey : [NSNumber numberWithInt:32],
            AVLinearPCMIsNonInterleaved : [NSNumber numberWithBool:NO],
            AVLinearPCMIsFloatKey : [NSNumber numberWithBool:YES],
            AVLinearPCMIsBigEndianKey : [NSNumber numberWithBool:NO],
        };
        [audioReader addOutput:[AVAssetReaderAudioMixOutput assetReaderAudioMixOutputWithAudioTracks:@[audioTrack] audioSettings:audioSettings]];

        audioReader.timeRange = timeRange;
        
        if ([audioReader startReading] != YES) {
            [audioReader release];
            MSG("avfoundation: could not start reading audio from '%s': %s\n", url.c_str(), [[[audioReader error] localizedDescription] UTF8String]);
            throw std::invalid_argument("could not start reading audio");
        }
        
        //---- setup video reader
        if (videoReader != nullptr)
            [audioReader release];
        
        videoReader = [[AVAssetReader alloc] initWithAsset:asset error:&error];
        if (!videoReader || error) {
            MSG("avassetwrapper: could not initialize video reader for '%s'\n", url.c_str());
            throw std::invalid_argument("could not initialize video reader");
        }
        
        NSDictionary* videoSettings = @{
            (id)kCVPixelBufferPixelFormatTypeKey: [NSNumber numberWithUnsignedInt:kCVPixelFormatType_32BGRA]
        };
        [videoReader addOutput:[AVAssetReaderTrackOutput assetReaderTrackOutputWithTrack:videoTrack outputSettings:videoSettings]];

        videoReader.timeRange = timeRange;

        if ([videoReader startReading] != YES) {
            [videoReader release];
            MSG("avfoundation: could not start reading video from '%s': %s\n", url.c_str(), [[[videoReader error] localizedDescription] UTF8String]);
            throw std::invalid_argument("could not start reading video");
        }
    }
    
    double getNextAudioFrame(float* buffer) {
        if ([audioReader status] != AVAssetReaderStatusReading) {
            MSG("get next audio frame: reached end of media\n");
            return -1;
        }
        
        AVAssetReaderOutput* output = [audioReader.outputs objectAtIndex:0];
        CMSampleBufferRef sampleBuffer = [output copyNextSampleBuffer];
        if (!sampleBuffer) {
            MSG("get next audio frame: could not copy audio sample buffer\n");
            return -1;
        }
        
        double pts = CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(sampleBuffer));

        
        
        CMItemCount numSamplesInBuffer = CMSampleBufferGetNumSamples(sampleBuffer);
        

        
        CMBlockBufferRef blockBuffer = CMSampleBufferGetDataBuffer(sampleBuffer);
        if (!blockBuffer) {
            MSG("get next audio frame: could not get audio block buffer\n");
            CFRelease(sampleBuffer);
            return -1;
        }

        size_t length = 0;
        void* data = nullptr;
        if (CMBlockBufferGetDataPointer(blockBuffer, 0, nullptr, &length, (char**)&data) != kCMBlockBufferNoErr) {
            MSG("get next audio frame: cannot get audio data\n");
            CFRelease(sampleBuffer);
            return -1;
        }
        
        printf("got audio samples: %ld %ld \n", numSamplesInBuffer, length);

        CFRelease(sampleBuffer);
        
        return pts;
    }
    
    double getNextVideoFrame(uint8_t* buffer) {
        if ([videoReader status] != AVAssetReaderStatusReading) {
            MSG("get next video frame: reached end of media\n");
            return -1;
        }

        AVAssetReaderOutput* output = [videoReader.outputs objectAtIndex:0];
        CMSampleBufferRef sampleBuffer = [output copyNextSampleBuffer];
        if (!sampleBuffer) {
            MSG("get next video frame: could not copy video sample buffer\n");
            return -1;
        }
        
        double pts = CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(sampleBuffer));
        
        CVImageBufferRef imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);
        
        // lock the image buffer
        CVPixelBufferLockBaseAddress(imageBuffer, 0);
        
        // XXX: note if movie width cannot be divided by 4 it seems the movie is scaled up to the next width that can
        // i.e. if you open a moive with 1278 pixels width, here, the imageBuffer will have a width of 1280. this of course
        // screws up our interface further up a bit, which relies on movie.getWidth ... thus we for now just ignore the scaling.
        //int width = (int)CVPixelBufferGetWidth(imageBuffer);
        //int height = (int)CVPixelBufferGetHeight(imageBuffer);
        int width = getVideoWidth();
        int height = getVideoHeight();
        int bytesPerRow = (int)CVPixelBufferGetBytesPerRow(imageBuffer);
        int skip = bytesPerRow - width * 4;
        int length = width * height * 4;
        MSG("w=%d h=%d bpr=%d skip=%d length=%d\n", width, height, bytesPerRow, skip, length);
        
        for (int y = height; --y >= 0;) {
            uint8_t* src = (uint8_t*)CVPixelBufferGetBaseAddress(imageBuffer) + y * bytesPerRow;
            for (int x = 0; x < width; ++x) {
                uint8_t b = *src++;
                uint8_t g = *src++;
                uint8_t r = *src++;
                uint8_t a = *src++;
                *buffer++ = r;
                *buffer++ = g;
                *buffer++ = b;
                *buffer++ = a;
            }
            src += skip;
        }
        
        // unlock the image buffer & cleanup
        CVPixelBufferUnlockBaseAddress(imageBuffer, 0);
        CFRelease(sampleBuffer);
        
        return pts;
    }
};
