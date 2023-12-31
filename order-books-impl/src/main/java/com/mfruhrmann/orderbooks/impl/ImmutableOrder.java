package com.mfruhrmann.orderbooks.impl;

import com.mfruhrmann.orderbooks.api.OrderBook;

import java.time.Instant;

public record ImmutableOrder(String id, OrderBook.Side side, OrderBook.OrderType type, Instant ts, double price, int size)
        implements OrderBook.Order {

    public ImmutableOrder withNewSize(int newSize) {
        return new ImmutableOrder(id, side, type, ts, price, newSize);
    }
}
