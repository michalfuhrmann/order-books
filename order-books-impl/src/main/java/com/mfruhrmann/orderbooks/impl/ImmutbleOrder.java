package com.mfruhrmann.orderbooks.impl;

import com.mfruhrmann.orderbooks.api.OrderBook;

import java.time.Instant;

public record ImmutbleOrder(String id, OrderBook.Side side, OrderBook.OrderType type, Instant ts, double price, int size) implements OrderBook.Order {
}
