package com.mfruhrmann.orderbooks.impl;

import com.mfruhrmann.orderbooks.api.OrderBook;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.mfruhrmann.orderbooks.api.OrderBook.CancelStatus.CANCELED;

/**
 * The only difference is underlying collection that stores orders for a given level. In this implementation it's ArrayDeque.
 */
public class BasicOrderBookArrayDeque implements OrderBook {

    public static final Deque<Order> EMPTY_LIST = new ArrayDeque<>();
    private final List<OrderBookTradeListener> tradeListeners = new ArrayList<>();

    private final AtomicLong tradeIdGenerator = new AtomicLong();

    private final Map<String, Order> orders = new HashMap<>();
    private final TreeMap<Double, Deque<Order>> asks = new TreeMap<>();
    private final TreeMap<Double, Deque<Order>> bids = new TreeMap<>(Comparator.reverseOrder());

    private static long getCurrentTime() {
        return System.nanoTime();
    }

    @Override
    public synchronized String addOrder(Order order) {
        BidAsk bidAsk = getBidAsk();
        if (order.type() == OrderType.LIMIT) {
            if (order.side() == Side.BUY) {
                Double bestAsk = bidAsk.ask();
                if (bestAsk == null || bestAsk > order.price()) {
                    orders.put(order.id(), order);
                    bids.computeIfAbsent(order.price(), aDouble -> new ArrayDeque<>()).add(order);
                    //we add to the bids
                } else {
                    //we have a trade
                    handleTrade(order, asks, bids);
                }
            } else if (order.side() == Side.SELL) {
                Double bestBid = bidAsk.bid();
                if (bestBid == null || bestBid < order.price()) {
                    orders.put(order.id(), order);
                    asks.computeIfAbsent(order.price(), aDouble -> new ArrayDeque<>()).add(order);
                } else {
                    //we have a trade
                    handleTrade(order, bids, asks);
                }
            }
        }

        return order.id();
    }

    private void handleTrade(Order incomingOrder, TreeMap<Double, Deque<Order>> orderBookSide, TreeMap<Double, Deque<Order>> oppositeSide) {
        var sizeLeftToMatch = incomingOrder.size();
        var orderBookIterator = orderBookSide.entrySet().iterator();
        while (orderBookIterator.hasNext()) {
            var nextLevelEntry = orderBookIterator.next();
            var price = nextLevelEntry.getKey();
            var ordersForLevel = nextLevelEntry.getValue();
            var nextOrderIterator = ordersForLevel.iterator();

            while (nextOrderIterator.hasNext() && sizeLeftToMatch > 0) {
                Order nextOrder = nextOrderIterator.next();
                if (sizeLeftToMatch >= nextOrder.size()) {
                    //remove order
                    nextOrderIterator.remove();
                    this.orders.remove(nextOrder.id());
                    //create a trade
                    notifyTradeListeners(incomingOrder, nextOrder, nextOrder.size());
                    sizeLeftToMatch -= nextOrder.size();
                } else {
                    //handle partial order
                    nextOrderIterator.remove();
                    this.orders.remove(nextOrder.id());

                    int size = nextOrder.size() - sizeLeftToMatch;
                    ordersForLevel.offerFirst(nextOrder.withNewSize(size));
                    //create a trade
                    notifyTradeListeners(incomingOrder, nextOrder, sizeLeftToMatch);

                    sizeLeftToMatch -= nextOrder.size();
                }
            }
            if (orderBookSide.getOrDefault(incomingOrder.price(), EMPTY_LIST).isEmpty()) {
                orderBookIterator.remove();
            }
            if (incomingOrder.type() == OrderType.LIMIT && price == incomingOrder.price()) {  //we cannot match further trades
                break;
            }
        }
        if (sizeLeftToMatch > 0) {
            Order incomingOrderWithNoMatch = incomingOrder.withNewSize(sizeLeftToMatch);
            this.orders.put(incomingOrderWithNoMatch.id(), incomingOrderWithNoMatch);
            oppositeSide.computeIfAbsent(incomingOrder.price(), price -> new ArrayDeque<>()).add(incomingOrderWithNoMatch);
        }
    }

    private void notifyTradeListeners(Order order, Order match, int sizeLeftToMatch) {
        tradeListeners.forEach(orderBookTradeListener -> orderBookTradeListener.onTrade(
                new Trade(String.valueOf(tradeIdGenerator.incrementAndGet()),
                        Set.of(match.id(), order.id()),
                        getCurrentTime(),
                        match.price(),
                        sizeLeftToMatch)));
    }

    private BidAsk getBidAsk() {
        var bid = bids.isEmpty() || bids.firstEntry().getValue().isEmpty() ? null : bids.firstKey();

        var ask = asks.isEmpty() || asks.firstEntry().getValue().isEmpty() ? null : asks.firstKey();
        return new BidAsk(bid, ask);
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
    public TopOrderBook getTopOrderBook() {
        var bid = bids.isEmpty() || bids.firstEntry().getValue().isEmpty() ? null : bids.firstKey();
        var bidSize = bids.isEmpty() ? 0.0 : bids.firstEntry().getValue().stream().mapToDouble(Order::size).sum();

        var ask = asks.isEmpty() || asks.firstEntry().getValue().isEmpty() ? null : asks.firstKey();
        var askSize = asks.isEmpty() ? 0.0 : asks.firstEntry().getValue().stream().mapToDouble(Order::size).sum();
        return new TopOrderBook(bid, bidSize, ask, askSize);
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

    record BidAsk(Double bid, Double ask) {
    }
}
