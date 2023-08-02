package com.mfruhrmann.orderbooks.impl.performance;

import com.mfruhrmann.orderbooks.api.OrderBook;
import com.mfruhrmann.orderbooks.utils.OrderManager;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.List;
import java.util.stream.IntStream;

@State(Scope.Benchmark)
public class OrdersState {

    public static final int ORDERS_SIZE = 100_000;
    public List<OrderBook.Order> orders;

    @Setup(Level.Trial)
    public void doSetup() {
        OrderManager orderManager = new OrderManager();

        this.orders = IntStream.range(0, ORDERS_SIZE)
                .mapToObj(x -> orderManager.createOrder(OrderBook.Side.values()[x % 2], OrderBook.OrderType.LIMIT, 100 + Math.round((Math.random() * 10) - 5), 1))
                .toList();
    }
}
