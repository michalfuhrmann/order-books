package com.mfruhrmann.orderbooks.impl.performance;

import com.mfruhrmann.orderbooks.api.time.TimeSource;
import com.mfruhrmann.orderbooks.impl.performance.time.FakeTimeSource;
import com.mfruhrmann.orderbooks.impl.time.SystemMillisTImesource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;


@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(value = 1, warmups = 0)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class OrderBookTimeSource {

    static Clock tickClock = Clock.tickMillis(ZoneId.systemDefault());
    TimeSource fakeTimeSource = new FakeTimeSource();
    TimeSource systemMillistimeSource = new SystemMillisTImesource();

    @Benchmark
    public void instant(OrdersState ordersState, Blackhole blackhole) {
        blackhole.consume(Instant.now());
    }

    @Benchmark
    public void nanoTime(OrdersState ordersState, Blackhole blackhole) {
        blackhole.consume(System.nanoTime());
    }

    @Benchmark
    public void timeMillis(OrdersState ordersState, Blackhole blackhole) {
        blackhole.consume(systemMillistimeSource.getCurrentTime());
    }

    @Benchmark
    public void fakeTimeSource(OrdersState ordersState, Blackhole blackhole) {
        blackhole.consume(fakeTimeSource.getCurrentTime());
    }


    @Benchmark
    public void tickClock(OrdersState ordersState, Blackhole blackhole) {
        blackhole.consume(Instant.now(tickClock));
    }
}
