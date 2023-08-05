package com.mfruhrmann.orderbooks.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface OrderBook {

    /**
     * Adds a new order to the book. The type of the order
     * should determine how the order is added to the book.
     */
    String addOrder(Order order);

    void addTradeListener(OrderBookTradeListener orderBookTradeListener);

    Order getOrder(String id);

    /**
     * Removes the order from the book.
     */
    CancelStatus cancelOrder(String id);

    /**
     * Returns a list of all orders in the book.
     */
    List<Order> getAllOrders();

    /**
     * Returns the depth of the order book.
     */
    int getDepth();

    BidAsk getTopBidAsk();

    Map<Double, Double> getAskLevels();

    Map<Double, Double> getBidLevels();

    enum OrderType {
        //        MARKET,
        LIMIT,
//        IMMEDIATE_OR_CANCEL,
//        FILL_OR_KILL /* whole order  must be filled*/
    }

    enum Side {
        BUY, SELL
    }

    enum CancelStatus {
        CANCELED, NOT_EXISTS
    }


    interface OrderBookTradeListener {
        void onTrade(OrderBook.Trade trade);
    }

    interface Order {
        String id();

        Side side();

        OrderType type();

        Instant ts();

        double price();

        int size();
    }

    record Trade(String id, Set<String> orderIds, Instant ts, double price, int size) {
    }

    record BidAsk(Double bid, double bidSize, Double ask, double askSize) {
    }

}

