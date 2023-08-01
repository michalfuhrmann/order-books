package com.mfruhrmann.orderbooks.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface OrderBook {

    enum OrderType {
        //        MARKET,
        LIMIT,
//        IMMEDIATE_OR_CANCEL,
//        FILL_OR_KILL /* whole order  must be filled*/
    }

    enum Side {
        BUY, SELL
    }

    record Order(String id, Side side, OrderType type, Instant ts, double price, int size) {
    }

    record Trade(String id, String orderId, Instant ts, double price, double size) {
    }

    record BidAsk(Double bid, Double ask) {
    }


    /**
     * Adds a new order to the book. The type of the order
     * should determine how the order is added to the book.
     */
    String addOrder(Order order);

    void addTradeListener(OrderBookTradeListener orderBookTradeListener);

    Order getOrder(String id);

    enum CancelStatus {
        CANCELED, NOT_EXISTS
    }

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


    Map<String, Double> getAskLevels();

    Map<String, Double> getBidLevels();

//    /**
//     * Returns order book stats such as highest bid, lowest ask, etc.
//     */
//    Map<String, Double> getStats();

//    /**
//     * A method to handle exceptions or unexpected events in the system.
//     */
//    void handleException(Exception e);

    interface OrderBookTradeListener {
        void onTrade(OrderBook.Trade trade);
    }

}

