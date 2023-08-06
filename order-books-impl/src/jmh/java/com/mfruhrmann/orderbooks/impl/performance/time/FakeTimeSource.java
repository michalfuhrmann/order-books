package com.mfruhrmann.orderbooks.impl.performance.time;

import com.mfruhrmann.orderbooks.api.time.TimeSource;

import java.util.concurrent.atomic.AtomicLong;

public class FakeTimeSource implements TimeSource {
    AtomicLong timeIncrementer = new AtomicLong(System.nanoTime());

    @Override
    public long getCurrentTime() {
        return timeIncrementer.incrementAndGet();
    }
}
