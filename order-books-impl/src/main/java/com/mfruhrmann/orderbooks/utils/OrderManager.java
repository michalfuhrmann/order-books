package com.mfruhrmann.orderbooks.utils;

import com.mfruhrmann.orderbooks.api.OrderBook;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class OrderManager {

    private final AtomicLong atomicLong = new AtomicLong(1);

    public OrderBook.Order createOrder(OrderBook.Side side, OrderBook.OrderType type, double price, int size) {
        return new OrderBook.Order(String.valueOf(atomicLong.incrementAndGet()), side, type, Instant.now(), price, size);
    }
}
