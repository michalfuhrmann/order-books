package com.mfruhrmann.orderbooks.impl;

import com.mfruhrmann.orderbooks.api.OrderBook;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.mfruhrmann.orderbooks.api.OrderBook.CancelStatus.CANCELED;

/**
 * Based on List instead of TreeMap, price levels are calculated as offsets from top of the book.
 */
public class ListBasedOrderBook implements OrderBook {

    private final List<OrderBookTradeListener> tradeListeners = new ArrayList<>();

    private final AtomicLong tradeIdGenerator = new AtomicLong();

    private final Map<String, Order> orders = new HashMap<>();
    private final LinkedList<Deque<Order>> asks = new LinkedList<>();
    private final LinkedList<Deque<Order>> bids = new LinkedList<>();
    private final double priceStep;
    private Double topBid = null;
    private Double topAsk = null;

    public ListBasedOrderBook(double priceStep) {
        this.priceStep = priceStep;
    }

    @Override
    public synchronized String addOrder(Order order) {
        if (order.type() == OrderType.LIMIT) {
            if (order.side() == Side.BUY) {
                Double bestAsk = topAsk;
                if (bestAsk == null || bestAsk > order.price()) {
                    orders.put(order.id(), order);
                    int index = computeIndexForPrice(topBid, order.price());
                    addOrder(bids, index, order);
                    topBid = Math.max(topBid == null ? order.price() : topBid, order.price());
                    //we add to the bids
                } else {
                    //we have a trade
                    handleTrade(order, asks, bids, topAsk, topBid);
                }
            } else if (order.side() == Side.SELL) {
                Double bestBid = topBid;
                if (bestBid == null || bestBid < order.price()) {
                    orders.put(order.id(), order);
                    int index = computeIndexForPrice(topAsk, order.price());
                    addOrder(asks, index, order);
                    topAsk = Math.min(topAsk == null ? order.price() : topAsk, order.price());

                } else {
                    //we have a trade
                    handleTrade(order, bids, asks, topBid, topAsk);
                }
            }
        }
        BidAsk bidAsk = getBidAsk();
        topBid = bidAsk.bid();
        topAsk = bidAsk.ask();

        return order.id();
    }

    private void addOrder(LinkedList<Deque<Order>> side, int index, Order order) {
        Deque<Order> level = index < side.size() ? side.get(index) : null;

        if (level == null) {
            level = new LinkedList<>();
            level.add(order);
            if (side.size() < index) {
                //add missing
                IntStream.range(side.size(), index).forEach(idx -> side.add(idx, new LinkedList<>()));
            }
            side.add(index, level);
            return;
        }
        level.add(order);
    }

    private int computeIndexForPrice(Double topPrice, double price) {
        if (topPrice == null) {
            return 0;
        }
        return (int) ((topPrice - price) / priceStep);
    }

    private void handleTrade(Order incomingOrder, LinkedList<Deque<Order>> orderBookSide, LinkedList<Deque<Order>> oppositeSide, Double topPrice, Double topOppositePrice) {
        var sizeLeftToMatch = incomingOrder.size();
        var orderBookIterator = orderBookSide.iterator();
        int priceLevelIndex = 0;
        while (orderBookIterator.hasNext()) {
            var nextLevelEntry = orderBookIterator.next();
            var price = topPrice + (priceStep * priceLevelIndex);
            var nextOrderIterator = nextLevelEntry.iterator();

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
                    nextLevelEntry.offerFirst(nextOrder.withNewSize(size));
                    //create a trade
                    notifyTradeListeners(incomingOrder, nextOrder, sizeLeftToMatch);

                    sizeLeftToMatch -= nextOrder.size();
                }
            }
            if (priceLevelIndex < orderBookSide.size() && (orderBookSide.get(priceLevelIndex) == null || orderBookSide.get(priceLevelIndex).isEmpty())) {
                orderBookIterator.remove();
            }
            if (incomingOrder.type() == OrderType.LIMIT && price == incomingOrder.price()) {  //we cannot match further trades
                break;
            }
            priceLevelIndex++;
        }
        if (sizeLeftToMatch > 0) {
            Order incomingOrderWithNoMatch = incomingOrder.withNewSize(sizeLeftToMatch);
            this.orders.put(incomingOrderWithNoMatch.id(), incomingOrderWithNoMatch);
            int idx = computeIndexForPrice(topOppositePrice, incomingOrder.price());
            if (idx < oppositeSide.size()) {
                oppositeSide.get(idx).add(incomingOrderWithNoMatch);
            } else {
                var ordersNewLevel = new LinkedList<Order>();
                ordersNewLevel.add(incomingOrderWithNoMatch);
                oppositeSide.add(idx, ordersNewLevel);
            }
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

    private long getCurrentTime() {
        return System.nanoTime();
    }


    private BidAsk getBidAsk() {
        var bid = bids.isEmpty() || bids.getFirst().isEmpty() ? null : bids.getFirst().getFirst().price();
        var ask = asks.isEmpty() || asks.getFirst().isEmpty() ? null : asks.getFirst().getFirst().price();

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
        var bid = bids.isEmpty() || bids.getFirst().isEmpty() ? null : bids.getFirst().getFirst().price();
        var bidSize = bids.isEmpty() || bids.getFirst().isEmpty() ? 0 : bids.getFirst().stream().mapToDouble(Order::size).sum();

        var ask = asks.isEmpty() || asks.getFirst().isEmpty() ? null : asks.getFirst().getFirst().price();
        var askSize = asks.isEmpty() || asks.getFirst().isEmpty() ? 0 : asks.getFirst().stream().mapToDouble(Order::size).sum();
        return new TopOrderBook(bid, bidSize, ask, askSize);
    }

    @Override
    public Map<Double, Double> getAskLevels() {
        return IntStream.range(0, asks.size())
                .filter(idx -> {
                    var ordersForLevel = asks.get(idx);
                    return ordersForLevel != null && !ordersForLevel.isEmpty();
                })
                .boxed()
                .collect(Collectors.toMap(idx -> topAsk + (idx * priceStep), idx -> asks.get(idx).stream().mapToDouble(Order::size).sum()));
//        return asks.entrySet().stream()
//                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().mapToDouble(Order::size).sum()));
    }

    @Override
    public Map<Double, Double> getBidLevels() {
        return IntStream.range(0, bids.size())
                .filter(idx -> {
                    var ordersForLevel = bids.get(idx);
                    return ordersForLevel != null && !ordersForLevel.isEmpty();
                })
                .boxed()
                .collect(Collectors.toMap(idx -> (topBid + (idx * priceStep)), idx -> bids.get(idx).stream().mapToDouble(Order::size).sum()));
    }

    record BidAsk(Double bid, Double ask) {
    }
}
