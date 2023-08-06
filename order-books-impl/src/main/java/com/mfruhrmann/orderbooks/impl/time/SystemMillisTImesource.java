package com.mfruhrmann.orderbooks.impl.time;

import com.mfruhrmann.orderbooks.api.time.TimeSource;

public class SystemMillisTImesource implements TimeSource {
    @Override
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }
}
