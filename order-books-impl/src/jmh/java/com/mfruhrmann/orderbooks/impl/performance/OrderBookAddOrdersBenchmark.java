package com.mfruhrmann.orderbooks.impl.performance;

import com.mfruhrmann.orderbooks.api.OrderBook;
import com.mfruhrmann.orderbooks.impl.BasicOrderBook;
import com.mfruhrmann.orderbooks.impl.BasicOrderBookArrayDeque;
import com.mfruhrmann.orderbooks.impl.ListBasedOrderBook;
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

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;


@Warmup(iterations = 2)
@Measurement(iterations = 3)
//@Fork(value = 1, warmups = 0, jvmArgs = "-Xmx256m")
@Fork(value = 1, warmups = 0)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class OrderBookAddOrdersBenchmark {

    public static final int ORDERS_SIZE = 1_000_000;  // basic order book roughly 17m addOrders/sec

    static List<OrderBook.Order> ordersForTradeMatching;
    static List<OrderBook.Order> ordersForLevelStacking;
    static Clock clock = Clock.tickMillis(ZoneId.systemDefault());

    @Setup(Level.Trial)
    public void doSetup() {
        OrderManager orderManager = new OrderManager();
        if (ordersForTradeMatching == null) {
            ordersForTradeMatching = IntStream.range(0, ORDERS_SIZE)
                    .mapToObj(x -> orderManager.createOrder(OrderBook.Side.values()[(x + 1) % 2], OrderBook.OrderType.LIMIT, 100 + (x % 10) - 5, 1))
                    .toList();
        }


        if (ordersForLevelStacking == null) {
            ordersForLevelStacking = IntStream.range(0, ORDERS_SIZE)
                    .mapToObj(x -> {
                        OrderBook.Side value = OrderBook.Side.values()[x % 2];
                        int delta = value == OrderBook.Side.BUY ? -(x % 20) : x % 20;
                        return orderManager.createOrder(value, OrderBook.OrderType.LIMIT, 100 + delta, 1);
                    })
                    .toList();
        }
    }

    @Benchmark
    public void addOrders_basicOrderBook(OrdersState ordersState, Blackhole blackhole) {
        OrderBook orderBook = new BasicOrderBook();
        ordersForTradeMatching.forEach(orderBook::addOrder);

    }

    @Benchmark
    public void addOrders_basicOrderBookArrayDeque(OrdersState ordersState, Blackhole blackhole) {
        OrderBook orderBook = new BasicOrderBookArrayDeque();
        ordersForTradeMatching.forEach(orderBook::addOrder);
    }


    @Benchmark
    public void addOrders_listBasedOrderBook(OrdersState ordersState, Blackhole blackhole) {
        OrderBook orderBook = new ListBasedOrderBook(1);
        ordersForTradeMatching.forEach(orderBook::addOrder);
    }

    @Benchmark
    public void basicOrder_LevelStacking(OrdersState ordersState, Blackhole blackhole) {
        OrderBook orderBook = new BasicOrderBook();
        ordersForLevelStacking.forEach(orderBook::addOrder);

    }

    @Benchmark
    public void basicOrder_LevelStacking_V2(OrdersState ordersState, Blackhole blackhole) {
        OrderBook orderBook = new BasicOrderBookArrayDeque();
        ordersForLevelStacking.forEach(orderBook::addOrder);
    }

    @Benchmark
    public void listBasedOrderBook_LevelStacking(OrdersState ordersState, Blackhole blackhole) {
        OrderBook orderBook = new ListBasedOrderBook(1);
        ordersForLevelStacking.forEach(orderBook::addOrder);
    }

}
