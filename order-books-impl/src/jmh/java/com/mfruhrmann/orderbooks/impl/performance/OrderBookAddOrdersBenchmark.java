package com.mfruhrmann.orderbooks.impl.performance;

import com.mfruhrmann.orderbooks.api.OrderBook;
import com.mfruhrmann.orderbooks.impl.BasicOrderBook;
import com.mfruhrmann.orderbooks.utils.OrderManager;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;


@Warmup(iterations = 1)
@Measurement(iterations = 1)
@Fork(value = 1, warmups = 0)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class OrderBookAddOrdersBenchmark {

    public static final int ORDERS_SIZE = 100_000;

    static List<OrderBook.Order> orders;

    @Setup(Level.Trial)
    public void doSetup() {
        OrderManager orderManager = new OrderManager();
        if (orders == null) {
            orders = IntStream.range(0, ORDERS_SIZE)
                    .mapToObj(x -> orderManager.createOrder(OrderBook.Side.values()[(x + 1) % 2], OrderBook.OrderType.LIMIT, 100 + (x % 10) - 5, 1))
                    .toList();
        }
    }


    @Benchmark
    public void basicOrderBookPerformanceAddOrders(OrdersState ordersState, Blackhole blackhole) {
        OrderBook orderBook = new BasicOrderBook();

        orders.forEach(orderBook::addOrder);

    }

    @Benchmark
    public void basicOrderBookPerformanceAddOrders2(OrdersState ordersState, Blackhole blackhole) {
        OrderBook orderBook = new BasicOrderBook();

        orders.forEach(orderBook::addOrder);

    }
}
