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

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>

#import "avion.hpp"

#include "audio_buffer.hpp"

// TODO:
// - flip image vertically, optimise pixel transfer
// - audio buffer size & sample rate request support
// - complete audio
// - API: replace seek with range (start + end time)?

class AVAssetDecoder : public AvionDecoder {
private:
    const std::string url;
    const bool decodeAudio;
    const bool decodeVideo;

    const int audioBufferSize;
    const bool audioInterleaved;
    AudioQueue<float> audioQueue;

    AVAsset* asset;
    AVAssetTrack* audioTrack;
    AVAssetTrack* videoTrack;
    
    double videoFrameRate;
    CGSize videoSize;
    
    double duration;

    AVAssetReader* audioReader;
    AVAssetReader* videoReader;

public:
    AVAssetDecoder(std::string url, bool decodeAudio, bool decodeVideo, int audioBufferSize, bool audioInterleaved, double audioSampleRate) :
    url(url), decodeAudio(decodeAudio), decodeVideo(decodeVideo),
    audioBufferSize(audioBufferSize), audioInterleaved(audioInterleaved), audioQueue(audioSampleRate),
    audioReader(nullptr), videoReader(nullptr) {
        
        NSURL* nsUrl = [NSURL URLWithString:[NSString stringWithCString:url.c_str() encoding:NSUTF8StringEncoding]];
        if (!nsUrl) {
            MSG("avf: invalid url '%s'\n", url.c_str());
            throw std::invalid_argument("invalid url " + url);
        }
        
        NSDictionary* options = @{ AVURLAssetPreferPreciseDurationAndTimingKey : @YES };
        
        //---- asset
        asset = [AVURLAsset URLAssetWithURL:nsUrl options:options];
        if (!asset) {
            MSG("avf: invalid url '%s'\n", url.c_str());
            throw std::invalid_argument("invalid url " + url);
        }
        
        //--- audio track
        if (decodeAudio) {
            NSArray* audioTracks = [asset tracksWithMediaType:AVMediaTypeAudio];
            if ([audioTracks count] < 1) {
                MSG("avf: no audio track for '%s'\n", url.c_str());
                throw std::invalid_argument("no audio track");
            }
            audioTrack = [audioTracks objectAtIndex:0];
        }
        
        //--- video track
        if (decodeVideo) {
            NSArray* videoTracks = [asset tracksWithMediaType:AVMediaTypeVideo];
            if ([videoTracks count] < 1) {
                MSG("avf: no video track for '%s'\n", url.c_str());
                throw std::invalid_argument("no video track");
            }
            videoTrack = [videoTracks objectAtIndex:0];
            videoFrameRate = [videoTrack nominalFrameRate];
            videoSize = [videoTrack naturalSize];
        }
        
        duration = CMTimeGetSeconds([asset duration]);
        
        seek(0.0);
        
        MSG("avf: %s: duration=%f framerate=%f size=%dx%d\n", url.c_str(), duration, videoFrameRate, (int)videoSize.width, (int)videoSize.height);
    }
    
    virtual ~AVAssetDecoder() {
        if (audioReader)
            [audioReader release];
        if (videoReader)
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
        if (decodeAudio) {
            if (audioReader != nullptr)
                [audioReader release];
            
            audioReader = [[AVAssetReader alloc] initWithAsset:asset error:&error];
            if (!audioReader || error) {
                MSG("avf: could not initialize audio reader for '%s'\n", url.c_str());
                throw std::invalid_argument("could not initialize audio reader");
            }
            
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
                MSG("avf: could not start reading audio from '%s': %s\n", url.c_str(), [[[audioReader error] localizedDescription] UTF8String]);
                throw std::invalid_argument("could not start reading audio");
            }
        }
        
        //---- setup video reader
        if (decodeVideo) {
            if (videoReader != nullptr)
                [audioReader release];
            
            videoReader = [[AVAssetReader alloc] initWithAsset:asset error:&error];
            if (!videoReader || error) {
                MSG("avf: could not initialize video reader for '%s'\n", url.c_str());
                throw std::invalid_argument("could not initialize video reader");
            }
            
            NSDictionary* videoSettings = @{
                                            (id)kCVPixelBufferPixelFormatTypeKey: [NSNumber numberWithUnsignedInt:kCVPixelFormatType_32BGRA]
                                            };
            [videoReader addOutput:[AVAssetReaderTrackOutput assetReaderTrackOutputWithTrack:videoTrack outputSettings:videoSettings]];
            
            videoReader.timeRange = timeRange;
            
            if ([videoReader startReading] != YES) {
                [videoReader release];
                MSG("avf: could not start reading video from '%s': %s\n", url.c_str(), [[[videoReader error] localizedDescription] UTF8String]);
                throw std::invalid_argument("could not start reading video");
            }
        }
    }
    
    int getNextAudioFrame(float* buffer, double& pts) {
        if (!decodeAudio)
            return -1;
        
        if ([audioReader status] != AVAssetReaderStatusReading) {
            MSG("avf: get next audio frame: reached end of media\n");
            return -1;
        }
        
        AVAssetReaderOutput* output = [audioReader.outputs objectAtIndex:0];
        CMSampleBufferRef sampleBuffer = [output copyNextSampleBuffer];
        if (!sampleBuffer) {
            MSG("avf: get next audio frame: could not copy audio sample buffer\n");
            return -1;
        }
        
        pts = CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(sampleBuffer));
        
        CMBlockBufferRef blockBuffer = CMSampleBufferGetDataBuffer(sampleBuffer);
        if (!blockBuffer) {
            MSG("avf: get next audio frame: could not get audio block buffer\n");
            CFRelease(sampleBuffer);
            return -1;
        }
        
        size_t length = 0;
        float* data = nullptr;
        if (CMBlockBufferGetDataPointer(blockBuffer, 0, nullptr, &length, (char**)&data) != kCMBlockBufferNoErr) {
            MSG("avf: get next audio frame: cannot get audio data\n");
            CFRelease(sampleBuffer);
            return -1;
        }
        length /= 4;
        
        printf("got audio samples: %ld \n", length);
        
        CFRelease(sampleBuffer);
        
        return 0;
    }
    
    int getNextVideoFrame(uint8_t* buffer, double& pts) {
        if (!decodeVideo)
            return -1;
        
        if ([videoReader status] != AVAssetReaderStatusReading) {
            MSG("avf: get next video frame: reached end of media\n");
            return -1;
        }
        
        AVAssetReaderOutput* output = [videoReader.outputs objectAtIndex:0];
        CMSampleBufferRef sampleBuffer = [output copyNextSampleBuffer];
        if (!sampleBuffer) {
            MSG("avf: get next video frame: could not copy video sample buffer\n");
            return -1;
        }
        
        pts = CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(sampleBuffer));
        
        CVImageBufferRef imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);
        
        // lock the image buffer
        CVPixelBufferLockBaseAddress(imageBuffer, 0);
        
        // note: if movie width cannot be divided by 4 it seems the movie is scaled up to the next width that can
        // i.e. if you open a move with 1278 pixels width, here, the imageBuffer will have a width of 1280.
        // for now, we just skip the remaining pixel columns...
        int width = getVideoWidth();
        int height = getVideoHeight();
        int bytesPerRow = (int)CVPixelBufferGetBytesPerRow(imageBuffer);
        int skip = bytesPerRow - width * 4;
        int length = width * height * 4;
        MSG("avf: w=%d h=%d bpr=%d skip=%d length=%d\n", width, height, bytesPerRow, skip, length);
        
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
        
        return 0;
    }
};

AvionDecoder* AvionDecoder::create(std::string url, bool decodeAudio, bool decodeVideo, int audioBufferSize, bool audioInterleaved, double audioSampleRate) {
    return new AVAssetDecoder(url, decodeAudio, decodeVideo, audioBufferSize, audioInterleaved, audioSampleRate);
}
