package com.mfruhrmann.orderbooks.impl;

import com.mfruhrmann.orderbooks.api.OrderBook;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.mfruhrmann.orderbooks.api.OrderBook.CancelStatus.CANCELED;

/**
 * Basic implementation of the order book serving as a foundation for further improvements and as a baseline for performance comparison.
 * This implementation is thread-safe but the thread safety is done in a very basic way where we synchronize on the whole class,
 * so the performance of this implementation is expected to be relatively the lowest.
 */
public class BasicOrderBook implements OrderBook {

    private final List<OrderBookTradeListener> tradeListeners = new ArrayList<>();

    private final Map<String, Order> orders = new HashMap<>();
    private final Map<String, Trade> trades = new HashMap<>();

    private final TreeMap<Double, Queue<Order>> asks = new TreeMap<>();
    private final TreeMap<Double, Queue<Order>> bids = new TreeMap<>(Comparator.reverseOrder());

    @Override

    public synchronized String addOrder(Order order) {
        BidAsk bidAsk = getBidAsk();
        if (order.type() == OrderType.LIMIT) {
            if (order.side() == Side.BUY) {
                Double bestAsk = bidAsk.ask();
                if (bestAsk == null || bestAsk > order.price()) {
                    //we add to the bids
                }else{
                    //we have a trade
                }
            } else if (order.side() == Side.SELL) {
                Double bestBid = bidAsk.bid();
                if (bestBid == null || bestBid < order.price()) {
                    //we add to the asks
                }else{
                    //we have a trade
                }
            }
        }

        return null;
    }

    private BidAsk getBidAsk() {
        Double bid = bids.isEmpty() ? null : bids.firstKey();
        Double ask = asks.isEmpty() ? null :asks.firstKey();
        return new BidAsk(bid, ask);
    }

    @Override
    public void addTradeListener(OrderBookTradeListener orderBookTradeListener) {

    }

    @Override
    public synchronized Order getOrder(String id) {
        return null;
    }

    @Override
    public synchronized CancelStatus cancelOrder(String id) {
        return CANCELED;
    }

    @Override
    public synchronized List<Order> getAllOrders() {
        return List.of();
    }

    @Override
    public int getDepth() {
        return 0;
    }

    @Override
    public BidAsk getTopBidAsk() {
        return null;
    }

    @Override
    public synchronized Map<String, Double> getAskLevels() {
        return null;
    }

    @Override
    public synchronized Map<String, Double> getBidLevels() {
        return null;
    }
}
