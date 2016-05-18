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

#include <algorithm>
#include <deque>
#include <utility>
#include <vector>

template<typename T>
class AudioQueue {
    struct Entry {
        Entry(double pts, T* samples, int length) : m_pts(pts), m_length(length), m_start(0) {
            m_samples = new T[length];
            std::copy(samples, samples + length, m_samples);
        }
        
        ~Entry() {
            delete[] m_samples;
        }
        
        double m_pts;
        T* m_samples;
        int m_length;
        int m_start;
    };
    
    const double m_sampleRate;

    std::deque<Entry> m_queue;
    
    int m_queueSize;

public:
    AudioQueue(double sampleRate) : m_sampleRate(sampleRate) {
    }
    
    ~AudioQueue() {
    }
    
    int size() const {
        return m_queueSize;
    }
    
    void put(double pts, T* src, int length) {
        m_queueSize += length;
        m_queue.push_back(Entry(pts, src, length));
    }
    
    void take(double& pts, T* dst, int length) {
        m_queueSize -= length;
        Entry& entry = m_queue.front();
        pts = entry.m_pts + (double)entry.m_start / m_sampleRate;
        while (length > 0) {
            int l = std::min(length, entry.m_length - entry.m_start);
            std::copy(entry.m_samples + entry.m_start, entry.m_samples + l, dst);
            entry.m_start += l;
            if (entry.m_start == entry.m_length) {
                m_queue.pop_front();
                entry = m_queue.front();
            }
            dst += l;
            length -= l;
        }
    }
    
    void clear() {
        m_queue.clear();
    }
};
