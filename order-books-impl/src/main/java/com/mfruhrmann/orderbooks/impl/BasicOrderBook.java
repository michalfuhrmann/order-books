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
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.mfruhrmann.orderbooks.api.OrderBook.CancelStatus.CANCELED;

/**
 * Basic implementation of the order book serving as a foundation for further improvements and as a baseline for performance comparison.
 * This implementation is thread-safe but the thread safety is done in a very basic way where we synchronize on the whole class,
 * so the performance of this implementation is expected to be relatively the lowest.
 */
public class BasicOrderBook implements OrderBook {

    public static final LinkedList<Order> EMPTY_LIST = new LinkedList<>();
    private final List<OrderBookTradeListener> tradeListeners = new ArrayList<>();

    private final AtomicLong tradeIdGenerator = new AtomicLong();

    private final Map<String, Order> orders = new HashMap<>();
    private final TreeMap<Double, LinkedList<Order>> asks = new TreeMap<>();
    private final TreeMap<Double, LinkedList<Order>> bids = new TreeMap<>(Comparator.reverseOrder());

    @Override
    public synchronized String addOrder(Order order) {
        BidAsk bidAsk = getTopBidAsk();
        if (order.type() == OrderType.LIMIT) {
            if (order.side() == Side.BUY) {
                Double bestAsk = bidAsk.ask();
                if (bestAsk == null || bestAsk > order.price()) {
                    orders.put(order.id(), order);
                    bids.computeIfAbsent(order.price(), aDouble -> new LinkedList<>()).add(order);
                    //we add to the bids
                } else {
                    //we have a trade
                    handleTrade(order, asks, bids);
                }
            } else if (order.side() == Side.SELL) {
                Double bestBid = bidAsk.bid();
                if (bestBid == null || bestBid < order.price()) {
                    orders.put(order.id(), order);
                    asks.computeIfAbsent(order.price(), aDouble -> new LinkedList<>()).add(order);
                } else {
                    //we have a trade
                    handleTrade(order, bids, asks);
                }
            }
        }

        return order.id();
    }

    private void handleTrade(Order incomingOrder, TreeMap<Double, LinkedList<Order>> orderBookSide, TreeMap<Double, LinkedList<Order>> oppositeSide) {
        int sizeLeftToMatch = incomingOrder.size();
        Iterator<Map.Entry<Double, LinkedList<Order>>> orderBookIterator = orderBookSide.entrySet().iterator();
        while (orderBookIterator.hasNext()) {
            Map.Entry<Double, LinkedList<Order>> nextLevelEntry = orderBookIterator.next();
            Double price = nextLevelEntry.getKey();
            LinkedList<Order> ordersForLevel = nextLevelEntry.getValue();
            Iterator<Order> nextOrderIterator = ordersForLevel.iterator();
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

                    ordersForLevel.add(0, new ImmutableOrder(nextOrder.id(), nextOrder.side(), nextOrder.type(), nextOrder.ts(), nextOrder.price(), nextOrder.size() - sizeLeftToMatch));
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
            Order incomingOrderWithNoMatch = new ImmutableOrder(incomingOrder.id(), incomingOrder.side(), incomingOrder.type(), incomingOrder.ts(), incomingOrder.price(), sizeLeftToMatch);
            this.orders.put(incomingOrderWithNoMatch.id(), incomingOrderWithNoMatch);
            oppositeSide.computeIfAbsent(incomingOrder.price(), price -> new LinkedList<>()).add(incomingOrderWithNoMatch);

        }
    }

    private void notifyTradeListeners(Order order, Order match, int sizeLeftToMatch) {
        tradeListeners.forEach(orderBookTradeListener -> orderBookTradeListener.onTrade(
                new Trade(String.valueOf(tradeIdGenerator.incrementAndGet()),
                        Set.of(match.id(), order.id()),
                        Instant.now(),
                        match.price(),
                        sizeLeftToMatch)));
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
