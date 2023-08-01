package com.mfruhrmann.orderbooks.impl;

import com.mfruhrmann.orderbooks.api.OrderBook;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.mfruhrmann.orderbooks.api.OrderBook.CancelStatus.CANCELED;

/**
 * Basic implementation of the order book serving as a foundation for further improvements and as a baseline for performance comparison.
 * This implementation is thread-safe but the thread safety is done in a very basic way where we synchronize on the whole class,
 * so the performance of this implementation is expected to be relatively the lowest.
 */
public class BasicOrderBook implements OrderBook {

    private final List<OrderBookTradeListener> tradeListeners = new ArrayList<>();

    private final Map<String, Order> orders = new HashMap<>();
    private final TreeMap<Double, Queue<Order>> asks = new TreeMap<>();
    private final TreeMap<Double, Queue<Order>> bids = new TreeMap<>(Comparator.reverseOrder());

    @Override
    public synchronized String addOrder(Order order) {
        BidAsk bidAsk = getBidAsk();
        if (order.type() == OrderType.LIMIT) {
            if (order.side() == Side.BUY) {
                Double bestAsk = bidAsk.ask();
                if (bestAsk == null || bestAsk > order.price()) {
                    orders.put(order.id(), order);
                    bids.computeIfAbsent(order.price(), aDouble -> new LinkedList<>()).add(order);
                    //we add to the bids
                } else {
                    //we have a trade
                    handleTrade(order, asks);
                }
            } else if (order.side() == Side.SELL) {
                Double bestBid = bidAsk.bid();
                if (bestBid == null || bestBid < order.price()) {
                    orders.put(order.id(), order);
                    asks.computeIfAbsent(order.price(), aDouble -> new LinkedList<>()).add(order);
                } else {
                    //we have a trade
                    handleTrade(order, bids);
                }
            }
        }

        return order.id();
    }

    private void handleTrade(Order order, TreeMap<Double, Queue<Order>> orderBookSide) {
        int sizeLeftToMatch = order.size();
        Queue<Order> orders = orderBookSide.getOrDefault(order.price(), new LinkedList<>());
        Iterator<Order> ordersForALevel = orders.iterator();
        while (ordersForALevel.hasNext() && sizeLeftToMatch > 0) {
            Order matchingBid = ordersForALevel.next();
            if (matchingBid.size() >= sizeLeftToMatch) {
                //remove order
                ordersForALevel.remove();
                this.orders.remove(matchingBid.id());
                //create a trade
                tradeListeners.forEach(orderBookTradeListener -> orderBookTradeListener.onTrade(new Trade(/**TODO**/"1", Set.of(matchingBid.id(), order.id()), Instant.now(), order.price(), order.size())));
                sizeLeftToMatch -= matchingBid.size();
            }
        }
        if (orderBookSide.getOrDefault(order.price(), new LinkedList<>()).isEmpty()) {
            orderBookSide.remove(order.price());
        }
    }

    private BidAsk getBidAsk() {
        Double bid = bids.isEmpty() || bids.firstEntry().getValue().isEmpty() ? null : bids.firstKey();
        double bidSize = bids.isEmpty() ? 0.0 : bids.firstEntry().getValue().stream().mapToDouble(Order::size).sum();

        Double ask = asks.isEmpty() || asks.firstEntry().getValue().isEmpty() ? null : asks.firstKey();
        double askSize = asks.isEmpty() ? 0.0 : asks.firstEntry().getValue().stream().mapToDouble(Order::size).sum();
        return new BidAsk(bid, bidSize, ask, askSize);
    }

    @Override
    public void addTradeListener(OrderBookTradeListener orderBookTradeListener) {
        tradeListeners.add(orderBookTradeListener);
    }

    @Override
    public synchronized Order getOrder(String id) {
        return orders.get(id);
    }

    @Override
    public synchronized CancelStatus cancelOrder(String id) {
        return CANCELED;
    }

    @Override
    public synchronized List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }

    @Override
    public int getDepth() {
        return 0;
    }

    @Override
    public BidAsk getTopBidAsk() {
        return getBidAsk();
    }

    @Override
    public Map<Double, Double> getAskLevels() {
        return asks.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().mapToDouble(Order::size).sum()));
    }

    @Override
    public Map<Double, Double> getBidLevels() {
        return bids.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().mapToDouble(Order::size).sum()));
    }
}
