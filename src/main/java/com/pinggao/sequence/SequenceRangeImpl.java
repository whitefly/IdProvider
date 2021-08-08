package com.pinggao.sequence;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.pinggao.sequence.api.SequenceRange;

public class SequenceRangeImpl implements SequenceRange {
    private long start;
    private long end;
    private long current;
    private Lock lock;

    public SequenceRangeImpl(long start, long end) {
        checkRange(start, end);
        this.start = start;
        this.end = end;
        current = start;
        lock = new ReentrantLock();
    }

    private static void checkRange(long start, long end) {
        if (end < start) {
            throw new RuntimeException("start should  less than end, but start is" + start + ",end is " + end);
        }
    }

    @Override
    public long nextValue() {
        if (over()) {
            return -1L;
        } else {
            lock.lock();
            try {
                long rnt = current;
                current++;
                return rnt;
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public boolean over() {
        return current >= end;
    }
}
